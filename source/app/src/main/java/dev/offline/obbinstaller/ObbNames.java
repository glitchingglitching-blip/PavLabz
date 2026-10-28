package dev.offline.obbinstaller;
final class ObbNames {
 static boolean sameKind(String candidate,String target){
  String[] parts=target.split("\\.",3);
  return parts.length==3 && candidate.matches(java.util.regex.Pattern.quote(parts[0])+"\\.[0-9]+\\."+java.util.regex.Pattern.quote(parts[2]));
 }
}
