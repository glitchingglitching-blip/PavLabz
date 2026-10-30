package dev.offline.obbinstaller;
import java.io.*;import java.nio.file.*;import java.util.*;
final class FolderAccess {
 static File gameRoot(int uid,String pkg)throws IOException{
  if(uid<10000||uid%100000<10000||(!pkg.equals(GameCatalog.target(GameCatalog.PAVLOV))&&!pkg.equals(GameCatalog.target(GameCatalog.CONTRACTORS))))throw new IOException("Invalid profile or game");
  return new File("/storage/emulated/"+(uid/100000)+"/Android/obb",pkg).getCanonicalFile();
 }
 static int removeEmptyFolders(File root)throws IOException{validateFolders(root,0,new int[]{0});return prune(root,0);}
 static void validateFolders(File dir,int depth,int[] count)throws IOException{
  if(depth>64||++count[0]>10000||!dir.getCanonicalFile().equals(dir.getAbsoluteFile()))throw new IOException("Unsafe folder tree");
  File[] entries=dir.listFiles();if(entries==null)throw new IOException("Folder access denied; reconnect access");
  for(File f:entries){if(!f.getCanonicalFile().equals(f.getAbsoluteFile()))throw new IOException("Redirected entry");if(f.isDirectory())validateFolders(f,depth+1,count);}
 }
 static int prune(File dir,int depth)throws IOException{
  if(depth>64||!dir.getCanonicalFile().equals(dir.getAbsoluteFile()))throw new IOException("Unsafe folder tree");
  File[] entries=dir.listFiles();if(entries==null)throw new IOException("Cannot inspect folder");int removed=0;
  for(File f:entries)if(f.isDirectory()){removed+=prune(f,depth+1);String[] left=f.list();if(left==null)throw new IOException("Cannot inspect folder");if(left.length==0){if(!f.delete())throw new IOException("Cannot delete empty folder: "+f.getName());removed++;}}
  return removed;
 }
 static File resolve(File root,String relative)throws IOException{
  if(!root.getCanonicalFile().equals(root.getAbsoluteFile()))throw new IOException("Redirected game folder");
  if(relative.length()>2048||relative.startsWith("/")||relative.contains("\\"))throw new IOException("Invalid path");
  File f=root;for(String part:relative.split("/")){if(part.isEmpty()){if(relative.isEmpty())break;throw new IOException("Invalid path");}if(part.equals(".")||part.equals(".."))throw new IOException("Invalid path");f=new File(f,part);if(!f.getCanonicalFile().equals(f.getAbsoluteFile()))throw new IOException("Redirected entry");}
  if(!f.toPath().normalize().startsWith(root.toPath().normalize()))throw new IOException("Outside game folder");return f;
 }
 static boolean protectedFile(File f){String n=f.getName().toLowerCase(Locale.ROOT);return n.startsWith("overflow1.")||n.startsWith("overflow2.");}
 static void handle(File root,DataInputStream in,DataOutputStream out)throws Exception{
  String action=in.readUTF(),path=in.readUTF();File f=resolve(root,path);
  if(action.equals("LIST")){
   if(!f.exists()&&path.isEmpty()){out.writeUTF("LIST");out.writeInt(0);out.flush();return;}
   File[] files=f.listFiles();if(files==null||files.length>4096)throw new IOException("Cannot list this folder");Arrays.sort(files,Comparator.comparing(File::getName));out.writeUTF("LIST");out.writeInt(files.length);
   for(File child:files){out.writeUTF(child.getName());out.writeBoolean(child.isDirectory());out.writeLong(child.length());}out.flush();return;
  }
  if(path.isEmpty()||protectedFile(f))throw new IOException("Overflow files and game folder are protected");
  if(action.equals("DELETE")){if(!f.exists()||(!f.isFile()&&!f.isDirectory()))throw new IOException("Entry missing");if(f.isDirectory()){String[] children=f.list();if(children==null||children.length!=0)throw new IOException("Only empty folders can be deleted");}if(!f.delete())throw new IOException("Could not delete entry");}
  else if(action.equals("MOVE_ROOT")){if(!f.isFile())throw new IOException("Only files can be moved");File target=resolve(root,f.getName());if(target.equals(f)||target.exists())throw new IOException("A file with that name already exists in the game folder");Files.move(f.toPath(),target.toPath());}
  else throw new IOException("Unknown folder action");out.writeUTF("OK");out.flush();
 }
}
