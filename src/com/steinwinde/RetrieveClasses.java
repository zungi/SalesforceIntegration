package com.steinwinde;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonStructure;


public class RetrieveClasses {

    private static final String ENDPOINT = "services/data/v39.0/tooling/";

    private static final String PROPERTIES_FILE_NAME = "sf.properties";
    private static final String PROP_TARGET_DIRECTORY = "target_dir";
    
    private class ApexClass {
        String id = null;
        String name = null;
        String body = null;
        
        ApexClass(JsonObject o) {
            JsonArray ar = o.getJsonArray("records");
            if(ar.isEmpty()) {
                System.out.println("Empty json: " + o);
                return;
            }
            JsonObject jo = ar.getJsonObject(0);
            this.id = jo.getString("Id");
            this.name = jo.getString("Name");
            this.body = jo.getString("Body");
        }
        @Override
        public String toString() {
            return id + "|" + name + "|" + body;
        }
    }
    
    private String instanceUrl = null;
    private String sessionId = null;
    private String dir = null;

    public static void main(String[] args) {
        new RetrieveClasses();
    }
    
    private RetrieveClasses() {
        InputStream is = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE_NAME);

        try {
            Properties p = new Properties();
            p.load(is);
            dir = p.getProperty(PROP_TARGET_DIRECTORY);
            SessionManager session = new SessionManager(p);
            instanceUrl = session.getInstanceUrl();
            sessionId = session.getSessionId();
            JsonArray ar = retrieveList();
            List<ApexClass> classes = retrieveClasses(ar);
            save(classes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void save(List<ApexClass> classes) throws IOException {
        for(ApexClass c:classes) {
            Files.write(Paths.get(dir + "/" + c.name + ".cls"), c.body.getBytes());
        }
    }
    
    private List<ApexClass> retrieveClasses(JsonArray classList) throws IOException {
        List<ApexClass> classes = new ArrayList<ApexClass>();
        for(int i = 0, size = classList.size(); i < size; i++) {
            JsonObject clDescr = classList.getJsonObject(i);
            if(!clDescr.getString("type").equals("CLASS")) {
                System.out.println("Skipping (probably trigger): " + clDescr.getString("name"));
                continue;
            }
            if(clDescr.getString("typeRef").contains("$")) {
                System.out.println("Skipping inner class: " + clDescr.getString("name"));
                continue;
            }
            
            String id = clDescr.getString("id");
            String sUrl = instanceUrl + "/" + ENDPOINT
                + "query/?q=Select+Id,Name,Body+from+ApexClass+WHERE+Id='"+id+"'";
        //        + "sobjects/ApexClass/" +classId;
            
            JsonStructure struc = HttpUtils.getJson(sUrl, sessionId);
            JsonObject obj = (JsonObject) struc;
            if(obj.getInt("totalSize")==0) {
                System.out.println("Skipping " + clDescr.getString("namespace") + "." + clDescr.getString("name"));
            } else {
                ApexClass cl = new ApexClass(obj);
                classes.add(cl);
            }
        }
        return classes;
    }
    
    private JsonArray retrieveList() throws IOException {
        String sUrl = instanceUrl + "/" + ENDPOINT + "apexManifest";
        JsonStructure struc = HttpUtils.getJson(sUrl, sessionId);
        JsonArray obj = (JsonArray) struc;
        System.out.println("Found number of classes and triggers: " + obj.size());
        return obj;
    }

}