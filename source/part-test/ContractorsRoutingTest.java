package dev.offline.obbinstaller;
import java.io.*;import java.nio.file.*;import java.util.*;import java.util.zip.*;
public final class ContractorsRoutingTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static Payload parse(String source)throws Exception{return new Payload(new ByteArrayInputStream(source.getBytes("UTF-8")));}
 static void rejected(String source)throws Exception{try{parse(source);throw new AssertionError("Invalid manifest accepted");}catch(IOException expected){}}
 public static void main(String[] args)throws Exception{
  Path assets=Path.of(args[0]), temp=Files.createTempDirectory(Path.of(args[1]),"contractors-route-");
  String contractors=Files.readString(assets.resolve(GameCatalog.asset(GameCatalog.CONTRACTORS))),pavlov=Files.readString(assets.resolve(GameCatalog.asset(GameCatalog.PAVLOV)));
  for(String game:new String[]{GameCatalog.CONTRACTORS,GameCatalog.PAVLOV}){
   Payload p=parse(game.equals(GameCatalog.CONTRACTORS)?contractors:pavlov);check(p.packageName.equals(GameCatalog.target(game)),"Target mismatch");
   Path dir=Files.createDirectory(temp.resolve(p.packageName));String[] names=p.files.stream().map(e->e.name).toArray(String[]::new);
   for(Payload.Entry e:p.files){check(e.name.endsWith("."+p.packageName+".obb"),"Wrong package filename");check(e.size>0,"Invalid size");Files.write(dir.resolve(e.name),new byte[0]);long total=0;for(Payload.Part part:e.parts)total+=part.size;check(total==e.size,"Part total");}
   ObbPlacement.checkNames(dir.toFile(),p.packageName,names,true);
   Path bad=dir.resolve(names[0]+"(1)");Files.write(bad,new byte[0]);try{ObbPlacement.checkNames(dir.toFile(),p.packageName,names,true);throw new AssertionError("Bad suffix accepted");}catch(IOException expected){}Files.delete(bad);
  }
  rejected(contractors.replace("com.CaveManStudio.ContractorsVR","com.untrusted.app"));rejected(contractors.replace("https://github.com/","https://example.com/"));rejected(contractors.replace(".obb\"",".obb(1)\""));
  try(ZipFile zip=new ZipFile(args[2])){Map<String,Payload> map=GameCatalog.payloads(zip);check(map.size()==2,"Missing game");check(map.get(parse(contractors).manifestHash).packageName.equals(GameCatalog.target(GameCatalog.CONTRACTORS)),"Contractors helper route");check(map.get(parse(pavlov).manifestHash).packageName.equals(GameCatalog.target(GameCatalog.PAVLOV)),"Pavlov helper route");check(!map.containsKey("arbitrary client hash"),"Unknown hash accepted");}
  check(AppVersion.code("PavLabz-Installer-v0.14.apk","pavlabz-versionCode=15;versionName=0.14")==15,"Update version");
  System.out.println("PASS: both real manifests and APK helper routes, Contractors exact folder/names, 64-bit sizes, part totals, bad suffix/host/package rejection, unknown hash rejection and update metadata.");
 }
}
