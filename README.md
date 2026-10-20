# Student Manager Android (offline-first)

Android Studio project wrapping the Student Manager interface in a WebView. Student, attendance and marks data are stored locally on the device with `localStorage`; no account or server is required for these features.

## Included features
- Student add, edit, delete and search; branch filtering
- Daily attendance marking (Present / Absent / Leave) and attendance summaries
- Subject-wise marks entry with validation and percentage calculation
- Student, attendance, marks and combined printable reports; Android print dialog supports Save as PDF where available
- JSON backup and restore; student CSV export
- Offline, rule-based Assistant for supported student, attendance and marks queries
- Dark mode and local data persistence

## AI limitation
The Assistant is **not a generative AI model**. It runs offline and recognizes a limited set of commands. ChatGPT-like free-form answers require an online AI provider and a secure backend. Do not embed a private API key in the APK.

## Storage and privacy
Data is stored locally on this device and does not automatically sync to other phones. Keep a JSON backup before uninstalling the app or resetting the phone. This is a starter app and has not had a full security audit.

## Build APK
1. Install Android Studio and Android SDK (API 35 platform/build tools).
2. Open the `StudentManagerAndroid` folder in Android Studio.
3. Allow Gradle sync to complete.
4. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`.

The APK was not compiled in the creation environment because an Android SDK/Gradle build environment was unavailable. Please build and test on Android Studio before relying on the app.
