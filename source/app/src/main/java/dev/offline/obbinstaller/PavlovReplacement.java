package dev.offline.obbinstaller;
import java.io.*;import java.nio.file.*;import java.security.*;import java.util.*;import java.util.zip.*;
final class PavlovReplacement{
 static final String PACKAGE="com.vankrupt.pavlov";
 static void checkNames(File dir,String[] expected,boolean complete)throws IOException{
  File[] files=dir.listFiles();if(files==null)throw new IOException("Cannot inspect Pavlov OBB folder");
  for(File f:files)if(f.isDirectory())throw new IOException("Subfolder found: "+f.getName()+". Use View folder to inspect it before installing.");
  for(String target:expected){int count=0;String kind=target.substring(0,target.indexOf('.'));long version=Long.parseLong(target.split("\\.",3)[1]);
   for(File f:files){
    String name=f.getName();if(!name.startsWith(kind+".")||!name.contains(PACKAGE))continue;
    if(!ObbNames.sameKind(name,target)||!f.isFile()||!f.getCanonicalFile().equals(f.getAbsoluteFile()))throw new IOException("Invalid Pavlov OBB entry: "+name);
    if(Long.parseLong(name.split("\\.",3)[1])>version)throw new IOException("Newer Pavlov OBB found: "+name);
    if(complete&&!name.equals(target))throw new IOException("Old Pavlov OBB remains: "+name);
    if(name.equals(target))count++;
   }if(complete&&count!=1)throw new IOException("Missing final Pavlov OBB: "+target);
  }
 }
 static void cleanOld(File dir,Payload.Entry e)throws Exception{
  File[] files=dir.listFiles();if(files==null)throw new IOException("Cannot inspect old Pavlov OBBs");
  for(File f:files){
   if(f.isDirectory()){
    if(f.getName().equalsIgnoreCase("update")&&f.getCanonicalFile().equals(f.getAbsoluteFile())){
     File[] nested=f.listFiles();if(nested==null)throw new IOException("Cannot inspect update folder");
     for(File n:nested)deleteOld(n,e,false);
     String[] left=f.list();if(left!=null&&left.length==0&&!f.delete())throw new IOException("Cannot remove empty update folder");
    }
   }else deleteOld(f,e,true);
  }
 }
 static void deleteOld(File f,Payload.Entry e,boolean root)throws Exception{
  String n=f.getName();boolean backup=n.startsWith(".backup-");String candidate=backup?n.substring(8):n;
  if(!ObbNames.sameKind(candidate,e.name))return;
  if(root&&!backup&&candidate.equals(e.name))return;
  if(Long.parseLong(candidate.split("\\.",3)[1])>Long.parseLong(e.name.split("\\.",3)[1]))return;
  if(!f.isFile()||!f.getCanonicalFile().equals(f.getAbsoluteFile()))throw new IOException("Invalid old Pavlov file: "+n);
  if(!f.delete())throw new IOException("New OBB verified, but old copy could not be deleted: "+n);
 }
 static void copy(File dir,Payload.Entry e,DataOutputStream out,DataInputStream network,boolean force)throws Exception{
  File target=new File(dir,e.name);
  if(!target.getCanonicalFile().equals(target.getAbsoluteFile())||(target.exists()&&!target.isFile()))throw new IOException("Invalid Pavlov target");
  if(!force&&target.isFile()&&target.length()==e.size&&Payload.hash(target).equals(e.sha256)){
   Payload.verifyStored(target,e);cleanOld(dir,e);Helper.say(out,"Already installed and verified: "+e.name);return;
  }
  Helper.say(out,"NEED:"+e.name);long size=network.readLong();if(size!=e.size||size<=0)throw new IOException("Invalid Pavlov download size");
  if(dir.getUsableSpace()<size+64L*1024*1024)throw new IOException("Not enough storage for verified Pavlov replacement");
  File temp=new File(dir,".obb-install-"+UUID.randomUUID()+".part");
  if(!temp.createNewFile())throw new IOException("Cannot create staging file");
  try{
   Helper.say(out,"Installing downloaded file: "+e.name);
   MessageDigest hash=MessageDigest.getInstance("SHA-256");CRC32 crc=new CRC32();long total=0;
   try(FileOutputStream sink=new FileOutputStream(temp)){
    byte[] buffer=new byte[1048576];while(total<size){int n=network.read(buffer,0,(int)Math.min(buffer.length,size-total));if(n<0)throw new IOException("Download ended early");sink.write(buffer,0,n);hash.update(buffer,0,n);crc.update(buffer,0,n);total+=n;}sink.getFD().sync();
   }
   String appHash=network.readUTF();String digest=Payload.hex(hash.digest());
   if(!digest.equals(e.sha256)||!digest.equals(appHash)||!String.format(Locale.ROOT,"%08x",crc.getValue()).equals(e.crc32))throw new IOException("Pavlov download integrity mismatch; old OBB retained");
   Payload.verifyStored(temp,e);
   // Same-directory atomic replacement removes the old destination without moving it to backup/recovery.
   Files.move(temp.toPath(),target.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
   Payload.verifyStored(target,e);cleanOld(dir,e);
   Helper.say(out,"Installed and verified: "+e.name+". Old main/patch copies deleted; overflow files unchanged.");
  }finally{if(temp.exists()&&!temp.delete())System.err.println("Left staging file: "+temp);}
 }
}
