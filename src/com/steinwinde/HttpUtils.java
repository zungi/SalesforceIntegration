package com.steinwinde;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.net.ssl.HttpsURLConnection;

public class HttpUtils {

    public static byte[] getBody(Map<String, String> params) throws UnsupportedEncodingException {

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> param : params.entrySet()) {
            if (sb.length() != 0)
                sb.append('&');
            sb.append(URLEncoder.encode(param.getKey(), "UTF-8"));
            sb.append('=');
            sb.append(URLEncoder.encode(param.getValue(), "UTF-8"));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static JsonStructure getJson(String sUrl, byte[] postData, String sessionId) throws IOException {
        HttpsURLConnection conn = getConnection(sUrl, true, false, sessionId);
        try(DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            wr.write(postData);
        }
        return readFromConnection(conn);
    }
    
    public static JsonStructure getJson(String sUrl, String sessionId) throws IOException {
        HttpsURLConnection conn = getConnection(sUrl, false, true, sessionId);
        return readFromConnection(conn);
    }
    
    private static JsonStructure readFromConnection(HttpsURLConnection c) throws IOException {
        try(InputStream is = c.getInputStream(); JsonReader rdr = Json.createReader(is)) {
            return rdr.read();
        } catch(IOException e) {
            if(c.getResponseCode() == 400) {
                try(InputStream is = c.getErrorStream(); JsonReader rdr = Json.createReader(is)) {
                    JsonStructure struc = rdr.read();
                    if(struc instanceof JsonObject) {
                        JsonObject obj = (JsonObject) struc;
                        System.err.println(obj.getString("error_description") + " (" + obj.getString("error") + ")");
                    } else {
                        JsonArray arr = (JsonArray) struc;
                        System.out.println("Array: " + arr);
                    }
                }
            }
            throw e;
        }
    }
    
    private static HttpsURLConnection getConnection(String sUrl, boolean post, boolean setContentType, String sessionId) throws IOException {
        System.out.println("URL: "+sUrl);
        URL url = new URL(sUrl);
        HttpsURLConnection c = (HttpsURLConnection) url.openConnection();
        c.setDoOutput(true);
        c.setInstanceFollowRedirects(false);
        c.setRequestMethod(post?"POST":"GET");
        if(setContentType) {
            c.setRequestProperty("Content-Type", "application/json");
        }
        if(sessionId!=null && sessionId.length()>0) {
            c.setRequestProperty("Authorization", "Bearer "+sessionId);
        }
        c.setUseCaches(false);
        return c;
    }
}
