package dev.offline.obbinstaller;
final class AppVersion {
 static long code(String filename,String label){
  if(!filename.matches("PavLabz-Installer-v[0-9]{1,6}\\.[0-9]{1,6}(?:\\.[0-9]{1,6})?\\.apk"))return -1;
  java.util.regex.Matcher tagged=java.util.regex.Pattern.compile("pavlabz-versionCode=([0-9]{1,9});versionName=([0-9]+\\.[0-9]+(?:\\.[0-9]+)?)").matcher(label);
  if(tagged.matches())return Long.parseLong(tagged.group(1));
  java.util.regex.Matcher old=java.util.regex.Pattern.compile("PavLabz-Installer-v0\\.([0-9]{1,6})\\.apk").matcher(filename);
  return old.matches()?Long.parseLong(old.group(1)):-1;
 }
 static String name(String filename,String label){int split=label.indexOf(";versionName=");return split>=0?label.substring(split+13):filename.substring("PavLabz-Installer-v".length(),filename.length()-4);}
}
