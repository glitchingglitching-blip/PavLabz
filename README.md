# PavLabz Installer v0.21


[Download the installer APK](https://github.com/glitchingglitching-blip/PavLabz/releases/download/installer-v0.21/PavLabz-Installer-v0.21.apk).

The APK updates previous installer versions using the same signing certificate. No installer uninstall is needed.

## Verify access, then install

1. Update the installer and keep the game closed. Tap **SETUP**, then **Reconnect access** after this update so the replacement helper is refreshed.
2. The first screen shows the pairing-code field, **Verify**, and a red **Not verified** circle.
3. Open **Wireless debugging setup / status**, then use **Pair device with pairing code** in Android settings. Keep that dialog open, enter its six-digit code, and tap **Verify**.
4. The circle turns green only after the installer confirms its transfer helper is ready. If already paired, use **Reconnect access**. Existing valid access is checked automatically when the installer resumes.
5. Once green, choose Pavlov or Contractors. Installation options appear on this same screen.
6. To repair an existing installation, check **Replace anyway - repair folder layout and reinstall every required OBB**.
7. Tap **Download and install game OBBs**, for Contractors choose whether to retain older root OBB backups, and wait for **all game OBBs installed and verified**.

The original dark/red style is retained. The code, labeled status circle and Verify button stay together; navigation adapts as the window is resized. Disconnecting access hides the options again.

### Verified transfer and folder access

Existing overflow1 and overflow2 files remain at their exact paths. The installer never moves or deletes them. No automatic recovery folders are created and no game contents are moved outside the selected game OBB directory.

After verifying access, each game has a View OBB folder button. The browser displays the actual selected game directory, opens subfolders, moves individual nested files into that same game root, and deletes explicitly selected files or empty folders after confirmation. Overflow files are protected from move and delete. Existing destination files are never overwritten by a manual move.

If a subfolder exists, installation stops before transfer and asks the user to inspect it. Main and patch are installed directly in the game root. Each download is checked for byte count, SHA-256 and CRC32, then both completed files are checked again on storage before success. Older Pavlov main/patch copies are deleted only after the replacement verifies. Failed transfers report an error; a completed main can remain alongside an older patch until retry succeeds.

Version 0.20 moved additional contents (including overflow files) into /sdcard/Download/PavLabz-recovery. Users affected by that version may need to manually restore required overflow files to their original game directory. This update does not automatically move recovery contents.

A verified transfer does not establish game or mod compatibility. Game closure needs game logs to diagnose.

## Correct layout

Pavlov uses two files directly in its package folder:

    Android/obb/com.vankrupt.pavlov/
      main.2449.com.vankrupt.pavlov.obb
      patch.2449.com.vankrupt.pavlov.obb

Contractors uses two files directly in its package folder:

    Android/obb/com.CaveManStudio.ContractorsVR/
      main.181.com.CaveManStudio.ContractorsVR.obb
      patch.181.com.CaveManStudio.ContractorsVR.obb

Both game transfers contain main and patch only, with no separately downloaded overflow files. Replacement main and patch files are written directly in the selected game folder. Existing overflow files stay at their original paths. Download chunks such as .part001 are reconstructed into actual .obb files by the installer.

Expected size, SHA-256 and CRC32 are pinned for complete files and download parts. The final flat layout is checked before success. These are the packaged game versions, not a promise of compatibility with future game updates.

## Downloads

- [Pavlov main and patch](https://github.com/glitchingglitching-blip/PavLabz/releases/tag/obb-v2449)
- [Contractors files](https://github.com/glitchingglitching-blip/PavLabz/releases/tag/contractors-obb-v181-20261025)

## Validation

Build and validation details are provided in the release. A headset transfer and game launch have not been tested for this version.

Pairing-screen positioning and red/green presentation were checked in an Android emulator; this does not confirm real Quest pairing or an end-to-end headset transfer.

Source and tests are attached to the release. Build with JDK 17, Android SDK 35 and Gradle 8.9 (gradle assembleDebug lintDebug). Use the existing signing key for installer updates. Keys and local SDK paths are not included in the archive.

## Build the complete project

The Android source, assets and tests are in the source folder.

    cd source
    ./gradlew assembleDebug lintDebug

On Windows, use gradlew.bat. Install JDK 17 and Android SDK platform 35, and set ANDROID_HOME to your SDK folder. The checked-in Gradle wrapper downloads Gradle 8.9. Development signing uses your local Android debug key; private signing keys and machine-specific SDK paths are excluded.

Local transfer regression tests are in source/part-test. Test-only UI instrumentation is in source/app/src/androidTest and is not included in the production installer APK.
