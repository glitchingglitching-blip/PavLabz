# PavLabz Installer v0.16

[Download the installer APK](https://github.com/glitchingfork/PavLabz/releases/download/installer-v0.16/PavLabz-Installer-v0.16.apk).

The APK updates v0.14 and v0.15 using the same signing certificate. No installer uninstall is needed.

## Verify access, then install

1. Update the installer and keep the game closed.
2. The first screen shows the pairing-code field, **Verify**, and a red **Not verified** circle.
3. Open **Wireless debugging setup / status**, then use **Pair device with pairing code** in Android settings. Keep that dialog open, enter its six-digit code, and tap **Verify**.
4. The circle turns green only after the installer confirms its transfer helper is ready. If already paired, use **Reconnect access**. Existing valid access is checked automatically when the installer resumes.
5. Once green, choose Pavlov or Contractors. Installation options appear on this same screen.
6. To repair an existing installation, check **Replace anyway - repair folder layout and reinstall every required OBB**.
7. Tap **Download and install game OBBs**, choose whether to retain older root OBB backups, and wait for **all game OBBs installed and verified**.

The original dark/red style is retained. The code, labeled status circle and Verify button stay together; navigation adapts as the window is resized. Disconnecting access hides the options again.

Normal installation and Replace anyway both repair misplaced subfolders. Verified OBBs move directly into the game folder. Incomplete, conflicting, older, or unknown files remaining in subfolders are preserved outside game OBBs at /sdcard/Download/PavLabz-recovery/(game package)/. Recovery copies are not automatically deleted. Redirected paths stop repair.

Replace anyway downloads and verifies every required OBB again, including matching files. Allow enough free headset storage for staged replacement; keeping backups uses additional space.

## Correct layout

Pavlov uses two files directly in its package folder:

    Android/obb/com.vankrupt.pavlov/
      main.2397.com.vankrupt.pavlov.obb
      patch.2397.com.vankrupt.pavlov.obb

Contractors uses two files directly in its package folder:

    Android/obb/com.CaveManStudio.ContractorsVR/
      main.181.com.CaveManStudio.ContractorsVR.obb
      patch.181.com.CaveManStudio.ContractorsVR.obb

Both game transfers contain main and patch only, with no separately downloaded overflow files. Neither game folder has an update subfolder. Download chunks such as .part001 are reconstructed into actual .obb files by the installer.

Expected size, SHA-256 and CRC32 are pinned for complete files and download parts. The final flat layout is checked before success. These are the packaged game versions, not a promise of compatibility with future game updates.

## Downloads

- [Pavlov main and patch](https://github.com/glitchingfork/PavLabz/releases/tag/obb-v2397-20261023)
- [Contractors files](https://github.com/glitchingfork/PavLabz/releases/tag/contractors-obb-v181-20261025)

## Validation

Build and Android lint passed. Local tests passed for both manifests and game routes, 24 normal/forced nested-folder repair scenarios, incomplete/conflicting/duplicate/older/unknown nested contents, replacement and rollback, and final size/SHA-256/CRC32. The APK signing certificate matches v0.14 and v0.15. No headset was connected for an end-to-end transfer test.

Pairing-screen positioning and red/green presentation were checked in an Android emulator; this does not confirm real Quest pairing or an end-to-end headset transfer.

Source and tests are attached to the release. Build with JDK 17, Android SDK 35 and Gradle 8.9 (gradle assembleDebug lintDebug). Use the existing signing key for installer updates. Keys and local SDK paths are not included in the archive.
