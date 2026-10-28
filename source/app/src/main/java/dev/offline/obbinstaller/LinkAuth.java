package dev.offline.obbinstaller;
import java.io.*;import java.security.*;import javax.crypto.Mac;import javax.crypto.spec.SecretKeySpec;
final class LinkAuth {
 static byte[] nonce(){byte[] b=new byte[32];new SecureRandom().nextBytes(b);return b;}
 static byte[] proof(byte[] key,String role,byte[] a,byte[] b)throws Exception{Mac m=Mac.getInstance("HmacSHA256");m.init(new SecretKeySpec(key,"HmacSHA256"));m.update(role.getBytes("UTF-8"));m.update(a);return m.doFinal(b);}
 static void client(DataInputStream in,DataOutputStream out,byte[] key)throws Exception{byte[] a=nonce(),b=new byte[32],p=new byte[32];out.write(a);out.flush();in.readFully(b);in.readFully(p);if(!MessageDigest.isEqual(p,proof(key,"server",a,b)))throw new IOException("Helper authentication failed");out.write(proof(key,"client",a,b));out.flush();}
 static void server(DataInputStream in,DataOutputStream out,byte[] key)throws Exception{byte[] a=new byte[32],b=nonce(),p=new byte[32];in.readFully(a);out.write(b);out.write(proof(key,"server",a,b));out.flush();in.readFully(p);if(!MessageDigest.isEqual(p,proof(key,"client",a,b)))throw new IOException("Unauthorized client");}
}
