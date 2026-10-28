package dev.offline.obbinstaller;
import java.io.*;import java.nio.file.*;import java.security.*;import java.util.zip.*;
public class ContractorsCopyTest {
 static Payload.Entry entry(byte[] b)throws Exception{Payload.Entry e=new Payload.Entry();e.name="main.181.com.CaveManStudio.ContractorsVR.obb";e.size=b.length;e.sha256=Payload.hex(MessageDigest.getInstance("SHA-256").digest(b));CRC32 c=new CRC32();c.update(b);e.crc32=String.format("%08x",c.getValue());return e;}
 static void copy(File d,Payload.Entry e,byte[] b,boolean backup,boolean force)throws Exception{ByteArrayOutputStream request=new ByteArrayOutputStream();DataOutputStream w=new DataOutputStream(request);w.writeLong(b.length);w.write(b);w.writeUTF(e.sha256);Helper.copy(d,e,new DataOutputStream(new ByteArrayOutputStream()),backup,new DataInputStream(new ByteArrayInputStream(request.toByteArray())),force);}
 public static void main(String[] args)throws Exception{
 Path root=Files.createTempDirectory(Path.of(args[0]),"helper-regression-");byte[] data="verified new OBB bytes".getBytes("UTF-8");Payload.Entry e=entry(data);
 for(boolean backup:new boolean[]{false,true}){File d=Files.createDirectory(root.resolve("older-"+backup)).toFile();File older=new File(d,"main.180.com.CaveManStudio.ContractorsVR.obb");Files.write(older.toPath(),new byte[]{1,2,3});copy(d,e,data,backup,false);if(older.exists()||new File(d,".backup-"+older.getName()).exists()!=backup)throw new AssertionError("Older replacement");Payload.verifyStored(new File(d,e.name),e);}
 File d=Files.createDirectory(root.resolve("same-name")).toFile();Files.write(new File(d,e.name).toPath(),data);
 Helper.copy(d,e,new DataOutputStream(new ByteArrayOutputStream()),false,new DataInputStream(new ByteArrayInputStream(new byte[0])),false);
 copy(d,e,data,true,true);if(!new File(d,".backup-"+e.name).isFile())throw new AssertionError("Force skipped matching file");
 File bad=Files.createDirectory(root.resolve("corrupt")).toFile();File prior=new File(bad,e.name);Files.write(prior.toPath(),new byte[]{9});byte[] altered=data.clone();altered[0]^=1;try{copy(bad,e,altered,false,true);throw new AssertionError("Accepted corruption");}catch(IOException expected){}if(Files.readAllBytes(prior.toPath())[0]!=9)throw new AssertionError("Old file lost");
 System.out.println("PASS: production helper older replacement with/without backup, matching skip, forced identical replacement, corruption preserves old file, final size/SHA/CRC");
 }
}
