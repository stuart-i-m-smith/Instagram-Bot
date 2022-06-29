package com.client.rest;

import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;

public class EntityHelper {

    public static String getString(HttpEntity entity) throws IOException {
        return EntityUtils.toString(entity);
    }

    public static JSONObject getJson(HttpEntity entity) throws IOException {
        return new JSONObject(EntityUtils.toString(entity));
    }
}
