![alt tag](https://cloud.githubusercontent.com/assets/12032146/20262054/3d5de056-aa69-11e6-8ecd-31f02d96c4d8.png)

# Serving Ads

This document contains instructions for serving commercial ads when users view video content via Orion360 player.

## Business Model Considerations

Serving ads can be a fairly easy way to monetize a service that has a large enough user base. Plenty of Internet services are based on a business model, where users are offered free service if they agree to watch ads. The tradeoff seems to be well accepted by consumers. In addition, many services also offer an optional upgrade to an ad-free service for those customers that are willing to pay for a subscription. This way, a service can monetize both user groups. 

One way to implement this is to turn adds on/off based on user account's subscription status. Another way is using different ad-free stream URLs that are protected and only available to users who have an active paid subscription (See SECURE_STREAMING.md for instructions on protecting content for subscription based customers).

From service provider's point of view, successful ad-based monetization requires a large user base (i.e. a popular service), but also smooth ad playback user experience, high-quality ad content, and reasonable amount/frequency of ads. Else, the ads will annoy users too much and they will switch to competing services. 

In practice, this involves selecting a suitable ad network that offers high quality and relevant/personalized ad content, integrating their ad-serving mechanism properly to one's own service, and reasonable configuration for it. In this document, we will go through these steps with one potential ad provider.

## Ad Network

Typically, a service provider does not produce ads in-house or negotiate separately with each individual advertiser. Instead, the service provider simply selects an *ad network* and then integrates their pre-built solution (SDK) into his own service. From then on, everything is automated: 
* Selling ads to the advertisers
* Managing ad video content on servers (ad catalog)
* Selecting which ads are to be played for a particular user based on his language, location, user profile etc.
* Selecting which file variant to download/stream, e.g. compatible video format, resolution, language, etc.
* Deciding when to play an ad, for example before (pre-roll) or after (post-roll) media content, or even temporarily interrupting the content for playing an ad (mid-roll)
* Actually downloading/streaming ad video content and playing it on screen
* Estimating viewability i.e. if the user really could watch the ad or not
* Handling analytics/statistics
* Handling ad clicks
* Handling payments from the ad network to the correct service provider for ad displays/clicks
* Etc etc.

Even though there are quite a lot of things going on, the ad networks have made everything fairly easy for service providers.

## Google IMA

As an example, we use Google Interactive Media Ads (Google IMA) as our ad network:

https://developers.google.com/interactive-media-ads

> "Interactive Media Ads (IMA) is a suite of SDKs that make it easy to integrate multimedia ads into your websites and apps. IMA SDKs can request ads from any VAST-compliant ad server and manage ad playback in your apps. IMA can also display companion ads, report metrics to ad servers, and incorporate key buying signals, such as Active View viewability, IDFA/ADID, and content targeting."

Important terms:

* *VAST* is a Video Ad Serving Template for structuring ad tags that serve ads to video players. Using an XML schema, VAST transfers important metadata about an ad from the ad server to a video player. - https://www.iab.com. For more information and an example, see https://en.wikipedia.org/wiki/Video_Ad_Serving_Template

* *IDFA*, the Identifier for Advertisers is a random device identifier assigned by Apple to a user’s device. Advertisers use this to track data so they can deliver customized advertising. The IDFA is used for tracking and identifying a user (without revealing personal information).
* *ADID*, the Advertising ID, serves similar purpose than IDFA, but comes from Google. For Android devices, Google Advertising ID is also known as *AAID*.

### Client-side vs. server-side

Google IMA supports both of the two primary approaches for serving ads:

* Client-side: your app maintains the control of content video playback, while IMA SDK handles ad playback related tasks. Ads may play in a separate video player that is positioned on top of the app's video player, or take control of the app's video player and utilize that for playing ads. Ad loader component handles everything behind the scenes, but you can observe and react to its events using callbacks, if necessary. From UI point of view, there are multiple layers on top of the video view. Some of them can be controlled by you, some others not. You will provide a handle to the correct location in the UI hierarchy for the ads and register views on top of the video player with proper explanations.


* Server-side: the ads and your content are *combined* on the ad network's server, which returns a single video stream to your app for playback. Changes to your app are minimal and the experience is TV-like. On the other hand, you have less control and the streams are combined on the Ad Manager servers, which may cause other issues (routing/latency etc.)

In a typical video player app, you can choose either approach and follow the ad network's instructions/examples to integrate the solution into your app. However, Orion360 SDK Pro is primarily targeted for playing super wide-angle videos such as 180° and 360° content, which are panned and zoomed interactively, and might be viewed in VR mode. Obviously, switching between such content and flat 16:9 aspect ratio ads, back and forth, is not always that simple. Next, we will discuss the issues briefly.

#### Client-side solution

In client-side solution, Orion360 SDK Pro will play video content in its own video player view. **In order to support ads, we must use ExoPlayer as the video engine.** Google IMA will play an ad by taking control of the same ExoPlayer instance, thus we must give it a handle to the ExoPlayer that we use with Orion360 and allow it load different content/control content playback. Google IMA will also require that we provide a ViewGroup where it can add a layer on top of the video player. It allows us to add more layers for our own UI components, but we must register each with an explanation, as in general we should avoid covering screen real estate over the ad player. Overall, client-side approach is fairly OK, but there are a few potential problems:

1. **VR MODE**: Consider a case where the user is viewing 360° content in VR mode. In this mode, display is split to left/right eye halves. It makes a terrible user experience, if an ad suddenly appears on top of the split view: suddenly there is no left/right eye separation and no response to head movements. The user might quickly feel sick, have eye strain, or at least become very annoyed. This must not happen, so the default assumption is that ads should not be played whenever VR mode is active, at least if a separate ad player view on top is being used. This is acceptable limitation for dual mode apps where VR mode is rarely activated, but unacceptable for monetizing VR-only apps.


2. **PROJECTION**: If the flat 16:9 ads are rendered on screen *via Orion360*, and the main media content uses wide-angle content with special projection, then the app must tell Orion360 to switch projection when an ad begins and ends. In order to do this, the app must be able to listen callbacks from the ad loader component and respond to them.


3. **TOUCH INPUT**: 360° content is often panned and zoomed actively using sensors and touch gestures. When an ad player view or ad button layer appears on top, Orion360's touch panning/zooming stops working (touch events are now captured by the ad network's view). That itself is not a major problem, but user's ongoing touch gestures may be falsely interpreted as ad clicks, which of course would annoy the user. Yet, touch gestures cannot be completely disabled either, as the user should be able to click an ad. This might not be a big issue, but app developers should be aware of it and consider usability testing.


