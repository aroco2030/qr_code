# QR Code Generator (Android)

A small Android app that builds a QR code from five fields
(Material, Gas, Thickness, Test Number, Machine ID) and encodes them as
`material;gas;thickness;test;machineID` — the same schema as the original
Python/tkinter tool. You can save the QR to your gallery or share it.

## Getting the APK without installing anything (recommended)

1. Create a free account at https://github.com if you don't have one.
2. Create a new **empty** repository (e.g. `qr-generator`). Do **not** add a README.
3. Upload every file/folder from this project into the repo
   (GitHub web UI: "Add file" -> "Upload files" -> drag the whole folder in,
   then Commit). Keep the folder structure intact.
4. Go to the **Actions** tab. A workflow named **Build APK** runs automatically.
   Wait ~2-4 minutes for the green check.
5. Open the finished run, scroll to **Artifacts**, download **QR-Generator-apk**.
   Unzip it to get `app-debug.apk`.

## Install on your phone

1. Copy `app-debug.apk` to your phone (USB, email, or cloud).
2. Tap it. Android will ask to allow installing from this source ->
   enable "Allow from this source", then Install.
   (Settings > Apps > Special access > Install unknown apps, if it doesn't prompt.)
3. This is a **debug** build signed with a debug key — perfect for personal use.

## Building locally instead (optional)

Open the project in Android Studio (Giraffe or newer) and click Run,
or from the project root run:

```
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

## Notes / customising

- Change the data schema in `MainActivity.generate()`.
- Error-correction level is `L` (matches the original). For dirtier scanning
  conditions raise it to `M`/`Q`/`H` in `encodeAsQr()`.
- App name / icon: `res/values/strings.xml` and `res/.../ic_launcher*`.
