package dev.offline.obbinstaller;
import android.app.Activity;import android.graphics.Color;import android.graphics.Typeface;import android.graphics.drawable.*;import android.content.res.ColorStateList;import android.view.*;import android.widget.*;
final class UiStyle {
 static final int RED=Color.rgb(194,10,36), WHITE=Color.rgb(235,232,233);
 static android.graphics.drawable.GradientDrawable panel(int a,int b){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{a,b});g.setStroke(1,Color.rgb(105,16,29));g.setCornerRadius(2);return g;}
 static TextView text(Activity a,String value,int size,Typeface face){TextView t=new TextView(a);t.setText(value);t.setTextSize(size);t.setTextColor(WHITE);t.setTypeface(face);t.setPadding(14,12,14,12);return t;}
 static void button(Button b,Typeface font,boolean active){b.setTypeface(font);b.setTextSize(20);b.setTextColor(WHITE);b.setAllCaps(true);b.setMinHeight(60);b.setPadding(18,12,18,12);android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();states.addState(new int[]{android.R.attr.state_pressed},panel(0xffdd1333,0xff4d0711));states.addState(new int[]{-android.R.attr.state_enabled},panel(0xff272027,0xff111116));states.addState(new int[]{},panel(active?0xffa9001c:0xff32101a,0xff0c0c10));b.setBackground(states);LinearLayout.LayoutParams lp=b.getLayoutParams() instanceof LinearLayout.LayoutParams ? (LinearLayout.LayoutParams)b.getLayoutParams() : new LinearLayout.LayoutParams(-1,-2);lp.setMargins(lp.leftMargin,6,lp.rightMargin,6);b.setLayoutParams(lp);}
 static void styleTree(MainActivity a,android.view.ViewGroup parent,Typeface font){
  for(int i=0;i<parent.getChildCount();i++){View v=parent.getChildAt(i);
   if(v instanceof CheckBox){CheckBox box=(CheckBox)v;box.setTypeface(font);box.setTextColor(WHITE);box.setButtonTintList(ColorStateList.valueOf(RED));box.setTextSize(18);}
   else if(v instanceof Button)button((Button)v,font,v==a.install||v==a.verifyButton);
   else if(v instanceof EditText){EditText e=(EditText)v;e.setTypeface(font);e.setTextColor(WHITE);e.setHintTextColor(0xffaaa3a9);e.setTextSize(19);e.setBackgroundTintList(ColorStateList.valueOf(RED));e.setPadding(14,16,14,16);}
   else if(v instanceof TextView){TextView t=(TextView)v;t.setTypeface(font);t.setTextColor(0xffc5bfc4);t.setTextSize(17);}
   else if(v instanceof android.view.ViewGroup)styleTree(a,(android.view.ViewGroup)v,font);
  }
 }
 static void scrollTo(LinearLayout content,ScrollView scroll,View target){
  android.graphics.Rect rect=new android.graphics.Rect();target.getDrawingRect(rect);content.offsetDescendantRectToMyCoords(target,rect);scroll.smoothScrollTo(0,Math.max(0,rect.top-24));
 }
 static void apply(MainActivity a,LinearLayout content,ScrollView scroll){
  Typeface font=Typeface.createFromAsset(a.getAssets(),"Rajdhani-Medium.ttf");content.setPadding(24,12,24,24);styleTree(a,content,font);
  content.addView(text(a,"OFFLINE OBB INSTALLER",32,font),0);TextView sub=text(a,"QUEST EDITION / VERIFY ACCESS",16,font);sub.setTextColor(0xffdf5265);content.addView(sub,1);
  a.status.setBackground(panel(0xff20151c,0xff101116));a.status.setPadding(18,18,18,18);a.progress.setProgressTintList(ColorStateList.valueOf(RED));a.progress.setIndeterminateTintList(ColorStateList.valueOf(RED));a.progressText.setTextSize(20);
  boolean compact=a.getResources().getConfiguration().screenWidthDp<480;
  LinearLayout root=new LinearLayout(a);root.setOrientation(compact?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff19151d,0xff07090e}));
  LinearLayout nav=new LinearLayout(a);nav.setOrientation(compact?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);nav.setPadding(12,20,12,20);nav.setBackground(panel(0xff0a0b10,0xff171017));
  TextView navTitle=text(a,"PAVLABZ",22,font);navTitle.setVisibility(compact?View.GONE:View.VISIBLE);nav.addView(navTitle);
  Button setup=new Button(a);setup.setText("SETUP");button(setup,font,false);setup.setOnClickListener(v->{a.setupPanel.setVisibility(View.VISIBLE);scroll.smoothScrollTo(0,0);});nav.addView(setup);
  Button downloads=new Button(a);a.navInstall=downloads;downloads.setText("INSTALL");button(downloads,font,true);downloads.setEnabled(false);downloads.setOnClickListener(v->{if(a.accessVerified&&a.gameChosen)scrollTo(content,scroll,a.installPanel);else a.show("Verify access and choose a game first.");});nav.addView(downloads);
  Button games=new Button(a);a.navGames=games;games.setText("GAMES");button(games,font,false);games.setEnabled(false);games.setOnClickListener(v->{if(a.busy){a.show("Finish installation before changing games.");return;}if(a.accessVerified)scrollTo(content,scroll,a.gameChoices);});nav.addView(games);
  Button exit=new Button(a);exit.setText("CLOSE");button(exit,font,false);exit.setOnClickListener(v->{if(!a.busy)a.finish();else a.show("Installation is running. Wait for verification to finish.");});nav.addView(exit);
  if(compact){for(int i=1;i<nav.getChildCount();i++){View child=nav.getChildAt(i);child.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));if(child instanceof TextView)((TextView)child).setTextSize(14);}root.addView(nav,new LinearLayout.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));}
  else{root.addView(nav,new LinearLayout.LayoutParams((int)(140*a.getResources().getDisplayMetrics().density),-1));root.addView(scroll,new LinearLayout.LayoutParams(0,-1,1));}
  final boolean[] narrow={compact};
  root.addOnLayoutChangeListener((view,left,top,right,bottom,oldLeft,oldTop,oldRight,oldBottom)->{
   if(right<=left)return;boolean next=(right-left)/a.getResources().getDisplayMetrics().density<480;if(next==narrow[0])return;narrow[0]=next;
   root.setOrientation(next?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);nav.setOrientation(next?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);navTitle.setVisibility(next?View.GONE:View.VISIBLE);
   for(int i=1;i<nav.getChildCount();i++){View child=nav.getChildAt(i);child.setLayoutParams(next?new LinearLayout.LayoutParams(0,-2,1):new LinearLayout.LayoutParams(-1,-2));if(child instanceof TextView)((TextView)child).setTextSize(next?14:20);}
   nav.setLayoutParams(next?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams((int)(140*a.getResources().getDisplayMetrics().density),-1));
   scroll.setLayoutParams(next?new LinearLayout.LayoutParams(-1,0,1):new LinearLayout.LayoutParams(0,-1,1));
  });
  root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});scroll.setFillViewport(true);a.setContentView(root);root.requestApplyInsets();
 }
}
