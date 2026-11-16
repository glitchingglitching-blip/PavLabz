# PavLabz Installer v0.23

[Download the installer APK](https://github.com/glitchingglitching-blip/PavLabz/releases/download/installer-v0.23/PavLabz-Installer-v0.23.apk).

This version adds Airsoft Allstars as a third game choice. The APK uses the same signing certificate as the previous installer, so it can update an existing installation.

## Set up and install

1. Close the game, open the installer, and use **Reconnect access** after updating so the helper refreshes. If this is your first time, open wireless debugging settings, pair using the six-digit code, and select **Verify**.
2. Once verified, choose **Pavlov**, **Contractors**, or **Airsoft Allstars**. Use the corresponding **View OBB folder** button to inspect that game's folder.
3. Select **Download and install game files**. Keep the headset powered on and connected until verification finishes.

Airsoft requires installed game version **491**, including its existing main OBB. Missing map bundles will be installed. Before replacing anything, the installer checks that each existing target file matches either the supported 491 reference or the already-installed modified file. If a file differs, it stops and identifies the file; restore the supported game files before retrying. This reference came from a connected Quest and has not been independently verified as a clean store download.

Airsoft maps are installed first and the main OBB last, directly in `Android/obb/com.NekomataGamesLLC.AirsoftAllstars/`. Each file is staged in the same folder, checked by size, SHA-256 and CRC32, and replaced only after verification. An empty `update` folder is removed; a nonempty subfolder stops installation for inspection. Other files are left in place. If interrupted, retry with Airsoft closed; files already verified may be skipped.

The Pavlov choice still installs the older **2449 main and patch OBBs**; it does not install the newer 2479 PAK release. Contractors uses version 181. Existing overflow files remain protected.

## Downloads and compatibility

- [Airsoft 491 installer data](https://github.com/glitchingglitching-blip/PavLabz/releases/tag/airsoft-v491)
- [Pavlov 2449 OBB data](https://github.com/glitchingglitching-blip/PavLabz/releases/tag/obb-v2449)
- [Pavlov 2479 PAK test build](https://github.com/glitchingglitching-blip/PavLabz/releases/tag/pavlov-paks-v2479)
- [Contractors 181 OBB data](https://github.com/glitchingglitching-blip/PavLabz/releases/tag/contractors-obb-v181-20261025)

The Airsoft OBB and map files passed file integrity and asset checks, but the mods and the installer transfer have not been verified in an in-game headset session. APK build and lint, local transfer/folder regression tests, and an emulator interface and same-folder atomic file replacement check passed. Pairing and an end-to-end Quest transfer still need testing.

## Build

Build from `source` with JDK 17, Android SDK 35 and Gradle 8.9: `./gradlew assembleDebug lintDebug`. The source and tests are attached to the installer release. Private signing keys are not included.
