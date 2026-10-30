package dev.offline.obbinstaller;
import java.io.*;import java.nio.file.*;import java.security.*;import java.util.*;import java.util.zip.*;
public final class PavlovReplaceTest{
 static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);}
 static byte[] bytes(String s){return s.getBytes(java.nio.charset.StandardCharsets.UTF_8);}
 static Payload.Entry entry(String kind)throws Exception{
  Payload.Entry e=new Payload.Entry();e.name=kind+".2397.com.vankrupt.pavlov.obb";byte[] b=bytes("verified-"+kind);e.size=b.length;e.sha256=Payload.hex(MessageDigest.getInstance("SHA-256").digest(b));CRC32 crc=new CRC32();crc.update(b);e.crc32=String.format("%08x",crc.getValue());return e;
 }
 static void copy(File dir,Payload.Entry e,boolean force,boolean corrupt)throws Exception{
  byte[] b=bytes("verified-"+e.name.split("\\.")[0]);if(corrupt)b[0]^=1;
  ByteArrayOutputStream buffer=new ByteArrayOutputStream();DataOutputStream wire=new DataOutputStream(buffer);wire.writeLong(b.length);wire.write(b);wire.writeUTF(e.sha256);
  Helper.copy(dir,e,new DataOutputStream(new ByteArrayOutputStream()),true,new DataInputStream(new ByteArrayInputStream(buffer.toByteArray())),force);
 }
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory(Path.of(args[0]),"pavlov-replace-");
  for(boolean force:new boolean[]{false,true})for(String scenario:new String[]{"old","corrupt","matching","duplicates"}){
   Path dir=Files.createDirectory(root.resolve(scenario+"-"+force)),update=Files.createDirectory(dir.resolve("update"));
   List<Path> overflow=new ArrayList<>();
   for(String kind:new String[]{"overflow1","overflow2"})for(Path parent:new Path[]{dir,update}){
    Path f=parent.resolve(kind+".2397.com.vankrupt.pavlov.obb");Files.write(f,bytes(f.toString()));overflow.add(f);
   }
   for(String kind:new String[]{"main","patch"}){
    Payload.Entry e=entry(kind);
    Files.write(dir.resolve(e.name),scenario.equals("matching")?bytes("verified-"+kind):bytes("old-"+kind));
    Path old=dir.resolve(kind+".2396.com.vankrupt.pavlov.obb");Files.write(old,bytes("older"));
    Path older=dir.resolve(kind+".2395.com.vankrupt.pavlov.obb");if(scenario.equals("duplicates"))Files.write(older,bytes("even-older"));
    Path saved=dir.resolve(".backup-"+e.name);Files.write(saved,bytes("backup"));
    Files.write(update.resolve(e.name),bytes("misplaced-old"));
    if(scenario.equals("corrupt")){
     try{copy(dir.toFile(),e,true,true);throw new AssertionError("Corruption accepted");}catch(IOException expected){}
     check(Files.exists(old)&&Files.exists(saved),"Deleted old copies before verification");
    }
    copy(dir.toFile(),e,force,false);Payload.verifyStored(dir.resolve(e.name).toFile(),e);
    check(!Files.exists(older)&&!Files.exists(old)&&!Files.exists(saved)&&!Files.exists(update.resolve(e.name)),"Old main/patch copies remain");
   }
   for(Path f:overflow)check(Arrays.equals(Files.readAllBytes(f),bytes(f.toString())),"Overflow moved or modified: "+f);
   String[] names={entry("main").name,entry("patch").name};
   try{ObbPlacement.checkNames(dir.toFile(),PavlovReplacement.PACKAGE,names,true);throw new AssertionError("Extra entries accepted");}catch(IOException expected){}
   Path recovery=root.resolve("recovery-"+scenario+"-"+force);check(PavlovReplacement.recoverExtra(dir.toFile(),names,recovery.toFile())==3,"Expected overflow files and update directory recovered");
   for(Path f:overflow){Path batch;try(java.util.stream.Stream<Path> list=Files.list(recovery)){batch=list.findFirst().orElseThrow();}Path saved=batch.resolve(dir.relativize(f));check(Arrays.equals(Files.readAllBytes(saved),bytes(f.toString())),"Recovery changed content: "+f);}
   ObbPlacement.checkNames(dir.toFile(),PavlovReplacement.PACKAGE,names,true);try(java.util.stream.Stream<Path> list=Files.list(dir)){check(list.count()==2,"Unexpected entry in final folder");}
   check(Files.list(dir).noneMatch(f->f.getFileName().toString().startsWith(".obb-")),"Temporary or held old file remains");
  }
  Path interrupted=Files.createDirectory(root.resolve("interrupted"));Payload.Entry a=entry("main"),b=entry("patch");
  Files.write(interrupted.resolve("main.2396.com.vankrupt.pavlov.obb"),bytes("old-main"));Files.write(interrupted.resolve("patch.2396.com.vankrupt.pavlov.obb"),bytes("old-patch"));
  copy(interrupted.toFile(),a,true,false);try{copy(interrupted.toFile(),b,true,true);throw new AssertionError("Corrupt patch accepted");}catch(IOException expected){}
  check(Files.exists(interrupted.resolve("patch.2396.com.vankrupt.pavlov.obb")),"Interrupted patch removed old copy");
  copy(interrupted.toFile(),b,true,false);PavlovReplacement.checkNames(interrupted.toFile(),new String[]{a.name,b.name},true);
  Path newer=Files.createDirectory(root.resolve("newer"));Files.write(newer.resolve("main.2450.com.vankrupt.pavlov.obb"),bytes("newer"));
  try{PavlovReplacement.checkNames(newer.toFile(),new String[]{a.name,b.name},false);throw new AssertionError("Newer version accepted");}catch(IOException expected){}
  Path failed=Files.createDirectory(root.resolve("failed-recovery"));Files.write(failed.resolve("keep.txt"),bytes("keep"));Path blocked=root.resolve("not-a-directory");Files.write(blocked,bytes("blocked"));
  try{PavlovReplacement.recoverExtra(failed.toFile(),new String[]{a.name,b.name},blocked.toFile());throw new AssertionError("Bad recovery location accepted");}catch(IOException expected){}
  check(Files.exists(failed.resolve("keep.txt")),"Recovery failure deleted source");
  Path live=Files.createDirectory(root.resolve("duplex"));Payload.Entry expected=entry("main");
  java.util.concurrent.ExecutorService pool=java.util.concurrent.Executors.newSingleThreadExecutor();
  try(java.net.ServerSocket listener=new java.net.ServerSocket(0,1,java.net.InetAddress.getLoopbackAddress())){
   listener.setSoTimeout(5000);
   java.util.concurrent.Future<?> work=pool.submit(()->{try(java.net.Socket peer=listener.accept()){peer.setSoTimeout(5000);Helper.copy(live.toFile(),expected,new DataOutputStream(peer.getOutputStream()),true,new DataInputStream(peer.getInputStream()),true);}catch(Exception error){throw new RuntimeException(error);}});
   try(java.net.Socket client=new java.net.Socket(java.net.InetAddress.getLoopbackAddress(),listener.getLocalPort())){
    client.setSoTimeout(5000);DataInputStream in=new DataInputStream(client.getInputStream());DataOutputStream out=new DataOutputStream(client.getOutputStream());
    check(in.readUTF().equals("NEED:"+expected.name),"Protocol NEED");out.writeLong(expected.size);out.flush();
    check(in.readUTF().startsWith("Installing downloaded file:"),"Protocol readiness before bytes");
    out.write(bytes("verified-main"));out.writeUTF(expected.sha256);out.flush();check(in.readUTF().startsWith("Installed and verified:"),"Protocol final verification");
   }work.get(5,java.util.concurrent.TimeUnit.SECONDS);
  }finally{pool.shutdownNow();}
  Payload.verifyStored(live.resolve(expected.name).toFile(),expected);
  System.out.println("PASS: duplex transfer handshake waits for helper readiness before streaming; no deadlock.");
  System.out.println("PASS: 8 Pavlov replacement scenarios, corruption handling, verified recovery of extra files and directories, and exact final two-file layout.");
 }
}
