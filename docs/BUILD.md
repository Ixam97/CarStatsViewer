# Building the App 

## Build Flavors

Car Stats Viewer is divided in different build flavors depending on it's intended use case:

* Stable: Intended for general use by everyone. Only uses completed and mostly tested features.
* Dev: Intended to only be used by developers while implementing new features. Many aspects are not stable for use and are only recommended for debugging.

<b>If you want to publish the app yourself as an internal test, only use the stable flavor from the master branch!

Branches other than `master` are considered to be in active development and contain unfinished and unstable features. Do not use these branches unless you know what you are doing!</b>

It is not required to refactor all packages in the Android Studio project. This might lead to issues once updates get released. Instead, just rename the `applicationId` in the `automotive\build.gradle`. This will lead to a custom package name when building the app without affecting source code.

Additionally, there will soon be a second flavor dimension to differentiate between the legacy version of Car Stats Viewer and the new version utilizing the Automotive Template Host. More on that when the time comes.

## API Credentials

The app has the ABRP Telemetry API integrated as standard. For this to work an API Key issued by Iternio is required. Iternio only wants one single API Key for each app, so please do not request you own API Key from them. Contact me directly if you want to enable ABRP Telemetry in your Fork and I will make sure to give you more details on how to implement the credentials. 

<b>Credentials should never be published to GitHub!</b>

## Installing the App on a Car

It is not possible to sideload apps on a production car in the same manner it is possible for Android phones. Therefore it is necessary to deploy it via the Google Play Store. Since the app does not meet the requirements enforced by Google, it cannot be published outside of an internal test.

To gain access to the Google Play Console to publish apps, you need to setup a developer account and pay a one time fee: https://support.google.com/googleplay/android-developer/answer/6112435

Within the Play Console, it is necessary to create a new form factor for "Automotive OS". Otherwise, the App Bundle will be rejected when uploaded.

Once setup, signed App Bundles can be uploaded as internal test releases.

## Create signed App Bundles

[Android Studio](https://developer.android.com/studio) is used to build the App. To be able to upload the App to the Google Play Store, a signed bundle is required: https://developer.android.com/studio/publish/app-signing

When building the bundle, select `stableRelease` as Build Variant. The created `.aab`-file can then be uploaded to the Play Console.

## Internal Testing

Internal tests are limited to a maximum of 100 testers. Their E-Mail-addresses used for the Google account in the car have to be added to the corresponding list. The testers can then opt-in via the link provided in the Testers-tab. After that the app has to be installed from the Play Store page from a web browser remotely, not via the built-in Play Store in the car since it is not listed publicly. The users need the link provided in the Play Console.