> Orion360 SDK Pro can play 16:9 flat ads as video sprites within the 3D world, for example, in a virtual TV that is placed inside the 360° environment. There can be a specific 360° background that is activated for the duration of a video ad. With this approach, ads could work also in VR mode, at least in theory. There could be multiple ad panels, one at each of the four main directions, to overcome the issue of user looking at the "wrong" direction and therefore not seeing the ad. There could be extra panels for showing companion ads, which are sometimes offered in addition to the main ad video.
>
>While there are many benefits in this approach, there are also difficulties. Integration requires access to the ad player's video frames or video texture, which might not be available - it depends on the ad network's SDK. Notice that ad network try to prevent tampering with the ads and are therefore quite restrictive with providing access to ad video content. However, with current Google IMA SDK, this is actually possible: since the same ExoPlayer instance is used both for the media content and the ads, all video frames will be processed and played by Orion360. We just need to listen for the ad loader's events to switch projection between 2D ads and 360 content!
>
> One remaining problem is touch input: touch based panning and zooming need to work during ad playback. Potentially, this can be fixed by adding a transparent custom layer on top of the ad network's view, and passing (most) touch input events to Orion360.
> 
> Yet another problem are the UI components that the ad loader draws on top of the video player. These might contain for example textual links, ad video remaining time, and a skip button. They are rendered outside of Orion360 directly on screen, without any understanding of Orion360's potentially active VR mode / underlying 3D world. Hence, while playing ad video content in a 3D world may be feasible and even kind of fun user experience, playing ads in VR mode probably should be avoided.

#### Server-side solution

In server-side solution, Orion360 SDK Pro will play a video stream that is dynamically modified by the ad network's server. They will inject ads into the video stream. All video content will play within Orion360's video view, but there are a few potential problems:

1. **VR MODE**: Consider a case where the user is viewing 360° content in VR mode. In this mode, display is split to left/right eye halves. It makes a terrible user experience, if an ad suddenly appears in the video stream: flat 16:9 content will be stretched and distorted due to spherical projection used for 360° content. This must not happen, so the default assumption is that ads should not be played whenever VR mode is active. This is acceptable limitation for dual mode apps where VR mode is rarely activated, but unacceptable for monetizing VR-only apps.

