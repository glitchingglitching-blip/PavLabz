package dev.offline.obbinstaller;
import java.io.*;import java.nio.file.*;import java.util.*;import java.security.*;import java.util.zip.*;
public class FlatPlacementTest{
 static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);}
 static Payload load(Path assets,String game)throws Exception{return new Payload(Files.newInputStream(assets.resolve(GameCatalog.asset(game))));}
 static byte[] bytes(Payload.Entry e){return ("data:"+e.name).getBytes(java.nio.charset.StandardCharsets.UTF_8);}
 static void fixture(Payload p)throws Exception{for(Payload.Entry e:p.files){byte[] b=bytes(e);e.size=b.length;e.sha256=Payload.hex(MessageDigest.getInstance("SHA-256").digest(b));CRC32 crc=new CRC32();crc.update(b);e.crc32=String.format("%08x",crc.getValue());}}
 static void copy(File dir,Payload.Entry e,boolean force)throws Exception{
  byte[] b=bytes(e);ByteArrayOutputStream data=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(data);out.writeLong(b.length);out.write(b);out.writeUTF(e.sha256);
  Helper.copy(dir,e,new DataOutputStream(new ByteArrayOutputStream()),false,new DataInputStream(new ByteArrayInputStream(data.toByteArray())),force);
 }
 static void rejected(String data)throws Exception{try{new Payload(new ByteArrayInputStream(data.getBytes("UTF-8")));throw new AssertionError("Bad manifest accepted");}catch(IOException expected){}}
 public static void main(String[] args)throws Exception{
  Path assets=Path.of(args[0]),root=Files.createTempDirectory(Path.of(args[1]),"flat-obb-");
  for(String game:new String[]{"contractors"}){
   Payload real=load(assets,game);check(real.files.size()==2,"Required file count");
   String raw=Files.readString(assets.resolve(GameCatalog.asset(game)));
   rejected(raw.replace("main.","update/main."));rejected(raw.replace("main.","../main."));
   rejected(raw.replace("patch.","overflow1."));rejected(raw.replace("patch.2397.","patch.2398.").replace("patch.181.","patch.182."));
   for(boolean force:new boolean[]{false,true})for(String scenario:new String[]{"valid","incomplete","conflicting","duplicate","old","unknown"}){
    Payload p=load(assets,game);fixture(p);Path dir=Files.createDirectories(root.resolve(game+"-"+scenario+"-"+force)),nested=Files.createDirectory(dir.resolve("update")),recovery=root.resolve("recovery-"+game+"-"+scenario+"-"+force);
    Payload.Entry e=p.files.get(p.files.size()-1);
    Files.write(nested.resolve(e.name),scenario.equals("incomplete")?new byte[]{0}:bytes(e));
    if(scenario.equals("conflicting"))Files.write(dir.resolve(e.name),new byte[]{9});
    if(scenario.equals("duplicate"))Files.write(dir.resolve(e.name),bytes(e));
    if(scenario.equals("old"))Files.write(nested.resolve(e.name.replace(".2397.",".2396.").replace(".181.",".180.")),new byte[]{1});
    if(scenario.equals("unknown"))Files.write(nested.resolve("keep-me.txt"),new byte[]{2});
    String[] names=p.files.stream().map(x->x.name).toArray(String[]::new);
    try{ObbPlacement.checkNames(dir.toFile(),p.packageName,names,false);throw new AssertionError("Nested folder ignored");}catch(IOException expected){}
    ObbPlacement.repairNested(dir.toFile(),p,recovery.toFile());
    check(!Files.exists(nested),"Nested folder remains");
    for(Payload.Entry file:p.files)copy(dir.toFile(),file,force);
    ObbPlacement.checkNames(dir.toFile(),p.packageName,names,true);
    for(Payload.Entry file:p.files)Payload.verifyStored(dir.resolve(file.name).toFile(),file);
    check(Files.list(dir).count()==p.files.size(),"Extra game folder contents");
    if(Arrays.asList("incomplete","conflicting","old","unknown").contains(scenario))check(Files.isDirectory(recovery),"Recovery missing");
    if(scenario.equals("conflicting"))check(Files.walk(recovery).anyMatch(f->f.getFileName().toString().equals(e.name)),"Conflicting nested copy not preserved");
   }
  }
  System.out.println("PASS: both manifests, flat paths, traversal/missing-kind/mixed-version rejection, 12 normal/force migration + production copy cases, incomplete/conflicting/duplicate/older/unknown contents preserved, exact final size/SHA/CRC.");
 }
}
