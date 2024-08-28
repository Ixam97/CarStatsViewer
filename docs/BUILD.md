# Building the App 

## Build Flavors

Car Stats Viewer is divided in different build flavors depending on it's intended use case:

* `stable`: Intended for general use by everyone. Only uses completed and mostly tested features.
* `dev`: Intended to only be used by developers while implementing new features. Many aspects are not stable for use and are only recommended for debugging.

In addition to that, there is also a second flavor dimension. This is used to build either the legacy or car app variant of Car Stats Viewer

* `legacy`: The classic variant of Car Stats Viewer. It is build using only custom activities, therefore not following the guidelines put in place by Google regarding In-Car-Apps.
* `carapp`: Implements the Google Automotive App Host. While-driving views are controlled and limited by the App Host to enforce drive distraction guidelines and apply the OEMs styling.

<b>If you want to publish the app yourself as an internal test, only use the stable flavor from the master branch!

Branches other than `master` are considered to be in active development and contain unfinished and unstable features. Do not use these branches unless you know what you are doing!</b>

It is not required to refactor all packages in the Android Studio project. This might lead to issues once updates get released. Instead, just rename the `applicationId` (either for the `legacy` or `carapp` build flavour) in the `automotive\build.gradle`. This will lead to a custom package name when building the app without affecting source code.

## Using additional features

Car Stats Viewer uses some third party APIs that require Tokens and credentials to function. These APIs can optionally be configure to be used or not. Those APIs are ABRP Telemetry, Google Firebase (for Crashlytics) and Mapbox (currently experimental).

### ABRP API Credentials

Car Stats Viewer has the ABRP Telemetry API integrated as standard. For this to work an API Key issued by Iternio is required. Iternio only wants one single API Key for each app, so please do not request you own API Key from them. Contact me directly if you want to enable ABRP Telemetry in your Fork and I will make sure to give you more details on how to implement the credentials.

<b>Credentials should never be published to GitHub!</b>

### Firebase

In the `gradle.properties`-file, set the property `useFirebase` to `true` if you want to use Firebase. Only then the required Plugins are used when building the app. This requires a `google-services.json` to be present in the app module. Refer to the Firebase documentation for more details on this: https://firebase.google.com/docs/android/setup

### Mapbox

Mapbox is currently only used for some testing and not a fully implemented feature in the app yet.
In the `gradle.properties`-file, set the property `useMapbox` to `true` if you want to use Mapbox. This property switches between two different Source Sets: One containing actual Mapbox code, the other functioning as a dummy. 
In order to be able to use Mapbox, you need to create the necessary API-keys for the download of the dependencies as well as for the usage of the maps in the app. Refer to the Mapbox documentation to find out how to acquire those: https://docs.mapbox.com/android/maps/guides/install/

Mapbox requires two different tokens:

* Mapbox Download Token: Create the property `MAPBOX_DOWNLOADS_TOKEN` in your `local.properties`-file (not added to git by default). This token is required to be able to download the Mapbox dependencies.
* Mapbox Access Token: If not already done so, create the file `/res/values/credentials.xml` as a resource file. **Do not add this file to git!** Create the string resource `mapbox_access_token`. This Token is required to be able to request map data at runtime.

## Installing the App on a Car

It is not possible to sideload apps on a production car in the same manner it is possible for Android phones. Therefore it is necessary to deploy it via the Google Play Store. The best way for personal use is to setup a so called internal test that is not reviewed by Google.

To gain access to the Google Play Console to publish apps, you need to setup a developer account and pay a one time fee: https://support.google.com/googleplay/android-developer/answer/6112435

Within the Play Console, it is necessary to create a new form factor for "Automotive OS". Otherwise, the App Bundle will be rejected when uploaded.

Once setup, signed App Bundles can be uploaded as internal test releases.

## Create signed App Bundles

[Android Studio](https://developer.android.com/studio) is used to build the App. To be able to upload the App to the Google Play Store, a signed bundle is required: https://developer.android.com/studio/publish/app-signing

When building the bundle, select `stableLegacyRelease` or `stableCarappRelease` as Build Variant. The created `.aab`-file can then be uploaded to the Play Console.

## Internal Testing

Internal tests are limited to a maximum of 100 testers. Their E-Mail-addresses used for the Google account in the car have to be added to the corresponding list. The testers can then opt-in via the link provided in the Testers-tab. After that the app has to be installed from the Play Store page from a web browser remotely, not via the built-in Play Store in the car since it is not listed publicly. The users need the link provided in the Play Console.