2. **PROJECTION**: This is essentially the same problem as above, which appears also when VR mode is not active. As flat 16:9 ads are rendered on screen via Orion360, and the main media content uses wide-angle content with special projection, then the app must tell Orion360 to switch projection when an ad begins and ends. In order to do this, the app must be able to listen callbacks from the ad server component and respond to them. This needs to happen quickly, i.e. the latency of such callbacks need to be minimal and not depend on network quality/speed.

3. **TOUCH**: Usually, the user should be able to click an ad if s/he finds it interesting. When Orion360 is used for playing the video stream that contains ads, the developer of the app must handle touch clicks to make clicking an ad possible.

4. **CONTENT**: Since the ad network will modify the video stream, there is a risk that 360° video content will change in such a way that for example a visible seam appears, encoding quality is not good enough for VR use, etc.

> While both client-side and server-side approaches could work with Orion360, we consider client-side approach more suitable to work with Orion360 rendering, and focus on that in the rest of the document.

## Google IMA client-side integration

Google IMA client-side SDK for Android documentation is available here:

https://developers.google.com/interactive-media-ads/docs/sdks/android/client-side

We encourage you to read the documentation.

### ExoPlayer IMA extension

The IMA SDK for Android uses Google ExoPlayer for video playback, not Android MediaPlayer. Hence, we too use ExoPlayer in this document. Moreover, there is an **ExoPlayer IMA extension**, which wraps the IMA SDK and makes everything easier. We will use that by including the extension via .gradle file:
```
dependencies {
    implementation 'com.google.android.exoplayer:extension-ima:2.18.1'
}
```

In fact, using the extension requires a few other changes to the project. Follow the instructions on the page linked above to modify your app. In short:

* build.gradle
  - Change to use Java version 11
  - Enable multidex
  - Include IMA extension to the dependencies, as shown above

* AndroidManifest.xml
  - Add INTERNET and ACCESS_NETWORK_STATE permissions
  - Use multidex Application class (or one of the other options to install multidex)
  
* Layout file
  - Create a layout file that contains com.google.android.exoplayer2.ui.StyledPlayerView view, if you want to test Google IMA without Orion360 / have a separate ad player on top of Orion360 view.
  - For playing ads via Orion360, you can't use OrionViewContainer as-is. See orion_view_container_ima.xml for a working video player layout, and check activity_ad.xml as an example how it is applied to a view hierarchy.
  
* Strings
  - Add an ad tag. This is important for your final app, as you need to use a proper tag from your own account, not ours or Google's sample tag!

### Example 1: Google IMA standalone player

Source code: ads/GoogleImaStandalonePlayer.java

This example mostly follows Google IMA's official example. It shows how easily you can use ExoPlayer's StyledPlayerView in a layout and integrate ads to your app by using ImaAdsLoader component and a few small extra steps when configuring an ExoPlayer instance.

> Note: This example does not use Orion360 at all, and therefore renders 360° content flat. The purpose of this example is to show the minimal setup required for Google IMA.

### Example 2: Two isolated players

Source code: ads/GoogleImaTwoIsolatedPlayers.java

This example shows the most simplistic way of combining Google IMA ad playback with Orion360 based media content playback: the ad player is completely separate and simply drawn on top of Orion360's video player. An event listener is used for toggling player visibilities and play/pause states.

> This approach works fairly well with pre-roll ads, but is not a good choice for mid-roll and post-roll ads if you want to let Google IMA decide when to play an ad: you have to manually maintain two separate players in sync. In addition, this approach may result into buffering the media content twice because of the two players, unless you use a dummy video URL/file for the ad player.

### Example 3: Shared player, separate views

Source code: ads/GoogleImaSharedPlayerSeparateViews.java

This example shows a more capable integration, where a single ExoPlayer instance plays both ads and media content. The output (decoded video frames) are passed either to ExoPlayer's StyledPlayerView (ads) or Orion360 (media content). An event listener is used for swapping the target surface.

### Example 4: Google IMA via Orion

Source code: ads/GoogleImaViaOrion.java

This example uses Orion360 and its ExoPlayer for both ad and media content playback, and renders both within Orion360's own view. To be specific: Orion360 plays flat 2D 16:9 aspect ratio ads as well as wide-angle 360° media content. It could play 360° ads, too, if such ads existed. An event listener is used for swapping active projection.

> This approach requires using a special layout instead of the usual OrionViewContainer, since Google IMA uses 
> callback to retrieve a handle to a view hierarchy where it can add views that are related to ads. The example contains such layout, which app developers are free to customize.
