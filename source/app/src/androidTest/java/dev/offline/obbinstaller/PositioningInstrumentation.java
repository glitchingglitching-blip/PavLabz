package dev.offline.obbinstaller;
import android.app.*;import android.os.*;import android.content.*;import android.view.*;import java.io.*;
public class PositioningInstrumentation extends Instrumentation{
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 void check(boolean b,String text){if(!b)throw new AssertionError(text);}
 void capture(String name)throws Exception{
  android.graphics.Bitmap bitmap=getUiAutomation().takeScreenshot();if(bitmap==null)throw new IOException("No screenshot");
  try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"pavlabz-"+name+".png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
  bitmap.recycle();
 }
 void verifyAtomicTransfer()throws Exception{
  File root=new File(getTargetContext().getExternalFilesDir(null),"airsoft-transfer-test-"+java.util.UUID.randomUUID());if(!root.mkdir())throw new IOException("Cannot create test folder");
  Payload.Entry entry=new Payload.Entry();entry.name="maps_scenes_assets_scenes_test.unity_a.bundle";byte[] old="old".getBytes("UTF-8"),data="new verified bytes".getBytes("UTF-8");entry.size=data.length;entry.sha256=Payload.hex(java.security.MessageDigest.getInstance("SHA-256").digest(data));java.util.zip.CRC32 crc=new java.util.zip.CRC32();crc.update(data);entry.crc32=String.format(java.util.Locale.ROOT,"%08x",crc.getValue());File target=new File(root,entry.name);java.nio.file.Files.write(target.toPath(),old);
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream wire=new DataOutputStream(bytes);wire.writeLong(data.length);wire.write(data);wire.writeUTF(entry.sha256);Airsoft491.copy(root,entry,new DataOutputStream(new ByteArrayOutputStream()),new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())),true);Payload.verifyStored(target,entry);check(target.delete(),"Cannot clean test file");check(root.delete(),"Cannot clean test folder");
 }
 @Override public void onStart(){
  Bundle result=new Bundle();
  try{
   MainActivity a=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
   waitForIdleSync();
   runOnMainSync(()->{
    a.setAccess(false);check(!a.accessVerified,"Initial verification state");check(a.optionsPanel.getVisibility()==View.GONE,"Options exposed before verification");
    check(!a.navInstall.isEnabled()&&!a.navGames.isEnabled(),"Navigation gate");
    check(a.verifyButton.getWidth()>0&&a.accessIndicator.getWidth()>0,"Verify row has no width");
    check(a.verifyButton.getRight()<=((View)a.verifyButton.getParent()).getWidth(),"Verify button overflow");
    check(a.accessIndicator.getRight()<=((View)a.verifyButton.getParent()).getWidth(),"Indicator overflow");
   });
   waitForIdleSync();capture("red");
   runOnMainSync(()->{
    // Only exercise presentation state. This does not simulate successful pairing/authentication.
    a.setAccess(true);check(a.optionsPanel.getVisibility()==View.VISIBLE,"Options not revealed");check(a.accessLabel.getText().toString().equals("Verified"),"Label");
    ((android.widget.Button)a.gameChoices.getChildAt(3)).performClick();check(a.gameChosen&&a.installPanel.getVisibility()==View.VISIBLE,"Inline game selection");check(GameCatalog.AIRSOFT.equals(a.selectedGame),"Airsoft selection");
   });
   waitForIdleSync();capture("green");
   runOnMainSync(()->{a.setAccess(false);check(a.optionsPanel.getVisibility()==View.GONE,"Options remain after disconnect");});
   verifyAtomicTransfer();
   result.putString("result","PASS: initial red gate, navigation gate, verify row bounds, green presentation, Airsoft selection, atomic file replacement, and disconnect hide.");
   finish(Activity.RESULT_OK,result);
  }catch(Throwable t){result.putString("failure",t.toString());finish(Activity.RESULT_CANCELED,result);}
 }
}
