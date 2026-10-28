# PavLabz Installer v0.15

[Download the installer APK](https://github.com/glitchingfork/PavLabz/releases/download/installer-v0.15/PavLabz-Installer-v0.15.apk).

The APK updates v0.14 using the same signing certificate. No installer uninstall is needed.

## Repair an existing installation

1. Close Pavlov or Contractors.
2. Update the installer and select the game.
3. Tap **Reconnect access** once after updating to load the new transfer helper.
4. Check **Replace anyway - repair folder layout and reinstall every required OBB**.
5. Tap **Download and install game OBBs**. Choose whether to keep the older root OBBs as backups.
6. Wait for **all game OBBs installed and verified** before opening the game.

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

Build and Android lint passed. Local tests passed for both manifests and game routes, 24 normal/forced nested-folder repair scenarios, incomplete/conflicting/duplicate/older/unknown nested contents, replacement and rollback, and final size/SHA-256/CRC32. The APK signing certificate matches v0.14. No headset was connected for an end-to-end transfer test.

Source and tests are attached to the release. Build with JDK 17, Android SDK 35 and Gradle 8.9 (gradle assembleDebug lintDebug). Use the existing signing key for installer updates. Keys and local SDK paths are not included in the archive.
