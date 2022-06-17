![alt tag](https://cloud.githubusercontent.com/assets/12032146/20262054/3d5de056-aa69-11e6-8ecd-31f02d96c4d8.png)

# Secured Streaming

This document contains instructions on implementing *secured streaming*, where access to content is restricted to authenticated users only.

## Why secured streaming?

Sometimes, a client application needs to access content that is not publicly available for everyone. Typical reasons for content availability restrictions are:
1. There is bonus content for users who have purchased access to the premium user area (business model reason).
2. There is user account specific private content that should be available only to the original creator and a few selected user accounts (privacy reason).
3. There is content that is made available based on users' region or age (legal reasons).

To enforce content availability restrictions, users need to be authenticated. Typically, this is done with user accounts: to access protected content, users log in with personal username and password. Once the user is authenticated and access to protected content is granted by the web application, the client application uses secured streaming for accessing protected content files.

## Overview

This document focuses on the following approach for secured streaming:
* Protected content (a video file) is stored in Amazon AWS S3 bucket.
* The video file is accessed *only* via AWS CloudFront CDN, not directly via S3 public link.
* CloudFront is configured to limit access rights by requiring *signed cookies* from the viewers.
* The client application (a video player app) passes the signed cookies when it attempts to play protected content.
* CloudFront will check the cookies and either grant or deny access.

Typically, a full solution includes a web application and user account database. An individual user must have a personal user account to access protected content, and s/he needs to log in to view any protected content. Once a user logs in, the web application generates and offers signed cookies to the client application, which uses them to retrieve content via CloudFront.

In this simple example, we don't create a web application nor a user account database. Instead, we create long-term signed cookies offline and bundle them with the client application. From security point of view‚ this is a very bad idea! Use this approach only for testing purposes. Read the warnings below and implement a proper web application and user authentication solution.

> You can find files related to this document under /cookies subdirectory in this repository. Normally you don't share the private key with anyone. Since this is only an example and we don't really need to protected access to the demo video file, we share also the private key, so that you can test and debug each step. When you create your own key pair, do not push the private key to code repository.

For more information, see https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/PrivateContent.html

## AWS Preparations

### Create an S3 bucket for protected demo content

1. Log in to AWS management console using your AWS account credentials.
2. Navigate to Amazon S3 / Buckets list.
3. Click 'Create bucket'. Here we use the following configuration:
- Bucket name: orion360sdk-protected-content
- AWS region: EU (Stockholm) eu-north-1
- ACLs enabled
- Block all public access (unchecked, for now)
- Bucket Versioning disabled
- No tags
- Server-side encryption disabled
4. Navigate to your new bucket (here: *orion360sdk-protected-content*).
5. Click 'Upload' and drag n drop a demo video file to upload it to S3 (here: *Orion360_test_video_1920x960.mp4*).
6. Once the upload completes, click the file to check its cloud URL (here: *https://orion360sdk-protected-content.s3.eu-north-1.amazonaws.com/Orion360_test_video_1920x960.mp4*)
7. Via Actions, enable public read access to the video file (for now). Check with another browser (not signed in to AWS) that above video URL works OK.

### Setup CloudFront for demo content

1. Log in to AWS management console using your AWS account credentials.
2. Navigate to Amazon CloudFront / Distributions list.
3. Click 'Create distribution'. Here we use the following configuration:
- Origin domain: choose your protected content S3 bucket (here: *orion360sdk-protected-content.s3.eu-north-1.amazonaws.com*)
- S3 bucket access: Yes use OAI (bucket can restrict access to only CloudFront)
  - Create new OAI: access-identity-orion360sdk-protected-content.s3.eu-north-1.amazonaws.com
    - Bucket policy: Yes, update the bucket policy (IMPORTANT!)
- Redirect HTTP to HTTPS
- Price class: what you want (here: Use only North America and Europe)
- Restrict viewer access: No (for now, we will first allow unprotected public access via CloudFront)

> Once you are done, wait until the new distribution's state changes from 'Deploying' to a timestamp. This will take a few minutes.

> Tip: Back in S3, select your bucket and tab *Permission*. Under *Bucket Policy*, there is now a JSON configuration that allows CloudFront to provide access to this bucket. It was created automatically, as we allowed updating bucket policy.

4. Under CloudFront distributions, check the Domain name of your new distribution (here: *d15i6zsi2io35f.cloudfront.net*).
5. Check with another browser (not signed in to AWS) that content filename appended to above subdomain works OK and the video can be played from CloudFront CDN (here: *https://d15i6zsi2io35f.cloudfront.net/Orion360_test_video_1920x960.mp4*)

### Limit access to content only through CloudFront

We will now deny public access directly via S3 URL, so that users cannot bypass restrictions that we will set later via CloudFront.

1. Log in to AWS management console using your AWS account credentials.
2. Navigate to Amazon S3 / Buckets list.
3. Select your bucket (here: orion360sdk-protected-content) and navigate to *Permissions* tab.
4. Under *Block public access*, click *Edit* and check *Block all public access*. Save and Confirm.
5. Check with another browser (not signed in to AWS) that direct S3 access is now blocked, but access via CloudFront still works.
- this URL does not work anymore (access denied): https://orion360sdk-protected-content.s3.eu-north-1.amazonaws.com/Orion360_test_video_1920x960.mp4
- this URL does still work: https://d15i6zsi2io35f.cloudfront.net/Orion360_test_video_1920x960.mp4

### Require that users access protected content using signed cookies

In fact, there are two options: *signed URL*s or *signed cookies*. Here we use signed cookies, as it suits best for accessing a collection of protected files (S3 bucket).

In order to sign cookies, we need a *signer*. This can be either a *trusted key group* (recommended by AWS) or an *AWS account*. Here we use trusted key group.

The signer will use a public-private key pair as follows: the signer uses private key to sign the URL or cookie, and CloudFront uses the public key to verify the signature. Thus, when you give a signed cookie to your client application and it then passes the cookie along with a content request, CloudFront can verify that the client indeed has a right to access the requested content.

> Key pairs should be automatically rotated for better security. That is out of the scope of this article.

#### Create a new key pair:

Requirements for a key pair:
* It must be an SSH-2 RSA key pair.
* It must be in base64-encoded PEM format.
* It must be a 2048-bit key pair.

1. Create a key pair using OpenSSL:
```
> openssl genrsa -out cookie_private_key.pem 2048
Generating RSA private key, 2048 bit long modulus
............................+++
............................................................................................+++
e is 65537 (0x10001)
```

2. Extract public key from the key pair file:
```
> openssl rsa -pubout -in cookie_private_key.pem -out cookie_public_key.pem
writing RSA key
```

#### Upload the key pair to CloudFront

Instructions from AWS documentation:

1. Sign in to the AWS Management Console and open the CloudFront console at https://console.aws.amazon.com/cloudfront/v3/home.
2. In the navigation menu, choose *Public keys*.
3. Choose *Create public key*.
4. In the *Create public key* window, do the following:
  - For *Key name*, type a name to identify the public key.
  - For *Key value*, paste the public key. If you followed the steps in the preceding procedure, the public key is in the file named cookie_public_key.pem. To copy and paste the contents of the public key, you can:
    - Use the cat command on the macOS or Linux command line, like this: ```cat public_key.pem```. Copy the output of that command, then paste it into the *Key value* field.
    - Open the *public_key.pem* file with a plaintext editor like Notepad (on Windows) or TextEdit (on macOS). Copy the contents of the file, then paste it into the *Key value* field.
  - (Optional) For *Comment*, add a comment to describe the public key.
  - When finished, choose *Create*.

> Record the public key ID. You use it later when you create signed URLs or signed cookies, as the value of the Key-Pair-Id field.

#### Add the public key to a key group

Instructions from AWS documentation:

1. Open the CloudFront console at https://console.aws.amazon.com/cloudfront/v3/home.
2. In the navigation menu, choose *Key groups*.
3. Choose *Create key group*.
4. On the *Create key group* page, do the following:
  - For *Key group name*, type a name to identify the key group.
  - (Optional) For *Comment*, type a comment to describe the key group.
  - For *Public keys*, select the public key to add to the key group, then choose *Add*. Repeat this step for each public key that you want to add to the key group.
  - Choose *Create key group*.

> Record the key group name. You use it later to associate the key group with a cache behavior in a CloudFront distribution. (In the CloudFront API, you use the key group ID to associate the key group with a cache behavior.)

#### Adding a signer to a distribution

A signer is associated with CloudFront cache behavior: some files may be protected and require signed cookie, while others can be unprotected and work without them.

Instructions from AWS documentation:
1. Record the key group ID of the key group that you want to use as a trusted signer.
2. Open the CloudFront console at https://console.aws.amazon.com/cloudfront/v3/home.
3. Choose the distribution whose files you want to protect with signed URLs or signed cookies.
4. Choose the *Behaviors* tab.
5. Select the cache behavior whose path pattern matches the files that you want to protect with signed URLs or signed cookies, and then choose *Edit*.
6. On the *Edit Behavior* page, do the following:
   - For *Restrict Viewer Access* (Use Signed URLs or Signed Cookies), choose Yes.
   - For *Trusted Key Groups* or *Trusted Signer*, choose *Trusted Key Groups*.
   - For *Trusted Key Groups*, choose the key group to add, and then choose *Add*. Repeat if you want to add more than one key group.
7. Choose *Yes, Edit to update the cache behavior*.
8. Use another browser (not signed in to AWS) to check access to the test video file via CloudFront (here: https://d15i6zsi2io35f.cloudfront.net/Orion360_test_video_1920x960.mp4). It should not work anymore, instead you get this error: 'Missing Key-Pair-Id query parameter or cookie value'. CloudFront now requires a signed cookie to grant access to this file.

> Now we have a video file in S3/CloudFront that is *protected* i.e. only accessible to a client that can include properly signed cookies to its content requests.

### Create signed cookies

You can set different kinds of limitations in the cookies, for example start and expiration dates and accepted IP address/range. These are out of the scope of this document, but you are free to use these features. They simply change the content of the signed cookies.

There are also various ways to create and transmit signed cookies from your web app to the viewer app (here: an Android application running Orion360 SDK Pro). In this example we don't have a web app to communicate with, so we will simply create the necessary signed cookies offline and bundle them directly to the app (ouch! You should not do that!)

WARNING: Don't do these things in a real app:
- Don't put your private key to the app and sign cookies within the app (a hacker can extract your private key from your app's .apk).
- Don't bundle pre-created signed cookies with the app like we do in this example (a hacker can extract the cookies from your app's apk).
- Don't sign cookies offline with an expiration date set very far to the future like we do in this example (if a hacker gets the cookies, they will work for him a long time).

Instead, you should have a web app in the cloud that actually generates signed cookies on-the-fly for properly authenticated users with short expiration date and perhaps with other limitations, too. You should generate and send new signed cookies whenever user logs in and also if a user's session lasts a long time. You should also automatically rotate the keys that are used for signing and validating the cookies in CloudFront.

#### Three required cookies

You need three *Set-Cookie* headers because each Set-Cookie header can contain only one name-value pair, and a CloudFront signed cookie requires three name-value pairs. The name-value pairs are: *CloudFront-Expires*, *CloudFront-Signature*, and *CloudFront-Key-Pair-Id*.

This is the format of the cookies:
```
Set-Cookie: 
CloudFront-Expires=date and time in Unix time format (in seconds) and Coordinated Universal Time (UTC); 
Domain=optional domain name; 
Path=/optional directory path; 
Secure; 
HttpOnly

Set-Cookie: 
CloudFront-Signature=hashed and signed version of the policy statement; 
Domain=optional domain name; 
Path=/optional directory path; 
Secure; 
HttpOnly

Set-Cookie: 
CloudFront-Key-Pair-Id=public key ID for the CloudFront public key whose corresponding private key you're using to generate the signature; 
Domain=optional domain name; 
Path=/optional directory path; 
Secure; 
HttpOnly
```

In our case, this becomes:
```
Set-Cookie: CloudFront-Expires=2147483647; Domain=d15i6zsi2io35f.cloudfront.net; Path=/*; Secure; HttpOnly

Set-Cookie: CloudFront-Signature=[hashed and signed version of the policy statement]; 
Domain=d15i6zsi2io35f.cloudfront.net; Path=/*; Secure; HttpOnly

Set-Cookie: CloudFront-Key-Pair-Id=K37HI3P0TW0W7Q; Domain=d15i6zsi2io35f.cloudfront.net; Path=/*; Secure; HttpOnly
```

> We are still missing a value for CloudFront-Signature field, which will be created next.

#### Creating a signature

We must first create *a policy statement* in JSON format and then sign and hash the policy statement. The result will be the signature.

Here we use *a canned policy* (the other alternative is a custom policy).

The policy statement must be in this exact format (JSON) with UTF-8 character encoding:
```
{
    "Statement": [
        {
            "Resource": "base URL or stream name",
            "Condition": {
                "DateLessThan": {
                    "AWS:EpochTime": ending date and time in Unix time format and UTC
                }
            }
        }
    ]
}
```

In our case, this becomes:
```
{
    "Statement": [
        {
            "Resource": "https://d15i6zsi2io35f.cloudfront.net/Orion360_test_video_1920x960.mp4",
            "Condition": {
                "DateLessThan": {
                    "AWS:EpochTime": 2147483647
                }
            }
        }
    ]
}
```

> After writing the statement, REMOVE ALL WHITE SPACE (including tabs and newline characters!)

Our example with removed white space:
```
{"Statement":[{"Resource":"https://d15i6zsi2io35f.cloudfront.net/Orion360_test_video_1920x960.mp4","Condition":{"DateLessThan":{"AWS:EpochTime":2147483647}}}]}
```

Next, we put the above condensed JSON (no white space) into a text file (UTF-8 encoding) called *cookie_policy.json* and run the following command-line:
```
> cat cookie_policy.json | openssl sha1 -sign cookie_private_key.pem | base64 | tr '+=/' '-_~'

uI3ott-V5IiFW-ZTgXg7AAN0iIC4Y2dnz0BLCLrPs7icTx3qghkz1HqZ9p0LnHShdEg8awMEsg5ev~ClXGBu52x80jIxI6tjBoH8ivZ3Ddt09TvNq95Q0ij2-1TsbHyxevJ3Iex29TCTMEG7Y36AWf9~IJzzJHKzp~SiflEAn-sPR0Z-9hdrQmkgalx5qSiu~Und7GM6qV2WMxwzrcGd7q8AV9N7IKnyJR-fqjOA7mEmOnQrT4iCCdkEcxmlgBxC3wRpmw53mbPP2OVr4c~b~dwB7XYr-gDbjtoSXCFwb6Ds~SdXx0hjmCbY1EynN8wGslfsYpHmiuyLFUnABOhzNQ__
```

This will hash it with SHA1, sign it using our private key, then base64 encode it, and finally replace a few forbidden characters.

The output of the command is the signature. Thus, our final signed cookies in this exercise are:
```
Set-Cookie: CloudFront-Expires=2147483647; Domain=d15i6zsi2io35f.cloudfront.net; Path=/*; Secure; HttpOnly

Set-Cookie: CloudFront-Signature=uI3ott-V5IiFW-ZTgXg7AAN0iIC4Y2dnz0BLCLrPs7icTx3qghkz1HqZ9p0LnHShdEg8awMEsg5ev~ClXGBu52x80jIxI6tjBoH8ivZ3Ddt09TvNq95Q0ij2-1TsbHyxevJ3Iex29TCTMEG7Y36AWf9~IJzzJHKzp~SiflEAn-sPR0Z-9hdrQmkgalx5qSiu~Und7GM6qV2WMxwzrcGd7q8AV9N7IKnyJR-fqjOA7mEmOnQrT4iCCdkEcxmlgBxC3wRpmw53mbPP2OVr4c~b~dwB7XYr-gDbjtoSXCFwb6Ds~SdXx0hjmCbY1EynN8wGslfsYpHmiuyLFUnABOhzNQ__;Domain=d15i6zsi2io35f.cloudfront.net; Path=/*; Secure; HttpOnly

Set-Cookie: CloudFront-Key-Pair-Id=K37HI3P0TW0W7Q; Domain=d15i6zsi2io35f.cloudfront.net; Path=/*; Secure; HttpOnly
```

### Test signed cookies

We can use for example *curl* for quickly testing that our signed cookies work as intended.

First, check that the protected video file cannot be accessed without signed cookies:
```
> curl https://orion360sdk-protected-content.s3.eu-north-1.amazonaws.com/Orion360_test_video_1920x960.mp4

<?xml version="1.0" encoding="UTF-8"?><Error><Code>AccessDenied</Code><Message>Access Denied</Message><RequestId>EF61VPCTA9EWTNJE</RequestId><HostId>HztIok5/OhFCakvtwsuQ8j10y6gvEKBL+vTiLANg7Q0Z0WYcaN3L2s8A9W4/Jba8FNp6IUHy2TM=</HostId></Error>

> curl https://d15i6zsi2io35f.cloudfront.net/Orion360_test_video_1920x960.mp4

<?xml version="1.0" encoding="UTF-8"?><Error><Code>MissingKey</Code><Message>Missing Key-Pair-Id query parameter or cookie value</Message></Error>
```

Next, let's try again with CloudFront URL using our signed cookies. Notice that curl does not support multiple cookie parameters and we must combine the cookies with a semicolon:
```
> curl --cookie "CloudFront-Expires=2147483647; CloudFront-Signature=uI3ott-V5IiFW-ZTgXg7AAN0iIC4Y2dnz0BLCLrPs7icTx3qghkz1HqZ9p0LnHShdEg8awMEsg5ev~ClXGBu52x80jIxI6tjBoH8ivZ3Ddt09TvNq95Q0ij2-1TsbHyxevJ3Iex29TCTMEG7Y36AWf9~IJzzJHKzp~SiflEAn-sPR0Z-9hdrQmkgalx5qSiu~Und7GM6qV2WMxwzrcGd7q8AV9N7IKnyJR-fqjOA7mEmOnQrT4iCCdkEcxmlgBxC3wRpmw53mbPP2OVr4c~b~dwB7XYr-gDbjtoSXCFwb6Ds~SdXx0hjmCbY1EynN8wGslfsYpHmiuyLFUnABOhzNQ__; CloudFront-Key-Pair-Id=K37HI3P0TW0W7Q; Domain=d15i6zsi2io35f.cloudfront.net; Path=/*; Secure; HttpOnly" https://d15i6zsi2io35f.cloudfront.net/Orion360_test_video_1920x960.mp4 --output test.mp4

  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100 2641k  100 2641k    0     0  7701k      0 --:--:-- --:--:-- --:--:-- 7701k
```

> We have now confirmed that our protected video file can be accessed only when proper signed cookies are included to the request.


## Client app modifications

See example streaming/SecuredStreaming for working examples of streaming protected content from S3 via CloudFront using:
- plain Android MediaPlayer (multiple approaches demonstrated)
- Orion360 SDK Pro with Android MediaPlayer as video engine

