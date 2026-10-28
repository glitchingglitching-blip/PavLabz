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
    ((android.widget.Button)a.gameChoices.getChildAt(1)).performClick();check(a.gameChosen&&a.installPanel.getVisibility()==View.VISIBLE,"Inline game selection");
   });
   waitForIdleSync();capture("green");
   runOnMainSync(()->{a.setAccess(false);check(a.optionsPanel.getVisibility()==View.GONE,"Options remain after disconnect");});
   result.putString("result","PASS: initial red gate, navigation gate, verify row bounds, green presentation, inline game options and disconnect hide.");
   finish(Activity.RESULT_OK,result);
  }catch(Throwable t){result.putString("failure",t.toString());finish(Activity.RESULT_CANCELED,result);}
 }
}
