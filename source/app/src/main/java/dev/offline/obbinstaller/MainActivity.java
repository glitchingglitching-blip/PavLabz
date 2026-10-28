package dev.offline.obbinstaller;

import android.app.Activity;
import android.os.Bundle;
import android.net.*;
import android.view.WindowManager;
import android.widget.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {
    String selectedGame=GameCatalog.PAVLOV;
    LinearLayout optionsPanel,installPanel,gameChoices,setupPanel; android.view.View accessIndicator; TextView accessLabel;
    Button verifyButton,navGames,navInstall; EditText pairingCode; volatile boolean accessVerified,checkingAccess; boolean gameChosen; int accessEpoch;
    TextView status; EditText savedHost; ScrollView menuScroll; boolean advancedVisible;
    CheckBox forceReplace; volatile boolean forceReinstall; Button install; ProgressBar progress; TextView progressText; Button success;
    volatile boolean busy; WirelessAdb adb; Button startHelper; android.net.nsd.NsdManager nsd; android.net.nsd.NsdManager.DiscoveryListener discovery;
    void show(String message) { getSharedPreferences("diagnostics",0).edit().putString("status",message).apply(); runOnUiThread(() -> status.setText(message)); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        String requested=getIntent().getStringExtra("game");
        selectedGame=state!=null?state.getString("game",GameCatalog.PAVLOV):(requested==null?GameCatalog.PAVLOV:requested);
        gameChosen=state!=null&&state.getBoolean("chosen",false);
        try{GameCatalog.asset(selectedGame);}catch(IllegalArgumentException invalid){finish();return;}
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        status = new TextView(this);
        status.setTextSize(20);
        status.setText(GameCatalog.title(selectedGame)+"\nDestination: /sdcard/Android/obb/"+GameCatalog.target(selectedGame)+"\n\nClose "+GameCatalog.title(selectedGame)+" before installing. Pair once to enable access, then install the verified patch.\n\nALL REQUIRED OBBs — all downloads are checked against pinned SHA-256 hashes.");
        status.append(selectedGame.equals(GameCatalog.PAVLOV) ? "\n\nPavlov: main and patch directly in this folder." : "\n\nContractors: main and patch directly in this folder.");
        status.append("\nPavlov: only main and patch are replaced. Existing overflow files stay where they are. Contractors uses its existing folder repair.");
        install = new Button(this);
                install.setText("Download and install game OBBs");
        forceReplace=new CheckBox(this);forceReplace.setText("Replace anyway - repair folder layout and reinstall every required OBB");forceReplace.setTextColor(UiStyle.WHITE);forceReplace.setTextSize(18);forceReplace.setButtonTintList(android.content.res.ColorStateList.valueOf(UiStyle.RED));
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(1000);progress.setVisibility(android.view.View.GONE);
        progressText=new TextView(this);progressText.setTextSize(18);
        success=new Button(this);success.setText("Success — all game OBBs installed and verified");success.setVisibility(android.view.View.GONE);success.setOnClickListener(v->finish());
                Button settings = new Button(this); settings.setText("1. Open wireless debugging settings");
        settings.setText("1. Wireless debugging setup / status");
        settings.setOnClickListener(v -> {
          int wifi=android.provider.Settings.Global.getInt(getContentResolver(),"adb_wifi_enabled",0);
          new android.app.AlertDialog.Builder(this).setTitle(wifi==1?"Wireless debugging is enabled":"Wireless debugging is OFF")
           .setMessage("USB debugging and wireless debugging are separate. This Quest does not expose the direct wireless-settings shortcut.\n\nSettings opens in a separate task; Quest controls whether it appears beside this window. On the app-info page, tap OPEN to enter Android Settings (not Meta Settings). Then go to System > Developer options > Wireless debugging. If Developer options is hidden, open About device and tap Build number seven times. Enable it and select Pair device with pairing code. Keep that dialog open beside this app.\n\nEnter its six-digit code here. Pairing and connection ports are discovered automatically.\n\nIf the Developer screen has no Wireless debugging option, stop and report that; opening Settings alone does not grant access.")
           .setPositiveButton("Open Android Settings app info",(d,w)->{try {startActivity(new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:com.android.settings")).setPackage("com.android.settings").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT));}catch(Exception e){show("Cannot open developer settings: "+e);}})
           .setNegativeButton("Stay here",null).show();
        });
        EditText host=new EditText(this);savedHost=host;host.setHint("Headset IP address (or 127.0.0.1)");host.setText(getSharedPreferences("menu",0).getString("host","127.0.0.1"));
        EditText port=new EditText(this);port.setHint("Pairing port from pairing-code dialog");port.setInputType(2);
        EditText code=new EditText(this);code.setHint("Six-digit pairing code");code.setInputType(2);code.setSaveEnabled(false);code.setImportantForAutofill(android.view.View.IMPORTANT_FOR_AUTOFILL_NO);
        Button pair=new Button(this);verifyButton=pair;pairingCode=code;pair.setText("Verify");
        pair.setOnClickListener(v -> {if(busy){show("Finish installation before pairing again.");return;}String h=host.getText().toString().trim(),p=port.getText().toString().trim(),c=code.getText().toString().trim();if(p.isEmpty()){show("Tap Pair device with pairing code in wireless debugging settings first. Waiting to detect the pairing port. If discovery fails, use Advanced.");return;}if(!c.matches("[0-9]{6}")){show("Enter the six-digit pairing code.");return;} verificationBusy(true);setAccess(false); new Thread(() -> {try {if(adb==null)adb=new WirelessAdb(this);show("Pairing with the headset… Keep the system pairing dialog open.");adb.pair(h,validPort(p),c);runOnUiThread(() -> { code.setText(""); verificationBusy(false); startHelper.performClick(); });show("Paired. Finding the connection and starting access automatically…");}catch(Exception e){verificationBusy(false);setAccess(false);show("Pairing failed: "+e+"\nOpen a fresh pairing-code dialog and enter its NEW code. Do not close that dialog while pairing.");}},"adb-pair").start();});
        EditText connection=new EditText(this);connection.setHint("Connection port from main wireless debugging screen");connection.setInputType(2);
        Button start=new Button(this);startHelper=start;start.setText("Reconnect access (after reboot)");
        start.setOnClickListener(v -> {if(busy){show("Finish installation before reconnecting.");return;}String h=host.getText().toString().trim(),p=connection.getText().toString().trim();verificationBusy(true);new Thread(() -> {try {if(helperReady()){setAccess(true);show("Helper ready. Close "+GameCatalog.title(selectedGame)+", then download and install.");return;}retirePreviousHelper();if(adb==null)adb=new WirelessAdb(this);show("Connecting wireless ADB…");if(!(p.isEmpty()?adb.connectTls(this,15000):adb.connect(h,validPort(p))))throw new IOException("ADB connection was not accepted");String uid=adb.shell("id -u").trim();if(!uid.equals("2000"))throw new IOException("Expected shell UID 2000; got "+uid);
          String dir="/data/local/tmp/quest-obb-"+java.util.UUID.randomUUID().toString().replace("-","");
          String apk=getApplicationInfo().sourceDir; byte[] secret=LinkAuth.nonce();String encoded=android.util.Base64.encodeToString(secret,android.util.Base64.NO_WRAP);getSharedPreferences("helper",0).edit().putString("secret",encoded).apply();
          String output=adb.shell("umask 077; mkdir '"+dir+"' && printf %s '"+encoded+"' | base64 -d > '"+dir+"/secret' && cp '"+apk+"' '"+dir+"/helper.apk' && chmod 444 '"+dir+"/helper.apk' && (nohup env CLASSPATH='"+dir+"/helper.apk' app_process /system/bin dev.offline.obbinstaller.Helper '"+dir+"/helper.apk' "+android.os.Process.myUid()+" '"+dir+"/secret' > '"+dir+"/helper.log' 2>&1 < /dev/null &)");
          Thread.sleep(1500);String log=adb.shell("cat '"+dir+"/helper.log'");java.util.regex.Matcher match=java.util.regex.Pattern.compile("PORT=([0-9]+)").matcher(log);if(match.find())getSharedPreferences("helper",0).edit().putInt("port",Integer.parseInt(match.group(1))).commit();if(!helperReady())throw new IOException("Helper startup: "+output+log);setAccess(true);show("Helper ready. Close "+GameCatalog.title(selectedGame)+", then download and install. Restart helper after reboot.");
        }catch(Exception e){setAccess(false);show("Connection/start failed: "+e+"\nYour pairing is saved. Check wireless debugging is enabled, then tap Reconnect access.");}finally{verificationBusy(false);}},"adb-start").start();});
                host.setVisibility(android.view.View.GONE);port.setVisibility(android.view.View.GONE);connection.setVisibility(android.view.View.GONE);
        Button manual=new Button(this);manual.setText("Advanced: manual address / ports");manual.setOnClickListener(v->{advancedVisible=true;host.setVisibility(android.view.View.VISIBLE);port.setVisibility(android.view.View.VISIBLE);connection.setVisibility(android.view.View.VISIBLE);});
        if(getSharedPreferences("menu",0).getBoolean("advanced",false))manual.performClick();
        code.setHint("Code from: Pair device with pairing code");pair.setText("Verify");
        optionsPanel=new LinearLayout(this);optionsPanel.setOrientation(LinearLayout.VERTICAL);optionsPanel.setVisibility(android.view.View.GONE);
        installPanel=new LinearLayout(this);installPanel.setOrientation(LinearLayout.VERTICAL);installPanel.setVisibility(android.view.View.GONE);
        gameChoices=new LinearLayout(this);gameChoices.setOrientation(LinearLayout.VERTICAL);
        TextView choose=new TextView(this);choose.setText("Choose your game");gameChoices.addView(choose);
        for(String game:new String[]{GameCatalog.PAVLOV,GameCatalog.CONTRACTORS}){
         Button pick=new Button(this);pick.setText(GameCatalog.title(game));pick.setOnClickListener(v->{if(busy||!accessVerified)return;selectedGame=game;gameChosen=true;installPanel.setVisibility(android.view.View.VISIBLE);show(GameCatalog.title(game)+"\nDestination: /sdcard/Android/obb/"+GameCatalog.target(game)+"\nMain and patch go directly in this folder. Pavlov replaces old copies without backups and preserves overflow files. Close the game before installing.");});gameChoices.addView(pick);
        }
        optionsPanel.addView(gameChoices);optionsPanel.addView(installPanel);
        for(android.view.View view:new android.view.View[]{forceReplace,install,progress,progressText,success})installPanel.addView(view);
        AppUpdates.attach(this,optionsPanel,()->busy);
        LinearLayout verificationRow=new LinearLayout(this);verificationRow.setOrientation(LinearLayout.HORIZONTAL);verificationRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        accessIndicator=new android.view.View(this);int dot=(int)(16*getResources().getDisplayMetrics().density);LinearLayout.LayoutParams dotParams=new LinearLayout.LayoutParams(dot,dot);dotParams.setMargins(12,8,12,8);verificationRow.addView(accessIndicator,dotParams);
        accessLabel=new TextView(this);accessLabel.setText("Not verified");verificationRow.addView(accessLabel,new LinearLayout.LayoutParams(0,-2,1));
        verificationRow.addView(pair,new LinearLayout.LayoutParams(0,-2,1));
        layout.addView(code);layout.addView(verificationRow);layout.addView(status);setupPanel=new LinearLayout(this);setupPanel.setOrientation(LinearLayout.VERTICAL);for(android.view.View support:new android.view.View[]{settings,start,manual,host,port,connection})setupPanel.addView(support);layout.addView(setupPanel);layout.addView(optionsPanel);
        Button revoke=new Button(this);revoke.setText("Disconnect installer access");revoke.setOnClickListener(v->{if(busy){show("Wait for installation to finish before disconnecting.");return;}new Thread(()->{try(java.net.Socket socket=helperSocket()){DataOutputStream out=new DataOutputStream(socket.getOutputStream());out.writeUTF("REVOKE_V1");out.flush();String response=new DataInputStream(socket.getInputStream()).readUTF();if(!response.equals("STOPPING"))throw new IOException("Unexpected helper response");getSharedPreferences("helper",0).edit().clear().commit();setAccess(false);show("Installer access disconnected. Enter a pairing code or reconnect to verify again.");}catch(Exception e){setAccess(false);show("Could not stop helper: "+e);}},"revoke").start();});optionsPanel.addView(revoke);

        nsd=(android.net.nsd.NsdManager)getSystemService(NSD_SERVICE);
        discovery=new android.net.nsd.NsdManager.DiscoveryListener(){
          public void onDiscoveryStarted(String t){} public void onDiscoveryStopped(String t){}
          public void onStartDiscoveryFailed(String t,int e){show("Automatic discovery unavailable. Use Advanced to enter the pairing port.");}
          public void onStopDiscoveryFailed(String t,int e){}
          public void onServiceLost(android.net.nsd.NsdServiceInfo i){runOnUiThread(()->{port.setText("");});}
          public void onServiceFound(android.net.nsd.NsdServiceInfo i){nsd.resolveService(i,new android.net.nsd.NsdManager.ResolveListener(){
            public void onResolveFailed(android.net.nsd.NsdServiceInfo i,int e){}
            public void onServiceResolved(android.net.nsd.NsdServiceInfo i){
              if(i.getHost()!=null && (i.getHost().isLoopbackAddress() || isLocalAddress(i.getHost())))runOnUiThread(()->{port.setText(Integer.toString(i.getPort()));host.setText("127.0.0.1");show("Pairing screen detected. Enter its six-digit code and tap Pair and enable installation.");});
            }
          });}
        };
        try{nsd.discoverServices("_adb-tls-pairing._tcp.",android.net.nsd.NsdManager.PROTOCOL_DNS_SD,discovery);}catch(Exception e){show("Discovery failed. Use Advanced for manual ports: "+e);}
        ScrollView scroll=new ScrollView(this);menuScroll=scroll;scroll.addView(layout);UiStyle.apply(this,layout,scroll);
        setAccess(false);show("Enter the six-digit wireless pairing code and tap Verify. If already paired, tap Reconnect access.");
        install.setOnClickListener(v -> {
            if (busy) return;
            if(!accessVerified||!gameChosen){show("Verify access and choose a game first.");return;}
            forceReinstall=forceReplace.isChecked();forceReplace.setEnabled(false);busy = true; install.setEnabled(false);success.setVisibility(android.view.View.GONE);progress.setVisibility(android.view.View.VISIBLE);progress.setIndeterminate(true);progressText.setText("Checking installer access…");
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            new Thread(this::performInstall, "obb-install").start();
        });
    }
    void verificationBusy(boolean checking){runOnUiThread(()->{if(isFinishing()||isDestroyed())return;if(checking)accessEpoch++;checkingAccess=checking;verifyButton.setEnabled(!checking);startHelper.setEnabled(!checking);pairingCode.setEnabled(!checking);});}
    void setAccess(boolean ready){runOnUiThread(()->{if(isFinishing()||isDestroyed())return;accessEpoch++;accessVerified=ready;
      android.graphics.drawable.GradientDrawable circle=new android.graphics.drawable.GradientDrawable();circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);circle.setColor(ready?0xff35c978:UiStyle.RED);accessIndicator.setBackground(circle);accessIndicator.setContentDescription(ready?"Access verified":"Access not verified");
      if(navGames!=null)navGames.setEnabled(ready);if(navInstall!=null)navInstall.setEnabled(ready);
      accessLabel.setText(ready?"Verified":"Not verified");accessLabel.setTextColor(ready?0xff35c978:UiStyle.WHITE);
      setupPanel.setVisibility(ready?android.view.View.GONE:android.view.View.VISIBLE);optionsPanel.setVisibility(ready?android.view.View.VISIBLE:android.view.View.GONE);installPanel.setVisibility(ready&&gameChosen?android.view.View.VISIBLE:android.view.View.GONE);
    });}
    @Override protected void onResume(){super.onResume();if(optionsPanel!=null&&!busy&&!checkingAccess){final int epoch=++accessEpoch;new Thread(()->{boolean ready=helperReady();runOnUiThread(()->{if(epoch==accessEpoch&&!checkingAccess&&!busy)setAccess(ready);});},"access-check").start();}}
    @Override protected void onSaveInstanceState(Bundle state){state.putString("game",selectedGame);state.putBoolean("chosen",gameChosen);super.onSaveInstanceState(state);}
    @Override protected void onPause(){
        if(savedHost!=null && menuScroll!=null)getSharedPreferences("menu",0).edit()
            .putString("host",savedHost.getText().toString().trim())
            .putBoolean("advanced",advancedVisible).putInt("scroll",menuScroll.getScrollY()).apply();
        super.onPause();
    }
    @Override public void onBackPressed(){
        // Keep the task available in Quest's app switcher instead of destroying setup.
        moveTaskToBack(true);
    }
    @Override protected void onNewIntent(android.content.Intent intent){super.onNewIntent(intent);String next=intent.getStringExtra("game");if(next!=null&&!next.equals(selectedGame)){if(busy){show("Finish installation before changing games.");return;}try{GameCatalog.asset(next);}catch(IllegalArgumentException invalid){return;}setIntent(intent);recreate();}else setIntent(intent);}
    java.net.Socket helperSocket() throws Exception {
      android.content.SharedPreferences prefs=getSharedPreferences("helper",0);int port=prefs.getInt("port",0);String secret=prefs.getString("secret","");if(port==0 || secret.isEmpty())throw new IOException("Tap Reconnect access first");
      java.net.Socket socket=new java.net.Socket();try{socket.connect(new java.net.InetSocketAddress("127.0.0.1",port),2000);socket.setSoTimeout(5000);LinkAuth.client(new DataInputStream(socket.getInputStream()),new DataOutputStream(socket.getOutputStream()),android.util.Base64.decode(secret,android.util.Base64.NO_WRAP));return socket;}catch(Exception e){socket.close();throw e;}
    }
    void retirePreviousHelper(){try(java.net.Socket socket=helperSocket()){DataOutputStream out=new DataOutputStream(socket.getOutputStream());out.writeUTF("REVOKE_V1");out.flush();if(new DataInputStream(socket.getInputStream()).readUTF().equals("STOPPING"))getSharedPreferences("helper",0).edit().clear().commit();}catch(Exception ignored){}}
    boolean helperReady(){try(java.net.Socket socket=helperSocket()){socket.setSoTimeout(2000);DataOutputStream out=new DataOutputStream(socket.getOutputStream());try(InputStream asset=getAssets().open(GameCatalog.asset(selectedGame))){String hash=new Payload(asset).manifestHash;out.writeUTF("PING_V2");out.writeUTF(hash);out.flush();return new DataInputStream(socket.getInputStream()).readUTF().equals("READY_V18:"+hash);}}catch(Exception e){return false;}}
    boolean isLocalAddress(java.net.InetAddress address){try{return java.net.NetworkInterface.getByInetAddress(address)!=null;}catch(Exception e){return false;}}
    @Override protected void onDestroy(){if(nsd!=null && discovery!=null)try{nsd.stopServiceDiscovery(discovery);}catch(Exception ignored){}super.onDestroy();}
    static int validPort(String value) { int p=Integer.parseInt(value);if(p<1 || p>65535)throw new IllegalArgumentException("Port must be 1–65535");return p; }
    void performInstall() {
        try {
            if (android.os.Process.myUid() / 100000 != 0)
                throw new IOException("Prototype supports the headset owner profile only");
            Payload payload;
            try (InputStream in = getAssets().open(GameCatalog.asset(selectedGame))) { payload = new Payload(in); }
            if(!payload.packageName.equals(GameCatalog.target(selectedGame)))throw new IOException("Selected game and download target differ");
            show("Connecting helper for " + payload.packageName + "…");
            try (java.net.Socket socket = helperSocket()) {

                socket.setSoTimeout(30 * 60 * 1000);

                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                DataInputStream in = new DataInputStream(socket.getInputStream());
                out.writeUTF(forceReinstall?"INSTALL_FORCE_V1":"INSTALL_V1"); out.writeUTF(payload.manifestHash); out.flush();
                while (true) {
                    String result = in.readUTF();
                    if (result.startsWith("NEED:")) {
                        String name = result.substring(5);
                        Payload.Entry entry = null;
                        for (Payload.Entry candidate : payload.files) if (candidate.name.equals(name)) entry = candidate;
                        if (entry == null) throw new IOException("Helper requested an unexpected file");
                        streamParts(entry, out, in);
                        continue;
                    }
                    if (result.startsWith("PLAN:")) {
                        String names = result.substring(5);
                        if (selectedGame.equals(GameCatalog.PAVLOV)||names.isEmpty()) { out.writeUTF("REPLACE"); out.flush(); continue; }
                        CountDownLatch choiceMade = new CountDownLatch(1);
                        AtomicBoolean backup = new AtomicBoolean(false);AtomicBoolean cancelled=new AtomicBoolean(false);
                        runOnUiThread(() -> new android.app.AlertDialog.Builder(this)
                            .setTitle("Replace older OBB?")
                            .setMessage((names.isEmpty() ? "No older matching OBB was found." : "Found on Quest:\n" + names)
                                + "\n\nKeep the old OBB in the Quest folder as a backup? It will use storage space until removed.")
                            .setPositiveButton("Back up old file", (dialog, which) -> { backup.set(true); choiceMade.countDown(); })
                            .setNegativeButton("Replace without backup", (dialog, which) -> { backup.set(false); choiceMade.countDown(); })
                            .setOnCancelListener(dialog -> { cancelled.set(true); choiceMade.countDown(); })
                            .show());
                        choiceMade.await();if(cancelled.get())throw new IOException("Installation cancelled. Existing OBB files were not replaced.");
                        out.writeUTF(backup.get() ? "BACKUP" : "REPLACE"); out.flush();
                        continue;
                    }
                    show(result);
                                        if(result.startsWith("DONE:")){runOnUiThread(()->{progress.setIndeterminate(false);progress.setProgress(1000);progressText.setText("All game OBBs installed and verified.");success.setVisibility(android.view.View.VISIBLE);});break;}
                    if(result.startsWith("ERROR:")){runOnUiThread(()->{progress.setVisibility(android.view.View.GONE);progressText.setText("Installation stopped. See message above.");});break;}
                }
            }
        } catch (Exception e) { if(!helperReady())setAccess(false);runOnUiThread(()->{progress.setVisibility(android.view.View.GONE);progressText.setText("Installation stopped.");}); show("Install unavailable: " + e + "\n\nIf this is a download error, your pairing remains valid. Check internet access and retry. Use Reconnect access only for a helper connection error."); }
        finally { runOnUiThread(() -> {
            busy = false; install.setEnabled(true);forceReplace.setEnabled(true);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }); }
    }
    void streamParts(Payload.Entry entry,DataOutputStream out,DataInputStream helper)throws Exception{
        out.writeLong(entry.size);out.flush();String ready=helper.readUTF();if(!ready.startsWith("Installing downloaded file:"))throw new IOException(ready);
        MessageDigest whole=MessageDigest.getInstance("SHA-256");java.util.zip.CRC32 fullCrc=new java.util.zip.CRC32();long total=0,lastUpdate=0;
        runOnUiThread(()->{progress.setIndeterminate(false);progress.setProgress(0);});
        for(int index=0;index<entry.parts.size();index++){
            Payload.Part part=entry.parts.get(index);show("Downloading "+entry.name+" — part "+(index+1)+" of "+entry.parts.size());
            HttpURLConnection connection=openReleaseAsset(part.url);
            try{
                if(connection.getResponseCode()!=200 || (connection.getContentLengthLong()>=0 && connection.getContentLengthLong()!=part.size) || (connection.getContentType()!=null && connection.getContentType().toLowerCase(java.util.Locale.ROOT).contains("text/html")))throw new IOException("GitHub did not return the expected release asset: "+part.name);
                MessageDigest chunkHash=MessageDigest.getInstance("SHA-256");java.util.zip.CRC32 chunkCrc=new java.util.zip.CRC32();long received=0;
                try(InputStream input=new BufferedInputStream(connection.getInputStream())){byte[] buffer=new byte[1048576];while(received<part.size){int n=input.read(buffer,0,(int)Math.min(buffer.length,part.size-received));if(n<0)throw new IOException("Incomplete GitHub part: "+part.name);out.write(buffer,0,n);whole.update(buffer,0,n);fullCrc.update(buffer,0,n);chunkHash.update(buffer,0,n);chunkCrc.update(buffer,0,n);received+=n;total+=n;long now=android.os.SystemClock.elapsedRealtime();if(now-lastUpdate>=250 || total==entry.size){lastUpdate=now;final long done=total;runOnUiThread(()->{progress.setProgress((int)(done*1000/entry.size));progressText.setText(entry.name+"\n"+(done*100/entry.size)+"% — "+(done/1048576)+" / "+(entry.size/1048576)+" MiB");});}}if(input.read()!=-1)throw new IOException("GitHub part larger than expected");}
                if(!Payload.hex(chunkHash.digest()).equals(part.sha256) || !String.format(java.util.Locale.ROOT,"%08x",chunkCrc.getValue()).equals(part.crc32))throw new IOException("Part integrity failed: "+part.name+". Existing OBB preserved.");
            }finally{connection.disconnect();}
        }
        String digest=Payload.hex(whole.digest());if(total!=entry.size || !digest.equals(entry.sha256) || !String.format(java.util.Locale.ROOT,"%08x",fullCrc.getValue()).equals(entry.crc32))throw new IOException("Reassembled OBB integrity failed. Existing OBB preserved.");out.writeUTF(digest);out.flush();runOnUiThread(()->{progress.setIndeterminate(true);progressText.setText("Download complete. Verifying reconstructed OBB…");});
    }
    HttpURLConnection openReleaseAsset(String address)throws Exception{
        java.net.URL url=new java.net.URL(address);
        for(int redirects=0;redirects<6;redirects++){
            String host=url.getHost();if(!url.getProtocol().equals("https") || !(host.equals("github.com") || host.equals("release-assets.githubusercontent.com") || host.equals("objects.githubusercontent.com")))throw new IOException("Untrusted GitHub download redirect");
            HttpURLConnection connection=(HttpURLConnection)url.openConnection();connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(30000);connection.setReadTimeout(120000);connection.setRequestProperty("User-Agent","PavLabz-OBB-Installer/0.9");int status=connection.getResponseCode();
            if(status==301 || status==302 || status==303 || status==307 || status==308){String location=connection.getHeaderField("Location");connection.disconnect();if(location==null)throw new IOException("Missing GitHub redirect");url=new java.net.URL(url,location);continue;}return connection;
        }throw new IOException("Too many GitHub redirects");
    }

}
