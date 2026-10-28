package dev.offline.obbinstaller;
import java.io.*;
final class ObbPlacement {
 static int repairNested(File dir,Payload payload,File recoveryRoot)throws Exception{
  if(payload.packageName.equals(PavlovReplacement.PACKAGE))return 0;
  File[] children=dir.listFiles();if(children==null)throw new IOException("Cannot inspect game OBB folder");
  int recovered=0;
  for(File child:children){
   if(!child.getCanonicalFile().equals(child.getAbsoluteFile()))throw new IOException("Redirected OBB entry: "+child.getName());
   if(!child.isDirectory())continue;
   for(Payload.Entry entry:payload.files){
    File misplaced=new File(child,entry.name);if(!misplaced.isFile())continue;
    if(!misplaced.getCanonicalFile().equals(misplaced.getAbsoluteFile()))throw new IOException("Redirected nested OBB");
    try{Payload.verifyStored(misplaced,entry);}catch(IOException unverified){continue;}
    File target=new File(dir,entry.name);
    if(target.exists()){
     if(!target.isFile()||!target.getCanonicalFile().equals(target.getAbsoluteFile()))continue;
     try{Payload.verifyStored(target,entry);}catch(IOException unverified){continue;}
     if(!misplaced.delete())throw new IOException("Cannot remove verified duplicate nested OBB");
    }else if(!misplaced.renameTo(target))throw new IOException("Cannot move verified OBB to game folder");
   }
   String[] remaining=child.list();if(remaining==null)throw new IOException("Cannot inspect nested OBB folder");
   if(remaining.length==0){if(!child.delete())throw new IOException("Cannot remove empty OBB subfolder");continue;}
   if(!recoveryRoot.getCanonicalFile().equals(recoveryRoot.getAbsoluteFile()))throw new IOException("Redirected recovery folder");
   if(!recoveryRoot.isDirectory()&&!recoveryRoot.mkdirs())throw new IOException("Cannot create recovery folder outside game OBBs");
   File recoveredDir=new File(recoveryRoot,java.util.UUID.randomUUID().toString()+"-"+child.getName());
   validateTree(child);
   if(!child.renameTo(recoveredDir)){copyTree(child,recoveredDir);removeVerifiedTree(child,recoveredDir);}
   recovered++;
  }return recovered;
 }
 static void validateTree(File source)throws IOException{
  if(!source.getCanonicalFile().equals(source.getAbsoluteFile()))throw new IOException("Redirected nested entry");
  if(source.isDirectory()){File[] children=source.listFiles();if(children==null)throw new IOException("Cannot read nested folder");for(File f:children)validateTree(f);}
  else if(!source.isFile())throw new IOException("Unsupported nested entry");
 }
 static void copyTree(File source,File target)throws Exception{
  if(source.isDirectory()){
   if(!target.mkdir())throw new IOException("Cannot create recovery subfolder");
   File[] children=source.listFiles();if(children==null)throw new IOException("Cannot read recovery source");
   for(File child:children)copyTree(child,new File(target,child.getName()));
  }else{
   if(!target.createNewFile())throw new IOException("Recovery entry already exists");
   try(InputStream in=new FileInputStream(source);FileOutputStream out=new FileOutputStream(target)){
    byte[] buffer=new byte[1048576];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);out.getFD().sync();
   }
   if(source.length()!=target.length()||!Payload.hash(source).equals(Payload.hash(target)))throw new IOException("Recovery copy verification failed; source retained");
  }
 }
 static void removeVerifiedTree(File source,File target)throws Exception{
  if(source.isDirectory()){
   File[] children=source.listFiles();if(children==null)throw new IOException("Cannot inspect recovered source");
   for(File child:children)removeVerifiedTree(child,new File(target,child.getName()));
  }else if(!target.isFile()||source.length()!=target.length()||!Payload.hash(source).equals(Payload.hash(target)))throw new IOException("Recovered file changed; source retained");
  if(!source.delete())throw new IOException("Cannot remove recovered source; verified copy remains outside game OBB folder");
 }
 static void checkNames(File dir,String pkg,String[] expected,boolean complete)throws IOException{
  if(pkg.equals(PavlovReplacement.PACKAGE)){PavlovReplacement.checkNames(dir,expected,complete);return;}
  File[] files=dir.listFiles();if(files==null)throw new IOException("Cannot inspect game OBB folder");
  for(File child:files)if(child.isDirectory())throw new IOException("Unexpected subfolder in game OBB folder: "+child.getName()+". Required OBBs must be directly in "+dir.getAbsolutePath()+". Move remaining folder contents outside the game OBB folder before retrying");
  for(String target:expected){String kind=target.substring(0,target.indexOf('.'));int count=0;
   for(File f:files){String name=f.getName();if(!name.startsWith(kind+".")||!name.contains(pkg))continue;
    if(!ObbNames.sameKind(name,target))throw new IOException("Incorrect OBB filename: "+name+". Expected "+target+" (no (1), extra extension or suffix). Move or rename this file before retrying");
    if(!f.isFile()||!f.getCanonicalFile().equals(f.getAbsoluteFile()))throw new IOException("Invalid OBB destination entry: "+name);
    count++;if(complete&&!name.equals(target))throw new IOException("Unexpected OBB version remains: "+name);
    long version=Long.parseLong(name.split("\\.",3)[1]);long wanted=Long.parseLong(target.split("\\.",3)[1]);
    if(version>wanted)throw new IOException("Newer OBB found: "+name+". Resolve game version mismatch before installing");
   }
   if(count>1)throw new IOException("Multiple "+kind+" OBB files found. Keep only the intended existing file in "+dir.getAbsolutePath()+" before retrying");
   if(complete&&count!=1)throw new IOException("Missing exact final OBB: "+target);
  }
 }
}
