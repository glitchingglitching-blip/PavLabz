package dev.offline.obbinstaller;
final class GameCatalog {
 static final String PAVLOV="pavlov", CONTRACTORS="contractors";
 static final String[] ASSETS={"payload.json","payload-contractors.json"};
 static java.util.Map<String,Payload> payloads(java.util.zip.ZipFile zip)throws Exception{
  java.util.Map<String,Payload> result=new java.util.HashMap<>();
  for(String game:new String[]{PAVLOV,CONTRACTORS}){
   try(java.io.InputStream input=zip.getInputStream(zip.getEntry("assets/"+asset(game)))){
    Payload p=new Payload(input);if(!p.packageName.equals(target(game)))throw new java.io.IOException("Game manifest target mismatch");
    if(result.put(p.manifestHash,p)!=null)throw new java.io.IOException("Duplicate game manifest");
   }
  }return result;
 }
 static String asset(String game){if(CONTRACTORS.equals(game))return ASSETS[1];if(PAVLOV.equals(game))return ASSETS[0];throw new IllegalArgumentException("Unknown game");}
 static String title(String game){asset(game);return CONTRACTORS.equals(game)?"Contractors":"Pavlov";}
 static String target(String game){asset(game);return CONTRACTORS.equals(game)?"com.CaveManStudio.ContractorsVR":"com.vankrupt.pavlov";}
}
