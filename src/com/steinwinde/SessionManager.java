package com.steinwinde;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.json.JsonObject;
import javax.json.JsonStructure;

public class SessionManager {

    private static final String CLIENT_ID = "client_id";
    private static final String CONSUMER_SECRET = "consumer_secret";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String HOST = "host";

    private static final String ENDPOINT = "/services/oauth2/token";

    private Properties p = null;
    
    private String instanceUrl = null;
    private String sessionId = null;
    
    public SessionManager(Properties p) throws IOException {
        if(p==null) {
            throw new IllegalArgumentException("No properties file.");
        }
        if(p.getProperty(CLIENT_ID)==null || p.getProperty(CLIENT_ID).length()==0) {
            throw new IllegalArgumentException("Properties file incomplete.");
        }
        this.p = p;
        getSession();
    }
    
    public String getInstanceUrl() {
        if(instanceUrl == null || instanceUrl.length()==0) {
            throw new IllegalStateException();
        }
        return instanceUrl;
    }
    
    public String getSessionId() {
        if(sessionId==null || sessionId.length()==0) {
            throw new IllegalStateException();
        }
        return sessionId;
    }
    
    /**
     * OAuth 2.0 Username-Password Flow,
     * @see <a href="https://help.salesforce.com/articleView?id=remoteaccess_oauth_username_password_flow.htm">https://help.salesforce.com/articleView?id=remoteaccess_oauth_username_password_flow.htm</a>
     * @return
     * @throws IOException
     */
    private void getSession() throws IOException {
        byte[] postData = HttpUtils.getBody(getAuthenticationHeader());
        String sUrl = "https://" + p.getProperty(HOST) + ENDPOINT;
        JsonStructure struc = HttpUtils.getJson(sUrl, postData, null);
        JsonObject obj = (JsonObject) struc;
        instanceUrl = obj.getString("instance_url");
        sessionId = obj.getString("access_token");
    }
    
    private Map<String, String> getAuthenticationHeader() {
        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "password");
        params.put("client_id", p.getProperty(CLIENT_ID));
        params.put("client_secret", p.getProperty(CONSUMER_SECRET));
        params.put("username", p.getProperty(USERNAME));
        params.put("password", p.getProperty(PASSWORD));
        return params;
    }
}
