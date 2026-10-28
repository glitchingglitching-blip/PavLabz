package dev.offline.obbinstaller;
import android.content.Context;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.math.BigInteger;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.*;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import io.github.muntashirakon.adb.AbsAdbConnectionManager;
final class WirelessAdb extends AbsAdbConnectionManager {
 private PrivateKey key; private Certificate cert;
 WirelessAdb(Context c) throws Exception {
  setApi(android.os.Build.VERSION.SDK_INT); setTimeout(20,java.util.concurrent.TimeUnit.SECONDS);
  File k=new File(c.getFilesDir(),"adb-key"), t=new File(c.getFilesDir(),"adb-cert");
  if(k.exists() && t.exists()) {
   key=KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(java.nio.file.Files.readAllBytes(k.toPath())));
   try(InputStream in=new FileInputStream(t)){cert=CertificateFactory.getInstance("X.509").generateCertificate(in);}
  } else {
   KeyPairGenerator g=KeyPairGenerator.getInstance("RSA");g.initialize(2048);java.security.KeyPair pair=g.generateKeyPair();key=pair.getPrivate();
   X500Name name=new X500Name("CN=Quest OBB Installer");long now=System.currentTimeMillis();
   cert=new JcaX509CertificateConverter().getCertificate(new JcaX509v3CertificateBuilder(name,new BigInteger(128,new SecureRandom()),new Date(now-86400000L),new Date(now+315360000000L),name,pair.getPublic()).build(new JcaContentSignerBuilder("SHA256withRSA").build(key)));
   java.nio.file.Files.write(k.toPath(),key.getEncoded());java.nio.file.Files.write(t.toPath(),cert.getEncoded());
  }
 }
 protected PrivateKey getPrivateKey(){return key;}
 protected Certificate getCertificate(){return cert;}
 protected String getDeviceName(){return "QuestObbInstaller";}
 String shell(String command) throws Exception {
  try(io.github.muntashirakon.adb.AdbStream stream=openStream("shell:"+command+"; printf '\\n__QUEST_OBB_END__\\n'; sleep 1");InputStream in=stream.openInputStream()) {
   ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){b.write(buf,0,n);String text=b.toString("UTF-8");int end=text.indexOf("\n__QUEST_OBB_END__");if(end>=0)return text.substring(0,end);if(b.size()>1048576)throw new IOException("Shell response too large");}throw new IOException("Shell closed before command completed");
  }
 }
}
