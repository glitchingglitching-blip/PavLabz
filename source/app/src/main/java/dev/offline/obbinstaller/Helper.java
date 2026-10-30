package dev.offline.obbinstaller;

import android.net.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.zip.*;

/** Launched by authorized adb shell using app_process, never by the APK itself. */
public final class Helper {
    public static void main(String[] args) throws Exception {
        if (android.os.Process.myUid() != 2000 || args.length != 3)
            throw new SecurityException("Must start as adb shell with APK path and installer UID");
        int allowedUid = Integer.parseInt(args[1]);
        if (allowedUid < 10000 || allowedUid >= 100000) throw new SecurityException("Owner profile only");
        File apk = new File(args[0]); byte[] secret=java.nio.file.Files.readAllBytes(new File(args[2]).toPath());if(secret.length!=32)throw new SecurityException("Invalid secret");
        try (ZipFile zip = new ZipFile(apk);
             java.net.ServerSocket server = new java.net.ServerSocket(0, 4, java.net.InetAddress.getByName("127.0.0.1"))) {
            java.util.Map<String,Payload> payloads=GameCatalog.payloads(zip);
            System.out.println("PORT="+server.getLocalPort());System.out.println("READY uid=" + allowedUid + " games=" + payloads.size());
            while (true) {
                try (java.net.Socket client = server.accept()) {
                    client.setSoTimeout(5000);

                    DataOutputStream out = new DataOutputStream(client.getOutputStream());
                    try {
                        DataInputStream request = new DataInputStream(client.getInputStream());
                        LinkAuth.server(request,out,secret);client.setSoTimeout(30*60*1000);String operation=request.readUTF();if(operation.equals("REVOKE_V1")){out.writeUTF("STOPPING");out.flush();return;}
                        if(operation.equals("PING_V2")){String hash=request.readUTF();if(!payloads.containsKey(hash))throw new IOException("Unknown game payload; reconnect access");out.writeUTF("READY_V18:"+hash);out.flush();continue;}
                        if (!operation.equals("INSTALL_V1") && !operation.equals("INSTALL_FORCE_V1"))
                            throw new IOException("Unknown request");
                        Payload payload=payloads.get(request.readUTF());
                        if(payload==null)throw new IOException("Helper payload differs from installed APK; reconnect access");
                        ensureInstalled(payload.packageName);
                        File root = new File("/sdcard/Android/obb").getCanonicalFile();
                        File dir = new File(root, payload.packageName);
                        if (!dir.getCanonicalFile().equals(dir.getAbsoluteFile()))
                            throw new IOException("Refusing redirected destination");
                        if (!dir.isDirectory() && !dir.mkdir()) throw new IOException("Cannot create target OBB directory");
                        String[] expectedNames=new String[payload.files.size()];
                        for(int i=0;i<expectedNames.length;i++)expectedNames[i]=payload.files.get(i).name;
                        File downloadRoot=new File("/sdcard/Download").getCanonicalFile();
                        int recovered=payload.packageName.equals(PavlovReplacement.PACKAGE)?0:ObbPlacement.repairNested(dir,payload,new File(downloadRoot,"PavLabz-recovery/"+payload.packageName));
                        if(recovered>0)say(out,"Preserved "+recovered+" misplaced folder(s) in /sdcard/Download/PavLabz-recovery/"+payload.packageName);
                        say(out,"Checking all required OBB filenames in "+dir.getAbsolutePath());
                        ObbPlacement.checkNames(dir,payload.packageName,expectedNames,false);
                        java.util.List<String> oldNames = payload.packageName.equals(PavlovReplacement.PACKAGE)?new java.util.ArrayList<>():findOldFiles(dir, payload);
                        say(out, "PLAN:" + join(oldNames));
                        String choice = request.readUTF();
                        if (!choice.equals("BACKUP") && !choice.equals("REPLACE")) throw new IOException("Invalid backup choice");
                        for (Payload.Entry entry : payload.files) copy(dir, entry, out, choice.equals("BACKUP"), request, operation.equals("INSTALL_FORCE_V1"));
                        if(payload.packageName.equals(PavlovReplacement.PACKAGE)){
                            int moved=PavlovReplacement.recoverExtra(dir,expectedNames,new File(downloadRoot,"PavLabz-recovery/"+payload.packageName));
                            if(moved>0)say(out,"Moved "+moved+" other item(s), including any overflow files, to /sdcard/Download/PavLabz-recovery/"+payload.packageName+". The game may require its overflow files.");
                        }
                        ObbPlacement.checkNames(dir,payload.packageName,expectedNames,true);
                        say(out, "DONE: All required .obb names, location, size, SHA-256 and CRC32 verified in " + dir.getAbsolutePath() + (payload.packageName.equals(PavlovReplacement.PACKAGE)?". Any other contents, including overflow files, are moved to /sdcard/Download/PavLabz-recovery/com.vankrupt.pavlov/. The game may require overflow files.":""));
                    } catch (Exception e) {
                        say(out, "ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage() + ". Earlier completed files remain installed.");
                    }
                } catch (IOException e) { System.err.println("Client disconnected: " + e); }
            }
        }
    }
    static void say(DataOutputStream out, String text) throws IOException { out.writeUTF(text); out.flush(); }
    static String join(java.util.List<String> names) {
        StringBuilder result = new StringBuilder();
        for (String name : names) { if (result.length() > 0) result.append(", "); result.append(name); }
        return result.toString();
    }
    static java.util.List<String> findOldFiles(File dir, Payload payload) throws IOException {
        java.util.List<String> old = new java.util.ArrayList<>();
        for (Payload.Entry entry : payload.files) {
            String[] pieces = entry.name.split("\\.", 3);
            String kind = pieces[0]; long newVersion = Long.parseLong(pieces[1]);
            String prefix = kind + ".";
            File[] matches = dir.listFiles((parent, name) -> name.matches("(?:main|patch|overflow1|overflow2)\\.[0-9]+\\." +
                java.util.regex.Pattern.quote(payload.packageName) + "\\.obb") && name.startsWith(prefix));
            if (matches == null) throw new IOException("Cannot list OBB directory");
            for (File f : matches) {
                String[] oldParts = f.getName().split("\\.", 3);
                if (Long.parseLong(oldParts[1]) < newVersion || f.getName().equals(entry.name)) old.add(f.getName());
            }
        }
        if (old.size() > payload.files.size()) throw new IOException("Multiple older OBBs found. Clean up the game folder before retrying: " + join(old));
        return old;
    }
    static void ensureInstalled(String pkg) throws Exception {
        Process p = new ProcessBuilder("/system/bin/pm", "path", "--user", "0", pkg).redirectErrorStream(true).start();
        boolean found = false;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line; while ((line = r.readLine()) != null) if (line.startsWith("package:")) found = true;
        }
        if (p.waitFor() != 0 || !found) throw new IOException("Target game is not installed for owner profile");
    }
    static void copy(File dir, Payload.Entry e, DataOutputStream out, boolean keepBackup, DataInputStream network, boolean force) throws Exception {
        if(e.name.endsWith("."+PavlovReplacement.PACKAGE+".obb")){PavlovReplacement.copy(dir,e,out,network,force);return;}
        File target = new File(dir, e.name);
        if (!target.getCanonicalFile().equals(target.getAbsoluteFile())) throw new IOException("Redirected file");
        if(!force && target.isFile() && target.length()==e.size){
            say(out,"Checking existing file: "+e.name);
            if(Payload.hash(target).equals(e.sha256)){Payload.verifyStored(target,e);say(out,"Already installed and verified: "+e.name);return;}
        }
        say(out, "NEED:" + e.name);
        long size = network.readLong();
        if (size != e.size || size <= 0 || size > 20L * 1024 * 1024 * 1024) throw new IOException("Invalid OBB download size");
        File prior = findPrior(dir, e);
        if (target.exists() && prior != null) throw new IOException("Both the new filename and an older version exist; clean up manually first");
        if (target.exists()) prior = target;
        long required = size + 64L * 1024 * 1024;
        if (dir.getUsableSpace() < required) throw new IOException("Not enough storage. Free at least " + ((required-dir.getUsableSpace()+1048575)/1048576) + " more MiB before retrying. This file needs " + ((required+1048575)/1048576) + " MiB free for safe installation");
        File backup = prior == null ? null : new File(dir, ".backup-" + prior.getName());
        if (keepBackup && backup != null && backup.exists()) throw new IOException("Backup already exists: " + backup.getName());
        File temp = new File(dir, ".obb-install-" + UUID.randomUUID() + ".part");
        if (!temp.createNewFile()) throw new IOException("Cannot create staging file");
        try {
            say(out, "Installing downloaded file: " + e.name);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");java.util.zip.CRC32 crc=new java.util.zip.CRC32();
            long total = 0;
            try (FileOutputStream sink = new FileOutputStream(temp)) {
                byte[] buffer = new byte[1024 * 1024]; int n;
                while (total < size) {
                    n = network.read(buffer, 0, (int)Math.min(buffer.length, size - total));
                    if (n < 0) throw new IOException("Downloaded stream ended early");
                    total += n;
                    sink.write(buffer, 0, n); digest.update(buffer, 0, n);crc.update(buffer,0,n);
                }
                sink.getFD().sync();
            }
            String localHash = Payload.hex(digest.digest());if(!e.crc32.isEmpty() && !String.format(java.util.Locale.ROOT,"%08x",crc.getValue()).equals(e.crc32))throw new IOException("Reconstructed file CRC32 mismatch");
            String appHash = network.readUTF();
            if (!localHash.equals(e.sha256))throw new IOException("File does not match the publisher SHA-256. Existing OBB preserved.");
            if (!appHash.equals(localHash)) throw new IOException("Download integrity check failed");
            say(out, "Verifying stored file: " + e.name);
            if (!Payload.hash(temp).equals(localHash)) throw new IOException("Stored file hash mismatch");
            File held = prior == null ? null : new File(dir, ".obb-old-" + UUID.randomUUID() + ".part");
            if (prior != null && !prior.renameTo(held)) throw new IOException("Cannot hold the older OBB");
            if (!temp.renameTo(target)) {
                if (held != null && held.exists()) held.renameTo(prior);
                throw new IOException("Cannot finalize destination");
            }
            try {
                Payload.verifyStored(target,e);
            } catch(Exception verification) {
                if(!target.delete())throw new IOException("Final verification failed; cannot remove invalid target",verification);
                if(held!=null && !held.renameTo(prior))throw new IOException("Final verification failed; restore old file from "+held.getName(),verification);
                throw verification;
            }
            say(out, "Installed and verified in " + dir.getAbsolutePath() + ": " + e.name);
            if (held != null) {
                if (keepBackup) {
                    if (!held.renameTo(backup)) throw new IOException("New file is installed, but backup rename failed; old copy remains at " + held.getName());
                    say(out, "Old OBB kept on Quest: " + backup.getName());
                } else if (!held.delete()) throw new IOException("New file is installed, but old OBB could not be removed: " + held.getName());
            }
        } finally { if (temp.exists() && !temp.delete()) System.err.println("Left staging file: " + temp); }
    }
    static File findPrior(File dir, Payload.Entry e) throws Exception {
        String[] parts = e.name.split("\\.", 3);
        String kind = parts[0]; long version = Long.parseLong(parts[1]);
        File[] files = dir.listFiles((parent, name) -> ObbNames.sameKind(name,e.name));
        if (files == null) throw new IOException("Cannot list OBB folder");
        File prior = null;
        for (File f : files) {
            String[] old = f.getName().split("\\.", 3);
            if (Long.parseLong(old[1]) < version) {
                if (prior != null) throw new IOException("Multiple older files found for " + kind);
                prior = f;
            }
        }
        return prior;
    }
}
