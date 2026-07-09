# Photos

A privacy oriented gallery application for Android built with Jetpack Compose and Kotlin. It is designed to act as a normal gallery when handed to others, while keeping private files secure under owner verification.

## Problem Identified :

Most of us already keep our highly private photos locked in a secure folder or vault anyway. The real problem happens when you need to show someone a specific photo, a meme, or a document. Whether it is a classmate, a faculty member, or a friend you do not entirely trust, handing over your unlocked phone triggers instant panic. 

You suddenly worry about those questionable memes, screenshots you forgot to clean up, or worse, that chat screenshot of the very person who is currently holding your phone. 

This app solves that scenario. You can quickly open the specific pictures you actually want to showcase inside this lightweight gallery and hand your phone over. Because it perfectly mimics the standard Photos UI, they will have no reason to suspect anything, and you can let them swipe freely without worrying.

## Features

- Photos Timeline: A native timeline showing date grouped media, complete with realistic bounded pinch to zoom pan gesture controls and video playback.
- Custom Mockups: Full control to customize mock categories, cities, names, and images on the search tab.
- Fast Local Picker: Integration of direct device local pickers that bypass cloud sync processes to add showcase media instantly.
- Peace of Mind Deletions: Deleting media in guest mode removes the reference from the guest screen immediately to assure the guest, while keeping the actual phone files untouched.
- Default Picker Source: Choose between System Gallery and Google Photos as the default picker in settings to instantly launch your preferred tool.
- Physical Device Locking: Automatically locks the device screen using Device Admin privileges whenever someone tries to exit, minimize, or swipe away from the app.
- Custom App Icons: Built in options to swap between preconfigured app icons, including standard photos and gallery options.

## Tech Stack

- Kotlin and Jetpack Compose
- Material You Dynamic Theme
- JSON serialization database for persistent decoy storage
- SharedPreferences for security options and preferences
- DevicePolicyManager API for physical device lock integration

## Installation

### Easy Installation (Recommended)

1. Go to the Releases tab of the GitHub repository page (https://github.com/bhartalayush/Guest-Gallery/releases).
2. Download the latest app-release-signed.apk file.
3. Open the downloaded APK file on your Android device and click install (ensure installation from unknown sources is allowed in your browser settings if prompted).

### Building from Source (Developer Installation)

1. Clone the repository:
   git clone https://github.com/bhartalayush/Guest-Gallery.git

2. Open the project in Android Studio (Ladybug or newer recommended).

3. Let Gradle sync and resolve project dependencies automatically.

4. Connect your Android device via USB (ensure USB debugging is enabled in Developer Options) or start a virtual device emulator.

5. Press the Run button in Android Studio, or build and install via terminal commands:
   - Command to build debug APK:
     ./gradlew assembleDebug
   - Command to install on connected device:
     adb install app/build/outputs/apk/debug/app-debug.apk

---

the amount of AI used in this project is the same amount as you use for Offer Letters
