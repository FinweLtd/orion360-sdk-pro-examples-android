![alt tag](https://cloud.githubusercontent.com/assets/12032146/20262054/3d5de056-aa69-11e6-8ecd-31f02d96c4d8.png)

# Serving Ads

This document contains instructions on implementing a solution for serving commercial ads while user is viewing video content. Typically, content playback is interrupted temporarily for playing an ad, or an ad is played before/after user selected content.

## Why serve ads?

Nowadays, a fairly large percentage of Internet services are based on a business model, where users are offered free service if they agree to view ads. This approach seems to be well accepted by consumers, although some services also offer an optional ad-free service for paying customers (see SECURE_STREAMING.md for instructions on protecting content).

Typically, a service provider does not produce ads in-house nor negotiate with each individual advertiser - instead, the service provider selects an *ad network* and then integrates the ad network's pre-built solution to his own service. From then on, everything is automated: selecting ads to be played in general or for a particular user (personalized ads), deciding when to play an ad, downloading/streaming ad content, handling ad clicks, payments from the ad network to the service provider for ad displays/clicks, etc.

Overall, serving ads can be a fairly easy way to monetize a service that has a large user base, provided that the audience accepts ad-based commercialization. This requires selecting a suitable ad network that offers high quality and relevant ad content, and integrating their ad-serving mechanism properly to one's own service.

## Overview

As an example, we use Google Interactive Media Ads (IMA) as our ad network:

https://developers.google.com/interactive-media-ads

> "Interactive Media Ads (IMA) is a suite of SDKs that make it easy to integrate multimedia ads into your websites and apps. IMA SDKs can request ads from any VAST-compliant ad server and manage ad playback in your apps. IMA can also display companion ads, report metrics to ad servers, and incorporate key buying signals, such as Active View viewability, IDFA/ADID, and content targeting."

Google IMA supports both of the two primary approaches for serving ads:
a) Client-side: your app maintains control of content video playback, while IMA SDK handles ad playback. Ads play in a separate video player that is positioned on top of the app's video player.
b) Server-side: ad network is in control. Ad and your content video are *combined* on the ad network's server as they wish, and return a single video stream to your app for playback.

In a typical video player app, you can choose either approach and follow the ad network's instructions, nothing special there. However, Orion360 SDK Pro is primarily targeted for super wide-angle videos such as 180° and 360° content, while ads are typically created for fixed 16:9 aspect ratio. Obviously, switching between 360° and 16:9 content back and forth is not that simple from user experience point-of-view. Next, we will discuss the issues briefly.

### Client-side solution

In client-side solution, Orion360 SDK Pro will play video content in its own video player, and Google IMA will play an ad *in a separate video player that is positioned on top of the Orion360's view*. In general, this approach is OK, but there are a few notable problems:

1. Consider a case where the user is viewing 360° content in VR mode. In this mode, display is split to left/right eye halves. It makes a terrible user experience, if an ad player view suddenly appears on top: there is no left/right eye separation and no response to head movements. The user might quickly feel sick, have eye strain, or at least become very annoyed. This must not happen, so ads should not be played whenever VR mode is active. This is acceptable limitation for dual mode apps, but not for VR-only apps.

2. 360° content is often panned and zoomed actively using sensors and touch gestures. When an ad player view appears on top, Orion360's panning/zooming stops working (not necessary/supported by the ad player). That itself is not a problem, but user's ongoing touch gestures may be falsely interpreted as ad clicks, which of course would annoy the user. Yet, touch gestures cannot be completely disabled either, as the user should be able to click an ad. Some solution is needed for the transition phase.

> In theory, Orion360 SDK Pro could play 2D 16:9 ads as video sprites within the 3D world, for example, in a virtual TV that is placed inside a 360° environment. There could be a specific 360° background that is activated for the duration of a video ad. With this approach, ads would work also in VR mode. Panning and zooming would work as well. There could be multiple ad panels, one at each of the four main directions, to overcome the issue of user looking at the "wrong" direction and therefore not seeing the ad. While there are many benefits in this approach, such integration requires access to the ad player's video frames or video texture, which is usually not available. Ad network SDKs try to prevent tampering with the ads and are therefore quite restrictive with providing access to ad video content.

### Server-side solution

In server-side solution, Orion360 SDK Pro will play a video stream that is actively modified by the ad network's server. They will inject ads within the video stream. All video content will play within Orion360's video view, but there are a few notable problems:

1. Consider a case where the user is viewing 360° content and Orion360 is using a special projection to make the environment look natural. When ad server injects a 16:9 ad to the video stream, and Orion360 does not know anything about that, the ad will be heavily distorted by the projection. This will ruin the experience and neither the advertiser nor the user is happy. This can only be solved if the ad network somehow notifies the app when an ad begins and ends, so that the app can tell Orion360 to switch projection for the duration of the ad.
 
2. Consider a case where the user is viewing 360° content in VR mode. In this mode, display is split to left/right eye halves. It makes a terrible user experience, if the ad server suddenly injects an ad to the video stream - even if we could change the projection: there is no left/right eye separation and no response to head movements. The user might quickly feel sick, have eye strain, or at least become very annoyed. This must not happen, so ads should not be played whenever VR mode is active. But the ad network's servers do not know when VR mode is active in the app.

3. The user should be able to click and ad if s/he finds it interesting. When Orion360 is used for playing the video stream that contains ads, the developer of the app must handle touch clicks to make clicking an ad possible.

## Google IMA client-side integration

Google IMA client-side SDK for Android:
https://developers.google.com/interactive-media-ads/docs/sdks/android/client-side

The IMA SDK for Android uses Google ExoPlayer for video playback, not Android MediaPlayer. In this example, we too use ExoPlayer. Furthermore, there is an ExoPlayer IMA extension, which wraps the IMA SDK and makes everything a bit easier. We will use that by including the extension via .gradle file:
```
dependencies {
    implementation 'com.google.android.exoplayer:extension-ima:2.18.1'
}
```

Using the extension requires a few other changes to the project. Follow the instructions on the page linked above. In short:

* build.gradle
  - Change to use Java version 11
  - Enable multidex
  - Include IMA extension

* AndroidManifest.xml
  - Add INTERNET and ACCESS_NETWORK_STATE permissions
  
* Layout file
  - Create a layout file that contains com.google.android.exoplayer2.ui.StyledPlayerView view

* Strings
  - Add content URL
  - Add ad tag

Then, see example ads/GoogleImaIntegration.java for source code example.


