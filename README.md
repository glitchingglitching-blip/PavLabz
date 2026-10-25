# PavLabz Installer v0.14

[Download the installer APK](https://github.com/glitchingfork/PavLabz/releases/download/installer-v0.14/PavLabz-Installer-v0.14.apk).

Select **Contractors** or **Pavlov**. Contractors downloads the latest barrel-origin-corrected OBB pair, with the existing team colors, thin muzzle beams, recoil and firing-pushback changes.

| Game | Final destination | Files |
| --- | --- | --- |
| Contractors | `/sdcard/Android/obb/com.CaveManStudio.ContractorsVR/` | `main.181.com.CaveManStudio.ContractorsVR.obb`, `patch.181.com.CaveManStudio.ContractorsVR.obb` |
| Pavlov | `/sdcard/Android/obb/com.vankrupt.pavlov/` | Existing version 2397 main and patch |

1. Install/update the APK and select your game.
2. Keep that game closed. Complete the existing wireless-debugging pairing setup, then tap **Reconnect access** after updating so the new helper is used.
3. Tap **Download and install main + patch**. Choose whether to keep existing files as backups. **Replace anyway** forces a full copy even when files already match.
4. Wait for the installer to report both files installed and verified, then open the game.

The APK downloads split release assets and reconstructs the actual `.obb` files directly in the selected game's folder. Do not copy `.part001` files into the game folder yourself. Both downloads and installed files are checked for exact size, SHA-256 and CRC32. A normal APK needs the existing authorized debugging helper to write the game folder.

[Contractors data and verification manifest](https://github.com/glitchingfork/PavLabz/releases/tag/contractors-obb-v181-20261025). Original byte sizes are preserved: main 2,458,143,161 bytes; patch 2,459,398,806 bytes.

Build/lint, signature and local transfer/routing regression checks passed. The new Contractors download flow has not yet been tested end to end on a headset. Strayed remains marked SOON.

The installer source is attached to the v0.14 release. Build with JDK 17, Android SDK 35 and Gradle 8.9 (`gradle assembleDebug`). Use your existing signing key to update an installed copy; signing keys and local SDK paths are not included.
