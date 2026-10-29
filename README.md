# PavLabz Installer v0.19


[Download the installer APK](https://github.com/glitchingglitching-blip/PavLabz/releases/download/installer-v0.19/PavLabz-Installer-v0.19.apk).

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

### Pavlov replacement fix

Pavlov replaces only main and patch in the game package OBB folder. Existing overflow1 and overflow2 files stay at their exact current paths and are never downloaded, overwritten, deleted, or moved by this installer.

The new main or patch is downloaded to a temporary file and checked for exact size, SHA-256 and CRC32 before replacement. A same-directory atomic replacement removes the old destination without moving it to a backup or recovery folder. Matching older main/patch versions and main/patch backup copies in the root, plus matching older main/patch files directly inside an update folder, are deleted after the new file verifies. Pavlov no longer offers the backup choice.

Pavlov no longer relocates entire subfolders. Existing overflow files and unrelated folder contents remain in place. An update folder containing overflow files is preserved; only an empty update folder can be removed. Newer-version main/patch files cause a version-mismatch stop rather than being deleted.

Contractors retains its existing folder-repair and optional-backup behavior. Verified misplaced OBBs move into its package folder; leftover nested content is preserved under /sdcard/Download/PavLabz-recovery/com.CaveManStudio.ContractorsVR/.

Replace anyway downloads and verifies every required OBB again, including matching files. Allow enough free headset storage for staged replacement; keeping backups uses additional space.

## Correct layout

Pavlov uses two files directly in its package folder:

    Android/obb/com.vankrupt.pavlov/
      main.2447.com.vankrupt.pavlov.obb
      patch.2447.com.vankrupt.pavlov.obb

Contractors uses two files directly in its package folder:

    Android/obb/com.CaveManStudio.ContractorsVR/
      main.181.com.CaveManStudio.ContractorsVR.obb
      patch.181.com.CaveManStudio.ContractorsVR.obb

Both game transfers contain main and patch only, with no separately downloaded overflow files. Replacement main and patch files are written directly in the selected game folder. Existing Pavlov overflow files and their locations are preserved. Download chunks such as .part001 are reconstructed into actual .obb files by the installer.

Expected size, SHA-256 and CRC32 are pinned for complete files and download parts. The final flat layout is checked before success. These are the packaged game versions, not a promise of compatibility with future game updates.

## Downloads

- [Pavlov main and patch](https://github.com/glitchingglitching-blip/PavLabz/releases/tag/obb-v2447)
- [Contractors files](https://github.com/glitchingglitching-blip/PavLabz/releases/tag/contractors-obb-v181-20261025)

## Validation

See release notes for validation of this version. Local tests passed for both manifests and game routes, 8 Pavlov replacement scenarios, a duplex transfer-handshake test, and 12 Contractors nested-folder repair scenarios, incomplete/conflicting/duplicate/older/unknown nested contents, replacement and rollback, and final size/SHA-256/CRC32. The APK signing certificate matches previous installer releases. No headset was connected for an end-to-end transfer test.

Pairing-screen positioning and red/green presentation were checked in an Android emulator; this does not confirm real Quest pairing or an end-to-end headset transfer.

Source and tests are attached to the release. Build with JDK 17, Android SDK 35 and Gradle 8.9 (gradle assembleDebug lintDebug). Use the existing signing key for installer updates. Keys and local SDK paths are not included in the archive.

## Build the complete project

The Android source, assets and tests are in the source folder.

    cd source
    ./gradlew assembleDebug lintDebug

On Windows, use gradlew.bat. Install JDK 17 and Android SDK platform 35, and set ANDROID_HOME to your SDK folder. The checked-in Gradle wrapper downloads Gradle 8.9. Development signing uses your local Android debug key; private signing keys and machine-specific SDK paths are excluded.

Local transfer regression tests are in source/part-test. Test-only UI instrumentation is in source/app/src/androidTest and is not included in the production installer APK.
