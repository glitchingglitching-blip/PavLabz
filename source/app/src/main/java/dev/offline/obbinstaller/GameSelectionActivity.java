package dev.offline.obbinstaller;
public final class GameSelectionActivity extends android.app.Activity{
 @Override public void onCreate(android.os.Bundle state){super.onCreate(state);startActivity(new android.content.Intent(this,MainActivity.class));finish();}
}
