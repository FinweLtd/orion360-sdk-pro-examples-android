![alt tag](https://cloud.githubusercontent.com/assets/12032146/20262054/3d5de056-aa69-11e6-8ecd-31f02d96c4d8.png)

# Examples for Orion360 SDK (Pro) for Android

This repository contains a set of examples for creating a 360 photo/video player using Orion360 SDK (Pro) for Android and Android Studio IDE.

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
  2. [Minimal Video Download Player](#minimal-video-download-player)

TODO
----

7. [Example: Minimal Video File Player](#example-minimal-video-file-player)
8. [Example: Minimal Video Controls](#example-minimal-video-controls)
9. [Example: Minimal VR Video File Player](#example-minimal-vr-video-file-player)
10. [Example: Minimal Image Download Player](#example-minimal-image-download-player)
11. [Example: Minimal Image File Player](#example-minimal-image-file-player)
12. [Example: Buffering Indicator](#example-buffering-indicator)
13. [Example: Preview Image](#example-preview-image)
14. [Example: Sensor Fusion](#example-sensor-fusion)
15. [Example: Touch Input](#example-touch-input)
16. [Example: Custom Controls](#example-custom-controls)
17. [Example: Projection](#example-projection)
18. [Example: Doughnut](#example-doughnut)
19. [Example: Screenshot](#example-screenshot)
20. [Example: Nadir Patch](#example-nadir-patch)
21. [Example: Director's Cut](#example-directors-cut)
22. [Example: Interactive Hotspots](#example-interactive-hotspots)

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

### Minimal Video Download Player

![alt tag](https://cloud.githubusercontent.com/assets/12032146/20632132/eb5799b2-b343-11e6-8828-4fa61144fa2e.png)

[View code](app/src/main/java/fi/finwe/orion360/sdk/pro/examples/minimal/MinimalVideoDownloadPlayer.java)

An example of a minimal Orion360 video player, for downloading a video file before playback.

Available network bandwith often becomes an issue when streaming video over the network (especially true with high-resolution 4k content). Unfortunately, saving a copy of a video file while streaming it is not possible with Android MediaPlayer as a video backend. Hence, if you need to obtain a local copy of a video file that resides in the network either for offline use or to be cached, download it separately as shown in this example.

Since downloading a large file will take a considerable amount of time, it needs to be done asynchronously. Here we use Android's DownloadManager service, which is recommended for video files (as an alternative, _MinimalImageDownloadPlayer_ shows how to download a file with own code). In this simple example, user needs to wait for the download to complete and the playback to begin as there is nothing else to do. However, you should consider placing a small download indicator somewhere in your app and allowing the user to continue using the app while the download is in progress. A high quality app has a download queue for downloading multiple files sequentially, is able to continue a download if it gets terminated early for example because of a network issue, allows user to cancel ongoing downloads, and uses platform notifications for indicating download progress and completion of a download. These features go beyond this example.

Video files are large and device models with small amounts of storage space tend to be popular as they are priced competitively. Consider saving the downloaded video file to external memory if it is currently present. It is also a good idea to offer a method for deleting downloaded content without uninstalling the whole app; this way users can still keep your app installed when they need to restore some storage space.

Example: Minimal Video File Player
----------------------------------

![alt tag](https://cloud.githubusercontent.com/assets/12032146/19034111/7b9fa784-896a-11e6-9fa9-67404a1df041.jpg)

[View code](app/src/main/java/fi/finwe/orion360/sdk/basic/examples/examples/MinimalVideoFilePlayer.java)

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

Example: Minimal Video Controls
-------------------------------

![alt tag](https://cloud.githubusercontent.com/assets/12032146/19034118/8a0634dc-896a-11e6-8878-8ecdc13549d4.jpg)

[View code](app/src/main/java/fi/finwe/orion360/sdk/basic/examples/examples/MinimalVideoControls.java)

An example of a minimal Orion360 video player, with minimal video controls.

This example uses _MediaController_ class as a simple way to add controls into a 360 video player. This approach requires only a few lines of code: first a new media controller is instantiated, and then Orion360 video view is added to it as a media player to control, and as a UI anchor view where to position the control widget. Finally, a gesture detector is used for showing and hiding the controls when the video view is tapped (by default, the media controller automatically hides itself after a moment of inactivity).

The control widget includes play/pause button, rewind and fast forward buttons, a seek bar, and labels for elapsed and total playing time. If you want to customize the look&feel of the control widget or add your own buttons, see _CustomControls_ example where video controls are created from scratch.

> When seeking within a video, notice that it is only possible to seek to keyframes - the player will automatically jump to the nearest one. The number of keyframes and their positions depend on video content, used video encoder, and encoder settings. In general, the more keyframes are added the larger the video file will be. The Orion360 example video is mostly static and thus has very few keyframes, allowing the user to seek only to a few positions.

Example: Minimal VR Video File Player
-------------------------------------

![alt tag](https://cloud.githubusercontent.com/assets/12032146/19034144/a45fb86c-896a-11e6-905c-c538897c20ad.jpg)

[View code](app/src/main/java/fi/finwe/orion360/sdk/basic/examples/examples/MinimalVRVideoFilePlayer.java)

An example of a minimal Orion360 video player, with VR mode enabled.

The most impressive way to experience 360 photos and videos is through virtual reality (VR). Unfortunately, most people do not have the necessary equipment yet. However, there is a _very_ cost efficient method: users can simply slide their existing smartphone inside a VR frame that essentially consists of a plastic or cardboard frame and a pair of convex lenses, and enable VR mode from an app that supports split-screen viewing.

Currently the most popular VR frame by far is Google Cardboard (https://vr.google.com/cardboard); millions of them have been distributed to users already. There are also plenty of Cardboard clones available from different manufacturers. It is a fairly common practice to create a custom-printed Cardboard-style VR frame for a dollar or two per piece, and give them out to users for free along with a 360/VR video app and content. That combo makes a really great marketing tool.

This example shows how to enable VR mode from an Orion360 video view for viewing content with Google Cardboard or other similar VR frame where smartphone can be slided in. In short, the example shows how to:
- Configure horizontally split video view in landscape orientation
- Configure (and lock) the field-of-view into a realistic setting
- Configure VR frame lens distortion compensation for improved image quality
- Initialize the view orientation to World orientation ie. keep video horizon perpendicular to gravity vector
- Hide the system navigation bar for occlusion free viewing in devices where it is made by software
- Disable magnetometer from sensor fusion so that Cardboard's magnetic switch does not interfere with it
- Create a gesture detector for toggling VR mode on/off with long taps and a hint about it with a single tap

> For high-quality VR experiences, consider using a high-end Samsung smartphone and an active GearVR frame (you will also need to use the Pro version of the Orion360 SDK). The equipment cost will be significantly higher, but also the improvement in quality is remarkable and well worth it. GearVR frame has great optics, high speed sensors and touch controls built-in. They only work with specific Samsung models that have a number of performance tunings built-in and drivers for the GearVR frame. In general, Cardboard-style VR is recommended when you want to provide the VR viewing experience for a large audience by giving out free VR frames, while GearVR-style VR is best for trade shows, shop desks and one-to-one marketing where quality counts the most!

Example: Minimal Image Download Player
--------------------------------------

![alt tag](https://cloud.githubusercontent.com/assets/12032146/19034160/b779c2e4-896a-11e6-978b-354ba1962177.jpg)

[View code](app/src/main/java/fi/finwe/orion360/sdk/basic/examples/examples/MinimalImageDownloadPlayer.java)

An example of a minimal Orion360 image player, for downloading an image file before playback.

> Notice that there is no example of a streaming player for 360 images, as an image always needs to be downloaded completely before it can be shown (tiled 360 images are not supported in the Basic version of the Orion360 SDK).

This example is similar to _MinimalVideoDownloadPlayer_, but showcases how to use _OrionImageView_ component instead of _OrionVideoView_ for showing a 360 image.

Since downloading a large file will take a considerable amount of time, the example uses an AsyncTask to download the file in the background and updates download progress on screen. In this simple example, user needs to wait for the download to complete and the playback to begin as there is nothing else to do. However, you should consider placing a small download indicator somewhere in your app and allowing the user to continue using the app while the download is in progress. A high quality app has a download queue for downloading multiple files sequentially, is able to continue a download if it gets terminated early for example because of a network issue, allows user to cancel ongoing downloads, and uses platform notifications for indicating download progress and completion of a download. These features go beyond this example.

Image files are large and device models with small amounts of storage space tend to be popular as they are priced competitively. Consider saving the downloaded image file to external memory if it is currently present. It is also a good idea to offer a method for deleting downloaded content without uninstalling the whole app; this way users can still keep your app installed when they need to restore some storage space.

> The hardware limits for 360 image resolution come from available memory for decoding the image file and maximum texture size for storing and rendering it. Notice that Orion360 automatically scales the image to fit to device's maximum texture size if necessary. In 2016, some popular older devices have 2048x2048 pixel texture size (4 megapixels), while new devices range from 4096x4096 (16 megapixels) to 16384x16384 pixels (256 megapixels). Obviously, depending on target device, the difference in rendered image quality can be quite remarkable with a high-resolution source image.

Example: Minimal Image File Player
----------------------------------

![alt tag](https://cloud.githubusercontent.com/assets/12032146/19034171/c8331e82-896a-11e6-970c-052b498a2344.jpg)

[View code](app/src/main/java/fi/finwe/orion360/sdk/basic/examples/examples/MinimalImageFilePlayer.java)

An example of a minimal Orion360 image player, for playing an image file from local file system.

This example is similar to _MinimalVideoFilePlayer_, but showcases how to use _OrionImageView_ component instead of _OrionVideoView_ for showing a 360 image.

This example showcases all supported file system locations and file access methods for image sources: the locations embedded to the app distribution packages, the app's private locations that become available after installation, and the locations that are more or less external to the app. To keep the example simple, only one location is active at a time and the others are commented out (you can easily select the active location from the source code). The supported locations are:

1. Application's private path on device's internal memory

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

> *Current version of Orion360 SDK (Basic) for Android does not support playing 360 images directly from the application installation package or expansion package. This feature will be added later in an update to the SDK. However, it is possible to embed content to these locations, and copy the image file before it is used, for example to application's private path on external memory.*

Example: Buffering Indicator
----------------------------

![alt tag](https://cloud.githubusercontent.com/assets/12032146/19034193/d91729aa-896a-11e6-8150-ed986d9483e3.jpg)

[View code](app/src/main/java/fi/finwe/orion360/sdk/basic/examples/examples/BufferingIndicator.java)

An example of a minimal Orion360 video player, with a buffering indicator.

A buffering indicator tells user that the video player is currently loading or preparing content, and playback will start/continue soon. This way user does not assume that she is supposed to do something more. It also shows that the application is working although nothing else seems to be happening on screen, which is quite important.

While the _MinimalVideoStreamPlayer_ example shows how to listen to buffering events and show/hide the buffering indicator accordingly, this example goes further to implement a buffering indicator that is shown also when the video view itself is being initialized, and that supports switching to VR mode where both left and right eye need their own copy of the indicator widget.

The buffering indicator can be easily realized with an Android progress bar widget. Take a look at the _activity_video_player.xml_ file, which uses a _FrameLayout_ to add an indeterminate progress bar widget on top of the Orion360 video view, centered on screen. For VR mode, there is a slightly more complex setup: a horizontal _LinearLayout_ contains two _RelativeLayout_ containers, which each take 50% of the width and set up a centered progress bar widget for left and right eye, respectively. When the buffering indicator needs to be shown, we check from the video view configuration if VR mode is currently active or not, and select whether to make VR mode or normal indicator visible, respectively. Finally, when user toggles between normal and VR mode, we must remember to update the buffering indicator if it is currently visible.

When video is being prepared for playback over the network, it can take a long time before Android MediaPlayer reports that buffering has started. Hence, it is a good idea to show the buffering indicator immediately after calling prepare() for a video view - without waiting for the callback that tells that buffering has started. Since the activity can get paused and resumed at any time, and the video playback is usually auto-started when the player activity is resumed, it is often simplest to show the buffering indicator in onResume() and hide it when the playback begins.

Unfortunately, some Android devices have a buggy implementation of video buffering events and the event that tells that buffering has stopped might never come! We have noticed this behavior occasionally when the player is buffering the very beginning of the video. To prevent the buffering indicator for staying on screen forever, you can for example use a simple handler that polls when the video playback has progressed, and thus ensures that the buffering indicator gets always removed when playback begins/continues.

Example: Preview Image
----------------------

![alt tag](https://cloud.githubusercontent.com/assets/12032146/19034201/e5e86ba8-896a-11e6-978d-be7384be2f08.jpg)

[View code](app/src/main/java/fi/finwe/orion360/sdk/basic/examples/examples/PreviewImage.java)

An example of a minimal Orion360 video player, with a preview image.

The preview image is a full-size equirectangular image overlay on top of the main image/video layer (notice the difference to tags, which are origin-facing rectilinear images that cover only _a part_ of the main image/video layer).

Similar to tags, the alpha value of the preview image can be freely adjusted. Hence, it is possible to completely cover the main image/video layer, add a semi-transparent layer, or cross-fade between image and video (or two images when using OrionImageView instead of OrionVideoView). Furthermore, with a PNG image that has transparent areas, only selected parts of the main layer can be covered.

> The preview image layer does not support video; only an image can be used. Therefore, it is not possible to cross-fade from one video to another. The underlying reason is that many Android devices do not support playing more than one video at a time. However, you can always fade to a preview image, quickly switch the video URI in the main layer, and then fade back to the video layer.

The typical use case is to add a preview image that is shown in the beginning while the video is still being buffered from the network. This is very useful. For example, the image could contain a brand logo, instructions for panning and zooming within the 360 view, or a reminder about placing the device inside a VR frame.

In fact, the feature is even more versatile than that. Here are a few ideas:
* Show an image also when the video completes to thank users for watching and to instruct what to do next.
* If you have a video playlist, show a hero image of the next video while buffering it.
* Show an image when user pauses the video, when the player stops for buffering, or when network is lost.
* Dim video easily by adjusting preview image alpha and _NOT_ setting a preview image at all.
* Add a color overlay FX with a single-color preview image and a small alpha value.
* Use a binocular style mask image as an FX that ensures that users focus on something important.
* Show dynamically loaded ads during video playback.
* Create a slideshow with cross-fade effect using OrionImageView, preview image, and an audio track.

In this example, the main use case of the feature is demonstrated: a preview image is shown while video is being buffered over the network, and when video playback begins the preview image is immediately cross-faded to the video layer.

Example: Sensor Fusion
----------------------

![alt tag](https://cloud.githubusercontent.com/assets/12032146/19034208/f4220166-896a-11e6-89b8-0c899729ce33.jpg)

[View code](app/src/main/java/fi/finwe/orion360/sdk/basic/examples/examples/SensorFusion.java)

An example of a minimal Orion360 video player, with sensor fusion control.

By default, the 360 view is automatically panned based on device orientation changes. Hence, to look at a desired direction, user can turn the device towards that direction, or when viewing through a VR frame, simply turn her head. 

> This feature requires movement sensors to operate - most importantly, a gyroscope. Unfortunately, not all Android devices have one. The feature is automatically disabled on devices that do not have the necessary sensor hardware, with a fallback to touch-only control.

The supported movement sensors include
* Accelerometer, which tells where is _Down_ direction, and linear acceleration
* Magnetometer, which tells where is _North_ direction, and the rotation about the device's own axis (slowly)
* Gyroscope, which tells rotation changes about the device's own axis (very rapidly)

Using data from all the three sensors, a sophisticated in-house developed sensor fusion algorithm calculates device orientation several hundreds of times per second. Considering the complexity of the topic, this works surprisingly well. Beware that there are hardware specific differences on the data rate, data accuracy, and calibration quality, as well as local magnetic interference that may change significantly when the device is moved just a few inches.

> If you experience issues with sensor fusion, it is usually because 1) one of the mandatory sensors is not present on the target device and sensor fusion is automatically disabled, or 2) magnetometer sensor is poorly calibrated; disable it or calibrate it by drawing 8-figures in the air with the device, rotating along its three axis.

The sensor fusion algorithm is also responsible for merging user's touch input drag events with the orientation calculated from movement sensor data (touch tapping events are handled separately elsewhere). Touch drag events are interpreted here as panning (drag), zooming (pinch), and rolling (pinch rotate). In practice, touch drag events update an offset that is applied to the calculated device orientation.

In short, the example shows how to:
* Manually disable sensor control, to enable touch-only mode
* Manually disable magnetometer input, to prevent issues with nearby magnetic objects and bad calibration
* Manually disable pinch rotate gesture, if you don't want this feature
* Manually configure pinch zoom gesture limits, or disable pinch zoom feature altogether
* Listen for device orientation changes (sensor fusion events), to implement custom features

> In mathematics, there are multiple alternatives for describing rotations. Probably the most well-known is Euler angles (yaw, pitch, roll). Unfortunately, Euler angle representation has severe issues, and thus professionally written algorithms typically use quaternions or rotation matrixes instead. Also the rotation order is _very_ significant. Hence, it is quite common to get more than confused when trying to figure out rotations! As a developer, you don't need to worry about these much as Orion360 SDK handles all the complexity on behalf of you. In addition, this example shows how to convert a quaternion representation of the current device rotation to angle degrees, which can be understood much more easily.

Example: Touch Input
----------------------

![alt tag](https://cloud.githubusercontent.com/assets/12032146/19034213/012d3f2e-896b-11e6-9b45-e6328a185a32.jpg)

[View code](app/src/main/java/fi/finwe/orion360/sdk/basic/examples/examples/TouchInput.java)

An example of a minimal Orion360 video player, with touch input.

Touch screen is the primary interaction mechanism of modern smartphones and tablets. Orion360 image and video views reserve touch drag gestures for panning, zooming and rolling the view (see _SensorFusion_ example for details), but tapping events are fully configurable by the app developer. Typically apps utilize single tapping, double tapping, and long tapping events.

This example uses single tapping for toggling between normal and full screen view, double tapping for toggling between video playback and pause states, and long tapping for toggling between normal and VR mode rendering. These are tried-and-true mappings that are recommended for all 360/VR video apps, and explained in detail next.

In most video player applications, user is offered an option to toggle between a normal view with controls and another, occlusion-free view where all widgets are hidden. Some applications implement this with a maximize-button and a notification telling how to return from the full-screen view. That is all good, but it is easy to miss or forget the instructions, and what users tend to try first is tapping the screen. Thus, it is a good idea to map single tapping for toggling between normal and full-screen view. On Android, this means showing and hiding together 1) video controls, 2) system navigation bar, 3) system title bar, 4) system action bar, 5) custom application title bar. Of course, most applications use only some of these elements. The complexity increases when video controls are hidden with a timer, and some elements use transition animation. Notice that this example focuses on demonstrating the feature, not on user experience.

When something doesn't seem to work, users tend to try again with an amplified manner and multiple times. Since toggling between play and pause states are the most common control operation in a video player application, we recommend mapping double tapping events for this purpose. This allows controlling play/pause state even in full-screen mode (without bringing the controls in view), is easy to learn, and quicly becomes very natural. However, it is crucial to add a short animation that indicates the state change when the double tapping event has been recognized. A professionally made application also shows a hint about this hidden feature when user is learning to use the app.

When user enters VR mode, all standard controls must be hidden and the whole screen reserved for split screen rendering. But when it is time to exit VR mode, it is not at all obvious to user how to do that! She will probably try to tap the screen, but we recommend not to exit VR mode from single tapping event, as it is very easy to accidentally tap the screen already when sliding the smartphone inside a VR frame. Instead, use single tapping event for showing a short-lived notification that hints about using long tapping for exiting VR mode - this prevents exiting VR mode accidentally, but allows users to find the way to exit VR mode with ease. Additional benefit is that long tapping can be used as a shortcut also for enabling VR mode (same gesture should always work both ways).

Sometimes a developer wants more refined control of touch events. Maybe he wants to reserve a part of the screen for a special action, or perform some operation when a particular area of the 360 content is tapped (notice the difference between the two).

In this example, left and right edge of the video view have hidden tapping areas that seek the video 10 seconds backward and forward, respectively. This is just an example of mapping different actions to tapping events based on _touch position on screen_.

To showcase tapping _something within the 3D scene_, a hotspot is added to the video view (tapping the hotspot area will trigger roll animation). Notice that with Orion360 SDK Basic, the developer must manually combine hotspot and tapping near to its location, whereas Orion360 SDK Pro has built-in 3D objects and callbacks for their tapping as well as gaze selection events.

> To keep the example simple, all tapping features work simultaneously. In a real application the tapping events must be filtered to prevent multiple actions occurring from one tapping event. For example, tapping a hotspot should not trigger toggling between normal and full-screen mode (unless you have created a hotspot just for that purpose!)

Example: Custom Controls
------------------------

![alt tag](https://cloud.githubusercontent.com/assets/12032146/19034225/10751826-896b-11e6-9719-9764cb05ec65.jpg)

[View code](app/src/main/java/fi/finwe/orion360/sdk/basic/examples/examples/CustomControls.java)

An example of a minimal Orion360 video player, with custom controls.

The _MinimalVideoControls_ example already showed how easy it is to add video controls to Orion360 video view, with just a couple of lines of code. However, the app developer frequently wants to customize the controls, for example to add a button for toggling video looping, full-screen view, VR mode, or projection. Also the buttons and seekbar look&feel are usually customized to reflect brand colors and style. This example showcases how a custom controller class can be written from scratch and easily swapped in place of Android MediaController - only the class name needs to be changed and _OrionVideoView_ passed as an additional parameter to make use of features that go beyond the Android media player.

In this example custom controls are implemented by extending the _FrameLayout_ class. Typically the custom controls class would be in its own source code file, but here it is placed as an inner class to the player activity to keep the whole example code in one place. The custom controls class uses the same principles for integrating with the video view than Android's own MediaController class: controllable media player is given as a parameter (here Orion video view), as well as an anchor view for positioniong the controls (again Orion video view). Finally, there is an additional method call just for passing Orion video view as itself, for gaining control of the features specific to Orion360 views (such as projection and VR mode).

The implementation contains a play/pause button, a seekbar, elapsed and total time labels, and an audio mute on/off button. All graphical elements are custom made, and interaction is built on top of the available APIs. You will find it easy to further customize and develop this controller for your own needs.

Example: Projection
-------------------

![alt tag](https://cloud.githubusercontent.com/assets/12032146/19034244/2532f7c4-896b-11e6-934e-e1744694ed30.jpg)
![alt tag](https://cloud.githubusercontent.com/assets/12032146/19034254/3113dab8-896b-11e6-909d-c6af8ed810a4.jpg)
![alt tag](https://cloud.githubusercontent.com/assets/12032146/19034266/3d8c78c2-896b-11e6-8cbc-f4eedc2b4516.jpg)

[View code](app/src/main/java/fi/finwe/orion360/sdk/basic/examples/examples/Projection.java)

An example of a minimal Orion360 video player, with different projections.

This is a fairly simple example that goes through all available video projections when the video view is tapped (in a real application you should avoid using tapping events for changing projection; use for example a button in video controls widget instead).

The supported projections and their main use cases are:
- Rectilinear projection. As its name implies, it keeps all lines straight and is the standard projection for viewing 360 content. This projection is active by default in Orion360 views.
- Littleplanet projection. Also known as stereographic (down) projection. A fun, creative projection that provides a birds-eye view that resembles looking through a fish-eye lens. Frequently used as an extra projection. Try this with your own videos!
- Source projection. The video frames are drawn on screen as they are, but looped from left and right edges. This projection shows the severe distortions of the equirectangular projection but allows to see the whole captured frame at once. Also useful for viewing non-panoramic content with Orion360 views (for example if your video mixes both types).

Example: Doughnut
-----------------

![alt tag](https://cloud.githubusercontent.com/assets/12032146/19034278/5153f09c-896b-11e6-8273-890220e129c4.jpg)

[View code](app/src/main/java/fi/finwe/orion360/sdk/basic/examples/examples/Doughnut.java)

An example of a minimal Orion360 video player, with doughnut video configuration.

While the full spherical (360x180) content is the primary target in panoramic photo and video shooting, there are camera solutions that do not capture the full sphere, and sometimes this is quite acceptable. For example, consider viewing a basketball game that is shot from the side of the basketball court - during the gameplay, there is little reason for wasting pixels to include the direction straight below the camera, which probably contains a bunch of cables anyway.

This example showcases how to configure Orion360 video view for a doughnut shape video source. Notice that Orion360 is quite flexible, and allows many different shapes using the same principles that are presented here. The configuration is done as follows:

- Typically panoramic content is projected on a spherical surface (consider you are inside a basketball, looking its inner surface from the center point - this is your canvas). With 3D graphics, this surface can be implemented with a polygon mesh. Orion360 views allow configuring limits for this mesh horizontally and vertically in degrees. By default, a full sphere is configured by setting the limits to [-180.0, 180.0] horizontally and [-90,90] vertically (zero angle refers to video frame center). For a doughnut shape video, we can use smaller vertical angles to limit our spherical canvas from top and bottom, resulting to a doughnut shape canvas.

- Since we now have a smaller canvas, we should also configure the surface model to use less resources by adjusting the mesh density, which can be thought of as a number of rows and columns on the sphere surface. Good rule of thumb is to have one row / column for each 6 degrees of field-of-view. The default number is thus 60 columns for 360 degrees horizontally, and 30 rows for 180 degrees vertically. For a doughnut shape video, the proper number of rows can be calculated by dividing the new vertical span by 6 and rounding it up.

- We can also configure which part of the video frame is mapped where on the canvas. In the normal case, we use the whole video frame to cover the canvas completely. However, doughnut shape videos tend to be really wide, and the maximum width supported by hardware video decoder easily becomes a limiting factor for video quality. You can make much better use of the video frame by splitting your doughnut video to left and right halves and stacking them in the video frame. Then, in the player application, use Orion360 to map the top part of the video texture to left side of the doughnut and bottom part of the video texture to right side of the doughnut.

- Now that we have a doughnut shape canvas, the user should not be able to pan outside of the canvas or else black areas will become visible! Fortunately, Orion360 allows configuring limits for the viewable scene: when user is about to pan too far, the panning will stop automatically (and the view will be dragged along with the device).

- There is one more important thing: how to handle rolling the device between landscape and portrait orientations? Surely a wider vertical span will be rendered when device is rotated from landscape to portrait, so this action should reveal some black areas? The solution in Orion360 is to use 16 control points at the edges of the viewport to automatically adjusts the zooming (field-of-view) so that black areas won't be revealed. Another option is to disable the part of the sensor fusion algorithm that tells which way is down, and thus prevent the issue from appearing at all.

> If you wish to use doughnut video in VR mode, consider applying a nadir and zenith patches instead of limiting the viewable scene.

Example: Screenshot
-------------------

![alt tag](https://cloud.githubusercontent.com/assets/12032146/19034298/647beea4-896b-11e6-8aad-def7f1cefc05.jpg)

[View code](app/src/main/java/fi/finwe/orion360/sdk/basic/examples/examples/Screenshot.java)

An example of a minimal Orion360 video player, with screenshot capture by tapping.

If you wish to capture a screenshot directly from the Orion360 renderer, this example shows how to do it. There are two alternatives, a synchronous method call that captures the screenshot immediately and returns it as a bitmap, and an asynchronous alternative that uses a callback for providing the bitmap when the screenshot is ready. The example saves the captured screenshots as PNG images.

> Orion360 SDK Basic uses OpenGL ES 2.0 that does not support fast asynchronous reading from GPU memory to CPU memory via pixel buffer objects (PBO). Hence, the implementation is slow and stops the renderer for the duration of the screen capture. As such, it is useful mostly for development purposes. Orion360 SDK Pro uses OpenGL ES 3.0, and does not suffer from this issue.

Example: Nadir Patch
--------------------

![alt tag](https://cloud.githubusercontent.com/assets/12032146/19034314/71a43a78-896b-11e6-9112-57c838501cc3.jpg)

[View code](app/src/main/java/fi/finwe/orion360/sdk/basic/examples/examples/NadirPatch.java)

An example of a minimal Orion360 video player, with a nadir patch image.

Nadir patch is frequently used when 360 photo or video is captured with a camera setup that does not cover the full sphere (360x180). The purpose is to cover the hole in the natural direction (down) with content producer or customer brand logo.

Orion360 allows adding 2D image panes to the 3D world; these are called tags. Adding a tag only requires a path to the image file, a direction vector, and a scale factor - the image pane is automatically set at the proper distance from the origin and kept facing to user at all times.

This example uses the tag feature to add a nadir patch image on top of the video layer. While a tag image pane is always a rectangular area, the PNG image file format with alpha channel allows drawing non-rectangular shapes, here a circular patch.

Orion360 tags must be created during view initialization, but they can be manipulated later. Here a standard Android object animator is used for fading in the patch image when the video playback begins. It is also possible to use current viewing direction as an input for tag manipulation, as shown here by keeping the patch upright at all times.

Example: Director's Cut
-----------------------

![alt tag](https://cloud.githubusercontent.com/assets/12032146/19034322/7dc6f584-896b-11e6-9198-aba9692ede8a.jpg)

[View code](app/src/main/java/fi/finwe/orion360/sdk/basic/examples/examples/DirectorsCut.java)

An example of a minimal Orion360 video player, with forced view rotation control.

It is characteristic to 360 content that the end user is able to decide where to look at, and that there is (hopefully interesting) content available at all directions. While this freedom of exploration is one of the best treats of consuming content in 360 degrees, it is also contrary to traditional video and photo production where a director or a photographer decides the direction where to aim the camera and how to frame the shot, and thus carefully leads the user through the story by ensuring that she sees the relevant cues.

In 360, most of the time it is desirable to let the end user be in charge of the virtual camera ie. turn the device or pan the content with a finger. Yet there are occasions where a decision needs to be made on behalf of the user. The primary concern is that in case of video content the playback progresses at a constant pace, and in order to keep up the rhythm the story telling must proceed as well - but at the moment of a cut or a major event, the user might be looking at a 'wrong' direction and hence miss important cues, making the storyline feel very confusing!

The solution is to force the view to certain direction at a certain moment of time. This is, of course, a tool that should not be used without a very good reason, and that requires skill to do well.

The first decision to be made is the very first frame of the video. There are a few typical use cases that are covered in the example:

- Case A: The user is holding the device in hand at some random angle, but presumably at an orientation that feels comfortable to her. In this case, we want to rotate the view so that viewing begins from the center of the video, ie. from the 'front' direction of the content. The experience would be the same for a user who is sitting in a bus and looking down-forward to a device that lies on her hand that is resting on her knees, and for a user who is lying on a sofa and looking up-forward to a device held with a raised arm. This is also the default configuration for Orion360, and the developer needs to do nothing to accomplish this.
- Case B: In case the director wants to make an artistic decision in the opening scene, she might want to force the view away from the 'front' direction of the content, to make the viewer first slightly confused and to find the 'front' direction where the action mostly takes place by panning the view there herself. This would be a rather rarely used effect and a variation of Case A.
- Case C: If the user makes use of a VR frame to view the content, the solution presented in Case A is not appropriate. It is crucial that the nadir is aligned with user's perception of 'down' direction, and also the horizon line appears to be in its natural place.
- Case D: Similar to Case B, the director may want to start from a certain viewing direction. However, when using a VR frame, only the yaw angle (azimuth/compass angle) should be enforced, to keep the content aligned with the user's perception of 'down' direction at all times.

After the question of the initial viewing rotation is settled, the director may want to add some additional forced viewing directions. The most suitable places are when there is a cut and the viewer is taken to another place and time anyway - it is not that disturbing if also the viewing direction is re-oriented at the same exact moment.

In order to perform such operations during video playback, we need to listen to the video position and check when a predefined moment of time has been reached. Unfortunately, the Android media player backend does not provide frame numbers, and even video position must be queried via polling. The example shows how to rotate the camera at certain positions of time (with respect to video player position).

Finally, the director may want to perform animated camera operations, such as panning and zooming. These are somewhat controversial, but feel fairly good when the user is not taken completely out of control. Hence, we perform the panning and zooming as small animated steps by always modifying the latest value towards the target, thus allowing simultaneous control by user. The example shows how to do this.

Example: Interactive Hotspots
-----------------------------

![alt tag](https://cloud.githubusercontent.com/assets/12032146/19034335/8aa0d1f8-896b-11e6-861a-1fb04902aa54.jpg)

[View code](app/src/main/java/fi/finwe/orion360/sdk/basic/examples/examples/InteractiveHotspots.java)

An example of a minimal Orion360 video player, with interactive hotspots.

This example initializes 4 hotspots ("Start" buttons) at front, back, left & right directions. It dims and pauses the video, and waits for user to trigger start event by interacting with one of the hotspots. To get user's attention, the "Start" buttons are continuously animated (floating).

There is a fifth hotspot that acts as a reticle, continuously showing the position where the user is looking/pointing at. When the reticle is moved close enough to one of the "Start" hotspots, a pre-selection animation begins (roll). If the user keeps looking at the "Start" button long enough, it is triggered and a post-selection animation begins (escape), video dimming is removed, and playback starts. However, if the user moves away from the "Start" button before the pre-selection animation ends, selection is canceled.

This is a fairly complex example. To structure information into easily digestable pieces, a simple Hotspot class is represented, and then improved by adding more and more features to it by the means of inheritance.

> While Orion360 SDK Basic allows creating interactive, animated hotspots that can be selected by tapping or gazing, the Pro version of the SDK provides better performance and makes implementing this feature _much_ easier for the developer, as it has built-in support for 3D objects that can be selected by tapping or gazing, pie-style animations for custom timed selection graphics, etc. The main difference is that with Basic version the developer needs to build this on the client side and make lots of calls from the UI thread, while Pro version provides these features built-in and runs them in the GL thread.
