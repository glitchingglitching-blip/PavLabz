package dev.offline.obbinstaller;
import org.json.*;import java.io.*;import java.security.MessageDigest;import java.util.*;
final class Payload {
 static final String SOCKET="dev.offline.obbinstaller.helper.v1";
 final String packageName,manifestHash;final List<Entry> files=new ArrayList<>();
 static final class Entry{String name,sha256,crc32;long size;final List<Part> parts=new ArrayList<>();}
 static final class Part{String name,url,sha256,crc32;long size;}
 Payload(InputStream source)throws Exception{
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;
  while((n=source.read(buffer))!=-1){if(bytes.size()+n>65536)throw new IOException("Manifest too large");bytes.write(buffer,0,n);}
  manifestHash=hex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));JSONObject json=new JSONObject(bytes.toString("UTF-8"));packageName=json.getString("packageName");
  if(!packageName.equals("com.vankrupt.pavlov") && !packageName.equals("com.CaveManStudio.ContractorsVR"))throw new IOException("Unexpected target package");
  JSONArray array=json.getJSONArray("files");if(array.length()!=2)throw new IOException("Missing required game OBBs");Set<String> names=new HashSet<>();Set<String> kinds=new HashSet<>();
  for(int i=0;i<array.length();i++){
   JSONObject item=array.getJSONObject(i);Entry e=new Entry();e.name=item.getString("name");e.sha256=item.getString("sha256");e.size=item.getLong("size");e.crc32=item.optString("crc32","");
   if(!e.name.matches("(main|patch)\\.[0-9]+\\."+java.util.regex.Pattern.quote(packageName)+"\\.obb") || !names.add(e.name) || !e.sha256.matches("[0-9a-f]{64}") || e.size<=0 || e.size>20L*1024*1024*1024)throw new IOException("Invalid file metadata");
   if(!kinds.add(e.name.substring(0,e.name.indexOf('.'))))throw new IOException("Duplicate main/patch kind");
   if(item.has("parts")){
    JSONArray parts=item.getJSONArray("parts");long total=0;if(parts.length()<1 || parts.length()>32 || !e.crc32.matches("[0-9a-f]{8}"))throw new IOException("Invalid part metadata");
    for(int k=0;k<parts.length();k++){
     JSONObject object=parts.getJSONObject(k);Part part=new Part();part.name=object.getString("name");part.url=object.getString("url");part.size=object.getLong("size");part.sha256=object.getString("sha256");part.crc32=object.getString("crc32");java.net.URI uri=new java.net.URI(part.url);
     if(!part.name.equals(e.name+String.format(Locale.ROOT,".part%03d",k+1)) || part.size<=0 || part.size>1900000000L || !part.sha256.matches("[0-9a-f]{64}") || !part.crc32.matches("[0-9a-f]{8}") || !"https".equals(uri.getScheme()) || !"github.com".equals(uri.getHost()) || uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null || !uri.getPath().startsWith("/glitchingglitching-blip/PavLabz/releases/download/") || !uri.getPath().endsWith("/"+part.name))throw new IOException("Invalid GitHub part");total+=part.size;e.parts.add(part);
    }if(total!=e.size)throw new IOException("Part sizes do not match OBB");
   }else{throw new IOException("Expected GitHub release parts");}
   if(!files.isEmpty() && !e.name.split("\\.",3)[1].equals(files.get(0).name.split("\\.",3)[1]))throw new IOException("Mixed OBB versions");
   files.add(e);
  }
 }
 static void verifyStored(File file,Entry e)throws Exception{
  if(file.length()!=e.size)throw new IOException("Final OBB byte count mismatch: "+e.name);
  MessageDigest hash=MessageDigest.getInstance("SHA-256");java.util.zip.CRC32 crc=new java.util.zip.CRC32();long size=0;
  try(InputStream input=new FileInputStream(file)){byte[] b=new byte[1048576];int n;while((n=input.read(b))!=-1){size+=n;hash.update(b,0,n);crc.update(b,0,n);}}
  if(size!=e.size || !hex(hash.digest()).equals(e.sha256) || !String.format(Locale.ROOT,"%08x",crc.getValue()).equals(e.crc32))throw new IOException("Final OBB integrity mismatch: "+e.name);
 }
 static String hex(byte[] bytes){StringBuilder result=new StringBuilder();for(byte b:bytes)result.append(String.format(Locale.ROOT,"%02x",b&255));return result.toString();}
 static String hash(File file)throws Exception{MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] b=new byte[1048576];int n;while((n=in.read(b))!=-1)digest.update(b,0,n);}return hex(digest.digest());}
}
