package dev.offline.obbinstaller;
import java.io.*;import java.util.*;import android.app.*;
final class FolderBrowser {
 static String parent(String path){int n=path.lastIndexOf('/');return n<0?"":path.substring(0,n);}
 static void open(MainActivity a,String game,String path){
  if(a.busy){a.show("Finish the current operation first.");return;}
  String key="allowed-"+game;
  if(!a.getSharedPreferences("folder-consent",0).getBoolean(key,false)){
   new AlertDialog.Builder(a).setTitle("Allow OBB folder access?").setMessage("Allow this installer to view "+GameCatalog.title(game)+" OBB files and perform the move/delete actions you select? Overflow files are protected. Access uses the wireless debugging connection; a normal storage permission cannot unlock Android/obb.").setPositiveButton("Allow",(d,w)->{a.getSharedPreferences("folder-consent",0).edit().putBoolean(key,true).apply();open(a,game,path);}).setNegativeButton("Cancel",null).show();return;
  }
  if(!a.accessVerified){new AlertDialog.Builder(a).setTitle("Connect folder access").setMessage("Enter the headset wireless-debugging pairing code and tap Verify. If already paired, tap Reconnect access. The folder opens automatically after access is verified.").setPositiveButton("Set up access",(d,w)->{a.pendingFolderGame=game;a.pendingFolderPath=path;a.setupPanel.setVisibility(android.view.View.VISIBLE);a.pairingCode.requestFocus();}).setNegativeButton("Cancel",null).show();return;}
  request(a,game,path,"LIST");
 }
 static void request(MainActivity a,String game,String path,String action){
  if(a.busy||!a.accessVerified){a.show("Verify access first and finish any current transfer.");return;}a.busy=true;
  new Thread(()->{try(java.net.Socket socket=a.helperSocket()){
   socket.setSoTimeout(15000);DataOutputStream out=new DataOutputStream(socket.getOutputStream());DataInputStream in=new DataInputStream(socket.getInputStream());String hash;try(InputStream asset=a.getAssets().open(GameCatalog.asset(game))){hash=new Payload(asset).manifestHash;}
   out.writeUTF("FOLDER_V1");out.writeUTF(hash);out.writeUTF(action);out.writeUTF(path);out.flush();String reply=in.readUTF();
   if(!action.equals("LIST")){if(!reply.equals("OK"))throw new IOException(reply);a.runOnUiThread(()->{a.busy=false;open(a,game,parent(path));});return;}
   if(!reply.equals("LIST"))throw new IOException(reply);int count=in.readInt();if(count<0||count>4096)throw new IOException("Invalid listing");String[] names=new String[count],labels=new String[count];boolean[] dirs=new boolean[count];
   for(int i=0;i<count;i++){names[i]=in.readUTF();dirs[i]=in.readBoolean();long bytes=in.readLong();labels[i]=(dirs[i]?"[Folder] ":"")+names[i]+(dirs[i]?"":" ("+(bytes/1048576)+" MiB)");}
   a.runOnUiThread(()->{a.busy=false;String location="/sdcard/Android/obb/"+GameCatalog.target(game)+(path.isEmpty()?"":"/"+path);AlertDialog.Builder dialog=new AlertDialog.Builder(a).setTitle(location).setItems(labels,(d,index)->{String child=path.isEmpty()?names[index]:path+"/"+names[index];if(dirs[index]){new AlertDialog.Builder(a).setTitle(names[index]).setItems(new String[]{"Open folder","Delete empty folder"},(x,k)->{if(k==0)open(a,game,child);else confirm(a,game,child);}).show();}else if(FolderAccess.protectedFile(new File(names[index]))){a.show("Overflow file protected: "+names[index]);open(a,game,path);}else{String[] choices=path.isEmpty()?new String[]{"Delete file"}:new String[]{"Move into game folder","Delete file"};new AlertDialog.Builder(a).setTitle(names[index]).setItems(choices,(x,k)->{if(!path.isEmpty()&&k==0)request(a,game,child,"MOVE_ROOT");else confirm(a,game,child);}).show();}}).setPositiveButton("Refresh",(d,w)->open(a,game,path)).setNegativeButton("Close",null);if(!path.isEmpty())dialog.setNeutralButton("Up",(d,w)->open(a,game,parent(path)));dialog.show();});
  }catch(Exception e){a.runOnUiThread(()->{a.busy=false;new AlertDialog.Builder(a).setTitle("Folder action stopped").setMessage(e.toString()+"\nIf the helper was started before this update, tap Reconnect access.").setPositiveButton("OK",null).show();});}},"folder-browser").start();
 }
 static void confirm(MainActivity a,String game,String path){new AlertDialog.Builder(a).setTitle("Delete this entry?").setMessage(path+"\nThis cannot be undone. Only empty folders can be deleted. Deleting an OBB can stop the game from loading.").setPositiveButton("Delete",(d,w)->request(a,game,path,"DELETE")).setNegativeButton("Cancel",(d,w)->open(a,game,parent(path))).show();}
}
