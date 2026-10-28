package dev.offline.obbinstaller;
import android.content.*;
public final class DiagnosticsReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context,Intent intent){String value=context.getSharedPreferences("diagnostics",0).getString("status","No recorded status yet. Retry the operation once.");value=value.replaceAll("https?://\\S+","[URL]").replaceAll("[A-Za-z0-9+/]{40,}={0,2}","[redacted]");setResultCode(0);setResultData(value);}
}
