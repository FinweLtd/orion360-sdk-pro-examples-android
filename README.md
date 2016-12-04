![alt tag](https://cloud.githubusercontent.com/assets/12032146/20262054/3d5de056-aa69-11e6-8ecd-31f02d96c4d8.png)

# Examples for Orion360 SDK (Pro) for Android

This repository contains a set of examples for creating a 360 photo/video player using Orion360 SDK (Pro) for Android and Android Studio IDE.

> This project is still under construction. Much more examples will be added.

Preface
-------

Thank you for choosing Orion360, and welcome to learning Orion360 SDK (Pro) for Android! You have made a good choice: Orion360 is the leading purpose-built SDK for 360/VR video apps, with over 400 licensed apps and 10+ millions of downloads in total - and growing fast.

We encourage you to begin studying from the _minimal_ examples. These are short and to-the-point; they will help you to create a simple 360 player in no-time. When you have mastered the basics, proceed to the more advanced examples that focus on a particular topic, such as touch input, VR mode, custom controls, and hotspots. The examples in this repository will become a valuable resource when you start adding features to your new 360 photo/video app, and will save you lots of development time!

In order to make studying the examples as easy as possible, each example has been implemented as an individual activity with very few dependencies outside of its own source code file - everything you need for a particular feature can be found from one place. To keep the examples short and easy to grasp, different topics are covered in separate examples. You don't need to go through them in order; just keep building your own app by adding features one-by-one based on your needs and priorities.

The aim of the examples is to show how the most common features can be implemented easily with Orion360, but also to help you understand _why_ something is done. Therefore, the examples are thoroughly commented and also briefly explained in this README. Notice that the Pro version of the SDK contains a number of features not available in the Basic version. Hence, also the amount of examples is greater, and they have been categorized under topics to keep everything organized.

Finally, all the examples are collected under a single application that can be compiled from this project, installed and run on your own Android device. This way you can try out the reference implementation with ease, and also experiment by modifying the examples. Notice that you are allowed to utilize the example code and resources in your own app project as described in the copyright section of the source code files.

Happy coding!

> This application is available in [Google Play](https://play.google.com/store/apps/details?id=fi.finwe.orion360.sdk.pro.examples)

Table of Contents
-----------------
1. [Preface](#preface)
2. [Table of Contents](#table-of-contents)
3. [Prerequisities](#prerequisities)
4. [Cloning and Running the Project](#cloning-and-running-the-project)
5. [Examples - Minimal](#examples-minimal)
  1. [Minimal Video Stream Player](#minimal-video-stream-player)
  2. [Minimal Video Adaptive Stream Player](#minimal-video-adaptive-stream-player)
  3. [Minimal Video Download Player](#minimal-video-download-player)
  4. [Minimal Video File Player](#minimal-video-file-player)
  5. [Minimal Video Controls](#minimal-video-controls)
  6. [Minimal VR Video File Player](#minimal-vr-video-file-player)
  7. [Minimal Image Download Player](#minimal-image-download-player)
  8. [Minimal Image File Player](#minimal-image-file-player)
6. [Examples - Application Framework](#examples-application-framework)
  1. [Custom Activity](#custom-activity)
  2. [Custom Fragment Activity](#custom-fragment-activity)
7. [Examples - Binding](#examples-binding)
  1. [Mono Panorama](#mono-panorama)
  2. [Mono Panorama VR](#mono-panorama-vr)
  3. [Stereo Panorama](#stereo-panorama)
  4. [Stereo Panorama VR](#stereo-panorama-vr)
  5. [Doughnut](#doughnut)
  6. [Rear-view Mirror](#rear-view-mirror)
  7. [Overview](#overview)
  8. [Blending](#blending)

Prerequisities
--------------

Basic Android software development skills are enough for understanding, modifying and running the examples.

As a first step, you should install Android Studio IDE (recommended version is 2.2 or newer):
https://developer.android.com/studio/install.html

Then, using the SDK Manager tool that comes with the IDE, install one or more Android SDKs. Notice that for Orion360 SDK Pro the minimum is **Android API level 19: Android 4.4 KitKat**.

> If you haven't already studied the Hello World project for Orion360 SDK (Pro), you should do that first and then continue with this example project. https://github.com/FinweLtd/orion360-sdk-pro-hello-android

Cloning and Running the Project
-------------------------------

To clone the project from GitHub, start Android Studio, select *Check out project from Version Control* and *Git* from the popup dialog.

![alt tag](https://cloud.githubusercontent.com/assets/12032146/20263277/863eb084-aa6e-11e6-9136-b7f4decc6556.png)

Set repository URL, parent directory, and project directory.

>Notice that the repository URL is easy to copy-paste from a web browser to Android Studio: click the green *Clone or download* button on the project's GitHub page, copy the URL from the dialog that appears, and paste it to Android Studio's dialog as shown below. 

Hit *Clone* button to retrieve the repository contents to your local machine.

![alt tag](https://cloud.githubusercontent.com/assets/12032146/20263303/adaf4bc4-aa6e-11e6-8282-5826b4035f76.png)

Cloning the project will take a moment. Android Studio then asks if you want to open the project, answer *Yes*.

When the project opens Android Studio performs Gradle sync that will take some time (please wait). After Gradle sync finishes, you can find the project files by opening the *Project* view on the left edge of the IDE window.

![alt tag](https://cloud.githubusercontent.com/assets/12032146/20263375/0d9c4bb8-aa6f-11e6-9356-fbca191643a8.png)

Next, connect an Android device to your computer via a USB cable, and then compile the project and run the app on your device by simply clicking the green *Play* button in the top toolbar. This will take a moment.

> While 360 photo/video apps can in theory be developed using an emulator, real Android hardware is highly recommended. The Android emulator does not support video playback. Moreover, to work with sensor fusion, touch control and VR mode, the developer frequently needs to run the app on target device.

When the app starts on your device, a menu of topics similar to the image below will be shown. Tap any topic to move to its submenu that contains one or more examples. Tap an example from the list to run it, and return to the examples menu by tapping the *Back* button from your device's Navigation Bar, and again to return to main menu (topics). In order to really understand what each example is about, you should always read the source code and comments.

![alt tag](https://cloud.githubusercontent.com/assets/12032146/20263452/66561464-aa6f-11e6-898c-f6329015dad6.png)

> Most examples use demo content that requires an Android device that can decode and play FullHD (1920x1080p) video, or less. However, a few examples may require UHD (3840x1920) resolution playback. If your development device does not support 4k UHD video, simply change the content URI to another one with smaller resolution (you can find plenty of demo content links from the *MainMenu* source code file).

Examples: Minimal
-----------------

This category contains examples that show a very minimal implementation for a particular topic, and thus provide a good starting point for studying Orion360.

### Minimal Video Stream Player

![alt tag](https://cloud.githubusercontent.com/assets/12032146/20630890/760a7514-b33c-11e6-9137-afba3863c5cc.png)

[View code](app/src/main/java/fi/finwe/orion360/sdk/pro/examples/minimal/MinimalVideoStreamPlayer.java)

An example of a minimal Orion360 video player, for streaming a video file over the network.

This example shows how to add an Orion360 view to an XML layout, set it as a rendering target in Java code, and define an MP4 video file (that resides somewhere in the network) as a content source. The video playback begins automatically when enough video frames have been downloaded and buffered. The example also shows how to create a simple buffering indicator by responding to buffering events.

The activity class extends _SimpleOrionActivity_, which creates a basic Orion360 player configuration, handles license checks etc., and propagates activity lifecycle events to Orion360 so that video playback can pause and continue automatically and get cleaned up.

Orion360 views have lots of features built-in; you will have all the following without writing any additional code:
- Support for rendering full spherical (360x180) equirectangular video content with rectilinear projection
- Panning, zooming and tilting the view with touch and movement sensors, which work seamlessly together
- Auto Horizon Aligner (AHL) keeps the horizon straight by gently re-orienting it when necessary

> Android device's hardware video decoder sets a limit for the maximum resolution / bitrate of a video file that can be decoded. In 2016, new mid-range devices support FullHD video and high-end devices 4k UHD video, while some popular older models cannot decode even FullHD. In addition, a decoded video frame needs to fit inside a single OpenGL texture. The maximum texture size in new devices ranges from 4096x4096 to 16384x16384, while some popular older models have 2048x2048 texture size. To be on the safe side, our recommendation is to use 1920x960 video resolution and a moderate bitrate. If necessary, you can offer another 3840x1920 stream for high-end devices, perhaps with an option to download the file first. With Orion360 SDK (Pro) you can also use adaptive HLS streams.

### Minimal Video Adaptive Stream Player

![alt_tag](https://cloud.githubusercontent.com/assets/12032146/20639984/37920482-b3dc-11e6-9fb0-8d42c26c50d4.png)

[View code](app/src/main/java/fi/finwe/orion360/sdk/pro/examples/minimal/MinimalVideoAdaptiveStreamPlayer.java)

An example of a minimal Orion360 video player, for playing a HLS stream over the network.

This example shows how to play adaptive HLS video streams. The principal idea is to encode a video multiple times with different parameters to create a set of different quality streams, and further divide these streams to short chunks at exactly the same timecodes so that video player can seamlessly switch between the streams by downloading the next chunk either at lower, same or better quality - depending on device capabilities and available network bandwidth.

Playing adaptive HLS streams can be surprisingly easy with Orion360 - simply use a URI that ends with an _.m3u8_ filename extension as a content URI, and Orion360 will recognize it as a HLS stream. It will then use the built-in ExoPlayer instance for downloading the HLS playlist/manifest file that defines the available stream qualities, estimates network bandwidth and device capabilities, and switches between streams automatically. Describing how HLS stream files can be created goes beyond this simple example (there are free tools such as _ffmpeg_ that can be used for the task).

> More information about adaptive video streams: https://en.wikipedia.org/wiki/Adaptive_bitrate_streaming

### Minimal Video Download Player

![alt tag](https://cloud.githubusercontent.com/assets/12032146/20632132/eb5799b2-b343-11e6-8828-4fa61144fa2e.png)

[View code](app/src/main/java/fi/finwe/orion360/sdk/pro/examples/minimal/MinimalVideoDownloadPlayer.java)

An example of a minimal Orion360 video player, for downloading a video file before playback.

Available network bandwith often becomes an issue when streaming video over the network (especially true with high-resolution 4k content). Unfortunately, saving a copy of a video file while streaming it is not possible with Android MediaPlayer as a video backend. Hence, if you need to obtain a local copy of a video file that resides in the network either for offline use or to be cached, download it separately as shown in this example.

Since downloading a large file will take a considerable amount of time, it needs to be done asynchronously. Here we use Android's DownloadManager service, which is recommended for video files (as an alternative, _MinimalImageDownloadPlayer_ shows how to download a file with own code). In this simple example, user needs to wait for the download to complete and the playback to begin as there is nothing else to do. However, you should consider placing a small download indicator somewhere in your app and allowing the user to continue using the app while the download is in progress. A high quality app has a download queue for downloading multiple files sequentially, is able to continue a download if it gets terminated early for example because of a network issue, allows user to cancel ongoing downloads, and uses platform notifications for indicating download progress and completion of a download. These features go beyond this example.

Video files are large and device models with small amounts of storage space tend to be popular as they are priced competitively. Consider saving the downloaded video file to external memory if it is currently present. It is also a good idea to offer a method for deleting downloaded content without uninstalling the whole app; this way users can still keep your app installed when they need to restore some storage space.

### Minimal Video File Player

![alt tag](https://cloud.githubusercontent.com/assets/12032146/20640465/3601e564-b3e7-11e6-961e-9d2e065856c1.png)

[View code](app/src/main/java/fi/finwe/orion360/sdk/pro/examples/minimal/MinimalVideoFilePlayer.java)

An example of a minimal Orion360 video player, for playing a video file from local file system.

This example showcases all supported file system locations and file access methods for video sources: the locations embedded to the app distribution packages, the app's private locations that become available after installation, and the locations that are more or less external to the app. To keep the example simple, only one location is active at a time and the others are commented out (you can easily select the active location from the source code). The supported locations are:

1. Application installation package's _/assets_ folder

   Private assets folder allows playing content embedded to the apps's own installation package (.apk). Notice 100MB .apk size limit in Google Play store. This is the recommended location when the application embeds video files to the installation package and _is NOT_ distributed via Google Play store (single large .apk file delivery).

2. Application installation package's _/res/raw_ folder

   Private raw resource folder allows playing content embedded to the app's own installation package (.apk). Notice 100MB .apk size limit in Google Play. Must use lowercase characters in filenames and access them without filename extension. This location is generally not recommended; use _/assets_ folder instead.

3. Application expansion packages

   Private expansion package allows playing content embedded to the app's extra installation package (.obb). Up to 2 GB per package, max 2 packages. This is the recommended location when the application embeds video files to the installation package and _is_ distributed via Google Play store. Fairly complex but very useful solution. For more information, see https://developer.android.com/google/play/expansion-files.html

4. Application's private path on device's internal memory

   Private internal folder is useful mainly when the app _downloads_ a video file for offline mode or to be cached, as only the app itself can access that location (exception: rooted devices). This location is recommended only if downloaded content files need to be protected from ordinary users - although the protection is easy to circumvent with a rooted device.

5. Application's private path on device's external memory

   Private external folder allows copying videos back and forth via file manager app or a USB cable, which can be useful for users who know their way in the file system and the package name of the app (e.g. developers). This location is recommended for caching downloaded content, as many devices have more external memory than internal memory.

6. Any public path on device's external memory

   Public external folders allow easy content sharing between apps and copying content from PC to a familiar location such as the /Movies folder, but reading from there requires READ_EXTERNAL_STORAGE permission (WRITE_EXTERNAL_STORAGE for writing) that needs to be explicitly requested from user, starting from Android 6.0. This location is recommended for playing content that is sideloaded by end users either by copying to device via a USB cable or read directly from a removable memory card.

> In case your app is intended for playing a couple of short fixed 360 videos or a fixed set of 360 photos, then you should consider embedding the content into the app. This approach provides several benefits:
> - Simpler content deployment without a streaming server and a content-delivery network (CDN)
> - Lower and more predictable content deployment cost - even FREE delivery via Google Play store
> - Built-in offline mode without making the UI more complex with content download and delete features
> - Guaranteed to have no buffering pauses during video playback
> 
> However, there are also some major drawbacks:
> - App installation package becomes large and potential users may skip the app based on its size
> - After watching the embedded content the whole app needs to be uninstalled to remove the content
> - Adding/updating content not possible without updating the app (many users will never update)
> - Only a limited amount of content can be embedded to the app
> 
> Typically one-shot apps that are intended for a particular event, product campaign, or offline use have embedded content. However, also apps that mostly use streamed content may include a few embedded items that are frequently needed and rarely updated, such as brand introduction, user tutorials, and menu backgrounds.

### Minimal Video Controls

![alt tag](https://cloud.githubusercontent.com/assets/12032146/20640544/41565e3e-b3e9-11e6-8c00-cf9662f088eb.png)

[View code](app/src/main/java/fi/finwe/orion360/sdk/pro/examples/minimal/MinimalVideoControls.java)

An example of a minimal Orion360 video player, with minimal video controls.

This example uses _MediaController_ class as a simple way to add controls into a 360 video player. This approach requires only a few lines of code: first a new media controller is instantiated and the _OrionVideoTexture_ that is associated with our Orion360 view is added to it as a media player to control. Next the Orion360 view is set as a UI anchor view where to position the control widget. Finally, a gesture detector is used for showing and hiding the controls when the Orion360 view is tapped (by default, the media controller automatically hides itself after a moment of inactivity).

The control widget includes play/pause button, rewind and fast forward buttons, a seek bar, and labels for elapsed and total playing time. If you want to customize the look&feel of the control widget or add your own buttons, see _CustomControls_ example where video controls are created from scratch.

> When seeking within a video, notice that it is only possible to seek to keyframes - the player will automatically jump to the nearest one. The number of keyframes and their positions depend on video content, used video encoder, and encoder settings. In general, the more keyframes are added the larger the video file will be. The Orion360 example video is mostly static and thus has very few keyframes, allowing the user to seek only to a few positions.

### Minimal VR Video File Player

![alt tag](https://cloud.githubusercontent.com/assets/12032146/20640590/f3459640-b3ea-11e6-9e9a-bd9851d7ef11.png)

[View code](app/src/main/java/fi/finwe/orion360/sdk/pro/examples/minimal/MinimalVRVideoFilePlayer.java)

An example of a minimal Orion360 video player, with VR mode enabled.

The most impressive way to experience 360 photos and videos is through virtual reality (VR). Unfortunately, most people do not have the necessary equipment yet. However, there is a _very_ cost efficient method: users can simply slide their existing smartphone inside a VR frame that essentially consists of a plastic or cardboard frame and a pair of convex lenses, and enable VR mode from an app that supports split-screen viewing.

Currently the most popular VR frame by far is Google Cardboard (https://vr.google.com/cardboard); millions of them have been distributed to users already. There are also plenty of Cardboard clones available from different manufacturers. It is a fairly common practice to create a custom-printed Cardboard-style VR frame for a dollar or two per piece, and give them out to users for free along with a 360/VR video app and content. That combo makes a really great marketing tool.

This example shows how to enable VR mode from an Orion360 video view for viewing content with Google Cardboard or other similar VR frame where smartphone can be slided in. The _SimpleOrionActivity_ class provides a convenience method that performs all necessary tasks:
- Configure horizontally split video view in landscape orientation
- Configure (and lock) the field-of-view into a realistic setting
- Configure VR frame lens distortion compensation for improved image quality
- Initialize the view orientation to World orientation ie. keep video horizon perpendicular to gravity vector
- Hide the system navigation bar for occlusion free viewing in devices where it is with by software
- Disable magnetometer from sensor fusion so that Cardboard's magnetic switch does not interfere with it
- Create a gesture detector for toggling VR mode on/off with long taps and a hint about it with a single tap

> For high-quality VR experiences, consider using a high-end Samsung smartphone and an active GearVR frame (you will also need to use the Pro version of the Orion360 SDK). The equipment cost will be significantly higher, but also the improvement in quality is remarkable and well worth it. GearVR frame has great optics, high speed sensors and touch controls built-in. They only work with specific Samsung models that have a number of performance tunings built-in and drivers for the GearVR frame. In general, Cardboard-style VR is recommended when you want to provide the VR viewing experience for a large audience by giving out free VR frames, while GearVR-style VR is best for trade shows, shop desks and one-to-one marketing where quality counts the most!

### Minimal Image Download Player

![alt tag](https://cloud.githubusercontent.com/assets/12032146/20640668/e1a00b9e-b3ec-11e6-94c7-7645f4ef490d.png)

[View code](app/src/main/java/fi/finwe/orion360/sdk/pro/examples/minimal/MinimalImageDownloadPlayer.java)

An example of a minimal Orion360 image player, for downloading an image file before playback.

> Notice that there is no example of a streaming player for 360 images, as an image always needs to be downloaded completely before it can be shown (tiled 360 images are not yet supported by Orion360 public SDKs).

Since downloading a large file will take a considerable amount of time, the example uses an AsyncTask to download the file in the background and updates download progress on screen. In this simple example, user needs to wait for the download to complete and the playback to begin as there is nothing else to do. However, you should consider placing a small download indicator somewhere in your app and allowing the user to continue using the app while the download is in progress. A high quality app has a download queue for downloading multiple files sequentially, is able to continue a download if it gets terminated early for example because of a network issue, allows user to cancel ongoing downloads, and uses platform notifications for indicating download progress and completion of a download. These features go beyond this example.

Image files are large and device models with small amounts of storage space tend to be popular as they are priced competitively. Consider saving the downloaded image file to external memory if it is currently present. It is also a good idea to offer a method for deleting downloaded content without uninstalling the whole app; this way users can still keep your app installed when they need to restore some storage space.

> The hardware limits for 360 image resolution come from available memory for decoding the image file and maximum texture size for rendering it. Notice that Orion360 automatically scales the image to fit to device's maximum texture size if necessary. In 2016, some popular older devices have 2048x2048 pixel texture size (4 megapixels), while new devices range from 4096x4096 (16 megapixels) to 16384x16384 pixels (256 megapixels). Obviously, depending on target device, the difference in rendered image quality can be quite remarkable with a high-resolution source image.

### Minimal Image File Player

![alt tag](https://cloud.githubusercontent.com/assets/12032146/20640714/bf81686c-b3ee-11e6-961a-6189dbdaf1bd.png)

[View code](app/src/main/java/fi/finwe/orion360/sdk/pro/examples/minimal/MinimalImageFilePlayer.java)

An example of a minimal Orion360 image player, for playing an image file from local file system.

This example showcases all supported file system locations and file access methods for image sources: the locations embedded to the app distribution packages, the app's private locations that become available after installation, and the locations that are more or less external to the app. To keep the example simple, only one location is active at a time and the others are commented out (you can easily select the active location from the source code). The locations are:

1. Application installation package's _/assets_ folder

   Private assets folder allows playing content embedded to the apps's own installation package (.apk). Notice 100MB .apk size limit in Google Play store. This is the recommended location when the application embeds image files to the installation package and _is NOT_ distributed via Google Play store (single large .apk file delivery), or contains only a few images.

2. Application installation package's _/res/raw_ folder

   Private raw resource folder allows playing content embedded to the app's own installation package (.apk). Notice 100MB .apk size limit in Google Play. Must use lowercase characters in filenames and access them without filename extension. This location is generally not recommended; use _/assets_ folder instead. **This location is currently not supported for images!**

3. Application expansion packages

   Private expansion package allows playing content embedded to the app's extra installation package (.obb). Up to 2 GB per package, max 2 packages. This is the recommended location when the application embeds lots of large image files to the installation package and _is_ distributed via Google Play store. Fairly complex but very useful solution. For more information, see https://developer.android.com/google/play/expansion-files.html **This location is currently not supported for images!**

4. Application's private path on device's internal memory

   Private internal folder is useful mainly when the app _downloads_ an image file for offline mode or to be cached, as only the app itself can access that location (exception: rooted devices). This location is recommended only if downloaded content files need to be protected from ordinary users - although the protection is easy to circumvent with a rooted device.

5. Application's private path on device's external memory

   Private external folder allows copying images back and forth via file manager app or a USB cable, which can be useful for users who know their way in the file system and the package name of the app (e.g. developers). This location is recommended for caching downloaded content, as many devices have more external memory than internal memory.

6. Any public path on device's external memory

   Public external folders allow easy content sharing between apps and copying content from PC to a familiar location such as the /Pictures folder, but reading from there requires READ_EXTERNAL_STORAGE permission (WRITE_EXTERNAL_STORAGE for writing) that needs to be explicitly requested from user, starting from Android 6.0. This location is recommended for playing content that is sideloaded by end users either by copying to device via a USB cable or read directly from a removable memory card.

> In case your app is intended for playing a couple of short fixed 360 videos or a fixed set of 360 photos, then you should consider embedding the content into the app. This approach provides several benefits:
> - Simpler content deployment without a web server and a content-delivery network (CDN)
> - Lower and more predictable content deployment cost - even FREE delivery via Google Play store
> - Built-in offline mode without making the UI more complex with content download and delete features
> - Guaranteed to have no buffering pauses during video playback
> 
> However, there are also some major drawbacks:
> - App installation package becomes large and potential users may skip the app based on its size
> - After watching the embedded content the whole app needs to be uninstalled to remove the content
> - Adding/updating content not possible without updating the app (many users will never update)
> - Only a limited amount of content can be embedded to the app
> 
> Typically one-shot apps that are intended for a particular event, product campaign, or offline use have embedded content. However, also apps that mostly use streamed content may include a few embedded items that are frequently needed and rarely updated, such as brand introduction, user tutorials, and menu backgrounds.

> *Current version of Orion360 SDK (Pro) for Android does not support playing 360 images directly from expansion packages. This feature will be added later in an update to the SDK. However, it is possible to embed content to this location, and copy the image file before it is used, for example to application's private path on external memory.*

Examples: Application Framework
-------------------------------

This category contains examples that show how Orion360 player can be configured into your own Activity or Fragment, instead of using provided _SimpleOrionActivity_ or _SimpleOrionFragment_ as a parent. These examples are also useful for understanding how exactly everything works under the hood, as the main Orion360 components are visible in the code examples and explained in the code comments.

### Custom Activity

![alt tag](https://cloud.githubusercontent.com/assets/12032146/20812172/44d7a48a-b819-11e6-9a97-9bf22da648ba.png)

[View code](app/src/main/java/fi/finwe/orion360/sdk/pro/examples/appfw/CustomActivity.java)

An example of a minimal Orion360 image player, implemented as a custom activity.

The following topics are covered:
- Propagating Activity lifecycle events to Orion360
- Verifying that Orion360 license file is present and valid (mandatory for rendering to work)
- What are _OrionView_, _OrionViewport_, _OrionScene_, _OrionPanorama_, _OrionTexture_, and _OrionCamera_
- How a typical mono spherical player is configured
- How sensor fusion and touch gestures can be used for controlling the camera
- Binding components together

> Notice that inheriting from SimpleOrionActivity provides you all of these, and more.

### Custom Fragment Activity

![alt tag](https://cloud.githubusercontent.com/assets/12032146/20812688/35a0092e-b81b-11e6-8bd4-60cb4989dcfe.png)

[View code](app/src/main/java/fi/finwe/orion360/sdk/pro/examples/appfw/CustomFragmentActivity.java)

An example of a minimal Orion360 image player, implemented as a custom fragment.

The following topics are covered:
- Propagating Fragment lifecycle events to Orion360
- Verifying that Orion360 license file is present and valid (mandatory for rendering to work)
- What are _OrionView_, _OrionViewport_, _OrionScene_, _OrionPanorama_, _OrionTexture_, and _OrionCamera_
- How a typical mono spherical player is configured
- How sensor fusion and touch gestures can be used for controlling the camera
- Binding components together

> Notice that inheriting from SimpleOrionFragment provides you all of these, and more.

Examples: Binding
-----------------

This category contains examples that show how Orion360 player can be configured for different purposes by creating a set of Orion objects and binding them together. The binding mechanism allows great flexibility while it still maintains ease of use - many complex tasks can be solved with only a few lines of code.

### Mono Panorama

![alt tag](https://cloud.githubusercontent.com/assets/12032146/20850222/c7cffd50-b8e2-11e6-8151-9723edf4f199.png)

[View code](app/src/main/java/fi/finwe/orion360/sdk/pro/examples/binding/MonoPanorama.java)

An example of bindings for creating a player for monoscopic full spherical videos.

In this example, the most typical Orion360 video player is configured: a full spherical equirectangular mono panorama player. In short, this configuration requires the following steps:

- Define one _OrionView_ in XML layout. This is where Orion360 will render its output.
- Create one _OrionViewport_ in Java code. This will define the internal layout of the _OrionView_. Bind it to _OrionView_.
- Create one _OrionScene_ in Java code. This will contain our 3D world. Bind it to _OrionView_.
- Create one _OrionCamera_ in Java code. This will project the 3D world onto a 2D surface. Bind it to _OrionView_.
- Create one _OrionPanorama_ in Java code. This will represent the spherical video surface in the 3D world. Bind it to _OrionScene_.
- Create one _OrionTexture_ in Java code. This will contain the latest decoded video frame. Bind it to _OrionPanorama_.
- Get _SensorFusion_ in Java code. That will rotate the camera according to device orientation. Bind it to _OrionScene_ AND _OrionCamera_.
- Create one _TouchControllerWidget_ in Java code. This will map touch gestures to camera control. Bind it to _OrionScene_ AND _OrionCamera_.

### Mono Panorama VR

![alt tag](https://cloud.githubusercontent.com/assets/12032146/20851127/247a2da6-b8e7-11e6-8198-063d4e81dba6.png)

[View code](app/src/main/java/fi/finwe/orion360/sdk/pro/examples/binding/MonoPanoramaVR.java)

An example of bindings for creating a VR player for monoscopic full spherical videos.

In this example, the most typical Orion360 VR video player is configured: a full spherical equirectangular mono panorama player. In short, this configuration requires the following steps:

- Define one _OrionView_ in XML layout. This is where Orion360 will render its output.
- Create two _OrionViewports_ in Java code. This will split the layout of the _OrionView_ horizontally. Bind them to _OrionView_.
- Create one _OrionScene_ in Java code. This will contain our 3D world. Bind it to _OrionView_.
- Create one _OrionCamera_ in Java code. This will project the 3D world onto a 2D surface. Bind it to _OrionView_.
- Create one _OrionPanorama_ in Java code. This will represent the spherical video surface in the 3D world. Bind it to _OrionScene_.
- Create one _OrionTexture_ in Java code. This will contain the latest decoded video frame. Bind it to _OrionPanorama_.
- Get _SensorFusion_ in Java code. That will rotate the camera according to device orientation. Bind it to _OrionScene_ AND _OrionCamera_.

### Stereo Panorama

![alt tag](https://cloud.githubusercontent.com/assets/12032146/20851231/d4457934-b8e7-11e6-98ce-93ae02985e6f.png)

[View code](app/src/main/java/fi/finwe/orion360/sdk/pro/examples/binding/StereoPanorama.java)

An example of bindings for creating a player for stereoscopic full spherical videos.

In this example, a slightly rare (but still important) Orion360 VR video player is configured: a full spherical equirectangular stereo panorama player (without VR mode, i.e. content is viewed in mono). In short, this configuration requires the following steps:

- Define one _OrionView_ in XML layout. This is where Orion360 will render its output.
- Create one _OrionViewport_ in Java code. This will define the internal layout of the _OrionView_. Bind it to _OrionView_.
- Create one _OrionScene_ in Java code. This will contain our 3D world. Bind it to _OrionView_.
- Create one _OrionCamera_ in Java code. This will project the 3D world onto a 2D surface. Bind it to _OrionView_.
- Create one _OrionPanorama_ in Java code. This will represent the spherical video surface in the 3D world. Bind it to _OrionScene_.
- Create one _OrionTexture_ in Java code. This will contain the latest decoded video frame. Bind it to _OrionPanorama_ using a method variant that allows configuring half of the texture to left eye and the other half to the right eye.
- Get _SensorFusion_ in Java code. That will rotate the camera according to device orientation. Bind it to _OrionScene_ AND _OrionCamera_.

> Here the purpose is to view stereo panorama video on phone/tablet screen (without VR glasses). The stereo effect is lost; only left eye image is used.

### Stereo Panorama VR

![alt tag](https://cloud.githubusercontent.com/assets/12032146/20851410/f286c2ee-b8e8-11e6-8ed3-dff4751fc2b1.png)

[View code](app/src/main/java/fi/finwe/orion360/sdk/pro/examples/binding/StereoPanoramaVR.java)

An example of bindings for creating a VR player for stereoscopic full spherical videos.

In this example, a fairly typical Orion360 VR video player is configured: a full spherical equirectangular stereo panorama player. In short, this configuration requires the following steps:

- Define one _OrionView_ in XML layout. This is where Orion360 will render its output.
- Create two _OrionViewports_ in Java code. This will split the layout of the _OrionView_ horizontally. Bind them to _OrionView_. Map first viewport to left eye and second viewport to right eye.
- Create one _OrionScene_ in Java code. This will contain our 3D world. Bind it to _OrionView_.
- Create one _OrionCamera_ in Java code. This will project the 3D world onto a 2D surface. Bind it to _OrionView_.
- Create one _OrionPanorama_ in Java code. This will represent the spherical video surface in the 3D world. Bind it to _OrionScene_.
- Create one _OrionTexture_ in Java code. This will contain the latest decoded video frame. Bind it to _OrionPanorama_ using a method variant that allows configuring half of the texture to left eye and the other half to the right eye.
- Get _SensorFusion_ in Java code. That will rotate the camera according to device orientation. Bind it to _OrionScene_ AND _OrionCamera_.

> It is **very** important that left and right eye images do not get swapped! There are many opportunities for human error in content creation, writing code, and even when placing the device into a VR frame. Two errors may cancel each other out. Pay attention and do test with VR glasses.

### Doughnut

![alt tag](https://cloud.githubusercontent.com/assets/12032146/20868798/f7a8ab40-ba6c-11e6-9b24-b49c6b932592.png)

[View code](app/src/main/java/fi/finwe/orion360/sdk/pro/examples/binding/Doughnut.java)

An example of bindings for creating a player for monoscopic doughnut videos.

In this example, the video player is otherwise typical but configured for doughnut shape video, which does not reach down to nadir nor up to zenith - there are round holes at the bottom and at the top (some cameras do not produce full spherical panoramas). In short, this configuration requires the following steps:

- Define one _OrionView_ in XML layout. This is where Orion360 will render its output.
- Create one _OrionViewport_ in Java code. This will define the internal layout of the _OrionView_. Bind it to _OrionView_.
- Create one _OrionScene_ in Java code. This will contain our 3D world. Bind it to _OrionView_.
- Create one _OrionCamera_ in Java code. This will project the 3D world onto a 2D surface. Bind it to _OrionView_.
- Create one _OrionPanorama_ in Java code. This will represent the spherical video surface in the 3D world. Bind it to _OrionScene_.
- Create one _OrionTexture_ in Java code. This will contain the latest decoded video frame. Bind it to _OrionPanorama_ so that you give two _Rects_ as parameters that define 1) the horizontal and vertical span of your camera (and thus the shape of the required panorama model) and 2) the part of the texture that you want to map on the doughnut surface (typically the full texture).
- Get _SensorFusion_ in Java code. That will rotate the camera according to device orientation. Bind it to _OrionScene_ AND _OrionCamera_.
- Create one _TouchControllerWidget_ in Java code. This will map touch gestures to camera control. Bind it to _OrionScene_ AND _OrionCamera_.

> Since the aspect ratio of a doughnut shape video is exceptionally wide, the maximum video resolution or the maximum texture size can become a limiting factor. One solution is to horizontally split the doughnut video to left and right halfs, and stack these into the video frame on top of each other (left=top, right=bottom). It is easy to re-combine the parts in the app by using Orion360 to map them onto the doughnut surface. A tiny seam may appear as a result.

### Rear-view Mirror

![alt tag](https://cloud.githubusercontent.com/assets/12032146/20859836/14f57a5c-b973-11e6-95db-ff6ba8e68121.png)

[View code](app/src/main/java/fi/finwe/orion360/sdk/pro/examples/binding/RearviewMirror.java)

An example of bindings for creating a player with a rear-view mirror.

In this example, two viewports and two cameras are used for simultanouesly showing two different views to the same equirectangular mono panorama video. In short, this configuration requires the following steps:

- Define one _OrionView_ in XML layout. This is where Orion360 will render its output.
- Create two _OrionViewports_ in Java code. This will define the internal layout of the _OrionView_ so that one viewport covers the whole _OrionView_ and another smaller one is put on top of it. Bind them to _OrionView_. Map the first viewport to the main camera and the second viewport to the rear-view camera.
- Create one _OrionScene_ in Java code. This will contain our 3D world. Bind it to _OrionView_.
- Create two _OrionCameras_ in Java code. These will project the 3D world onto 2D surfaces so that one camera looks ahead and the other camera to the opposite direction. Bind them to _OrionView_.
- Create one _OrionPanorama_ in Java code. This will represent the spherical video surface in the 3D world. Bind it to _OrionScene_.
- Create one _OrionTexture_ in Java code. This will contain the latest decoded video frame. Bind it to _OrionPanorama_.
- Get _SensorFusion_ in Java code. That will rotate both cameras according to device orientation. Bind it to _OrionScene_ AND both _OrionCameras_.
- Create one _TouchControllerWidget_ in Java code. This will map touch gestures to camera control. Bind it to _OrionScene_ AND both _OrionCameras_. Notice that zooming feature is disabled from rear-view camera.

> Here the focus is in the use of the binding mechanism, hence a couple of details have been obmitted compared to how a real mirror works: in reality when the user looks up the rear-view image should reflect bottom direction (here: top direction), and the actual mirror effect is also missing i.e. texts should show up backwards in the mirror-view (here: not mirrored).

### Overview

![alt tag](https://cloud.githubusercontent.com/assets/12032146/20865503/48680d78-ba1d-11e6-93b3-60aeee88271b.png)

[View code](app/src/main/java/fi/finwe/orion360/sdk/pro/examples/binding/Overview.java)

An example of bindings for creating a player with an overview image on top.

In this example, the view contains two viewports: the main viewport - which responds to sensors and touch - covers the whole view, and another smaller viewport is drawn on top of it for an overview image with original equirectangular projection. In short, this configuration requires the following steps:

- Define one _OrionView_ in XML layout. This is where Orion360 will render its output.
- Create two _OrionViewports_ in Java code. These will define the internal layout of the _OrionView_ so that the main viewport covers the whole view and another smaller one for the overview image is drawn on top of it. Bind them to _OrionView_.
- Create two _OrionScenes_ in Java code. These will contain our 3D worlds, one for the main viewport and another for the overview viewport. Bind them to _OrionView_.
- Create two _OrionCameras_ in Java code. These will project the 3D worlds onto 2D surfaces, one for the main viewport and another for the overview viewport. Bind them to _OrionView_.
- Create two _OrionPanoramas_ in Java code. These will represent the spherical spherical video surface for the main viewport and the plane video surface for the overview viewport. Bind them to _OrionScene_.
- Create one _OrionTexture_ in Java code. This will contain the latest decoded video frame. Bind it to both _OrionPanoramas_.
- Get _SensorFusion_ in Java code. That will rotate the camera according to device orientation. Bind it to main viewport _OrionScene_ AND main viewport _OrionCamera_.
- Create one _TouchControllerWidget_ in Java code. This will map touch gestures to camera control. Bind it to main viewport _OrionScene_ AND main viewport _OrionCamera_.

### Blending

![alt tag](https://cloud.githubusercontent.com/assets/12032146/20861365/f2fb9df4-b996-11e6-8a1b-66461636e223.png)

[View code](app/src/main/java/fi/finwe/orion360/sdk/pro/examples/binding/Blending.java)

An example of bindings for creating a player that blends two panoramas together.

In this example, two panoramas and two textures are used for blending a 360 image and a 360 video together by making one of them partially translucent and slightly smaller so that the larger one can be seen through it. In short, this configuration requires the following steps:

- Define one _OrionView_ in XML layout. This is where Orion360 will render its output.
- Create one _OrionViewport_ in Java code. This will define the internal layout of the _OrionView_. Bind it to _OrionView_.
- Create one _OrionScene_ in Java code. This will contain our 3D world. Bind it to _OrionView_.
- Create one _OrionCamera_ in Java code. This will project the 3D world onto 2D surface. Bind it to _OrionView_.
- Create two _OrionPanoramas_ in Java code. These will represent the spherical image surface and spherical video surface in the 3D world. Bind them to _OrionScene_.
- Create two _OrionTextures_ in Java code. These will contain the decoded image and the latest decoded video frame. Bind them to _OrionPanoramas_.
- Get _SensorFusion_ in Java code. That will rotate the camera according to device orientation. Bind it to _OrionScene_ AND _OrionCamera_.
- Create one _TouchControllerWidget_ in Java code. This will map touch gestures to camera control. Bind it to _OrionScene_ AND _OrionCamera_.
