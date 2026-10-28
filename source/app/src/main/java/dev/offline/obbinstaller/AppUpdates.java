package dev.offline.obbinstaller;

import android.app.Activity;
import android.widget.*;
import java.net.*;
import java.io.*;
import org.json.*;

/** Public release metadata only; never uses GitHub credentials. */
final class AppUpdates {
    static final String PREFIX="https://github.com/glitchingglitching-blip/PavLabz/releases/download/";
    static void attach(Activity activity, LinearLayout layout, java.util.function.BooleanSupplier busy) {
        Button button=new Button(activity); button.setText("Check for app updates"); layout.addView(button);
        TextView text=new TextView(activity);layout.addView(text);
        final String[] download={null};
        Runnable check=()->{
            button.setEnabled(false); text.setText("Checking for updates…");
            new Thread(()->{
                HttpURLConnection connection=null;
                try {
                    connection=(HttpURLConnection)new URL("https://api.github.com/repos/glitchingglitching-blip/PavLabz/releases/latest").openConnection();
                    connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(15000);connection.setReadTimeout(15000);
                    connection.setRequestProperty("Accept","application/vnd.github+json");connection.setRequestProperty("User-Agent","PavLabz-Installer");
                    if(connection.getResponseCode()!=200)throw new IOException("Release check unavailable");
                    ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                    try(InputStream in=connection.getInputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(bytes.size()+n>1048576)throw new IOException("Release response too large");bytes.write(buffer,0,n);}}
                    JSONObject release=new JSONObject(bytes.toString("UTF-8"));
                    if(release.optBoolean("draft")||release.optBoolean("prerelease"))throw new IOException("No stable release");
                    JSONArray assets=release.getJSONArray("assets");String found=null,version=null;
                    long current=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).getLongVersionCode();
                    for(int i=0;i<assets.length();i++){
                        JSONObject asset=assets.getJSONObject(i);String name=asset.getString("name");
                        String label=asset.optString("label","");long candidate=AppVersion.code(name,label);if(candidate<=current)continue;
                        String address=asset.getString("browser_download_url");URL url=new URL(address);
                        if(!address.startsWith(PREFIX)||url.getUserInfo()!=null||url.getPort()!=-1||!url.getPath().endsWith("/"+name)||!asset.optString("state").equals("uploaded"))throw new IOException("Invalid update link");
                        found=address;version=AppVersion.name(name,label);break;
                    }
                    final String target=found,label=version;
                    activity.runOnUiThread(()->{if(activity.isFinishing()||activity.isDestroyed())return;download[0]=target;button.setEnabled(true);button.setText(target==null?"Check for app updates":"Update available — v"+label);text.setText(target==null?"App is up to date.":"Tap to download the new APK. Open it from Downloads and approve installation to update.");});
                }catch(Exception e){activity.runOnUiThread(()->{if(activity.isFinishing()||activity.isDestroyed())return;button.setEnabled(true);text.setText("Could not check for updates. Check internet and try again.");});}
                finally{if(connection!=null)connection.disconnect();}
            },"app-update-check").start();
        };
        button.setOnClickListener(v->{if(busy.getAsBoolean()){text.setText("Finish the OBB installation before updating the app.");return;}if(download[0]==null){check.run();return;}try{activity.startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(download[0])));}catch(Exception e){text.setText("No browser available. Download the latest APK from the PavLabz GitHub releases page.");}});
        check.run();
    }
}
