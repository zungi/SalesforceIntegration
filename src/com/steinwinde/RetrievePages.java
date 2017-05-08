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


public class RetrievePages {

    private static final String ENDPOINT = "services/data/v39.0/tooling/query/?q=SELECT+Id,Name+FROM+ApexPage";

    private static final String PROPERTIES_FILE_NAME = "sf.properties";
    private static final String PROP_TARGET_DIRECTORY = "target_dir";
    
    private class ApexPage {
        String id = null;
        String name = null;
        String namespace = null;
        String body = null;
        
        ApexPage(JsonObject o) {
            this.id = o.getString("Id");
            this.name = o.getString("Name");
            if(!o.isNull("NamespacePrefix")) {
                this.namespace = o.getString("NamespacePrefix");
            }
            this.body = o.getString("Markup");
        }
        @Override
        public String toString() {
            return id + "|" + namespace + "|" + name + "|" + body;
        }
    }
    
    private String instanceUrl = null;
    private String sessionId = null;
    private String dir = null;

    public static void main(String[] args) {
        new RetrievePages();
    }
    
    private RetrievePages() {
        InputStream is = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE_NAME);

        try {
            Properties p = new Properties();
            p.load(is);
            dir = p.getProperty(PROP_TARGET_DIRECTORY);
            SessionManager session = new SessionManager(p);
            instanceUrl = session.getInstanceUrl();
            sessionId = session.getSessionId();
            JsonArray ar = retrieveList();
            List<ApexPage> pages = retrievePages(ar);
            save(pages);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void save(List<ApexPage> pages) throws IOException {
        for(ApexPage p:pages) {
            Files.write(Paths.get(dir + "/" + p.name + ".page"), p.body.getBytes());
        }
    }
    
    private List<ApexPage> retrievePages(JsonArray pageList) throws IOException {
        List<ApexPage> pages = new ArrayList<ApexPage>();
        for(int i = 0, size = pageList.size(); i < size; i++) {
            JsonObject pageDescr = pageList.getJsonObject(i);
            String id = pageDescr.getString("Id");
            String name = pageDescr.getString("Name");
            JsonObject attributes = pageDescr.getJsonObject("attributes");
            String type = attributes.getString("type");
            System.out.println("type: "+type);
            String sUrlPart = attributes.getString("url");
            
            if(!type.equals("ApexPage")) {
                System.out.println("Skipping: " + name);
                continue;
            }
            
            String sUrl = instanceUrl + sUrlPart;
            
            JsonStructure struc = HttpUtils.getJson(sUrl, sessionId);
            System.out.println("Got: "+struc);
            JsonObject obj = (JsonObject) struc;
            ApexPage page = new ApexPage(obj);
            pages.add(page);
        }
        return pages;
    }
    
    private JsonArray retrieveList() throws IOException {
        String sUrl = instanceUrl + "/" + ENDPOINT;
        JsonStructure struc = HttpUtils.getJson(sUrl, sessionId);
        JsonObject obj = (JsonObject) struc;
        JsonArray arr = obj.getJsonArray("records");
        return arr;
    }

}