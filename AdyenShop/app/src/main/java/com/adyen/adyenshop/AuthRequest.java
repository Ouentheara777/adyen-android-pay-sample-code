package com.adyen.adyenshop;

import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class AuthRequest extends JsonObjectRequest {

    public AuthRequest(int method, String url, JSONObject jsonRequest,
                       Response.Listener<JSONObject> listener,
                       Response.ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, errorListener);
    }

    public AuthRequest(String url, JSONObject jsonRequest,
                       Response.Listener<JSONObject> listener,
                       Response.ErrorListener errorListener) {
        super(url, jsonRequest, listener, errorListener);
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
            //return createBasicAuthHeader("ws_891138@Company.GoogleCommerce", "}9w8]]!3h[(SgB==%B@#7!(2D");
        return createBasicAuthHeader("ws_578310@Company.TestCompany", "fuKbmquward9");
    }

    Map<String, String> createBasicAuthHeader(String username, String password) {
        Map<String, String> headerMap = new HashMap<String, String>();

        String credentials = username + ":" + password;
        String encodedCredentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
        headerMap.put("Authorization", "Basic " + encodedCredentials);

        return headerMap;
    }

    //for Taras' merchant server
//    Map<String, String> createBasicAuthHeader(String username, String password) {
//        Map<String, String> headerMap = new HashMap<String, String>();
//
//        headerMap.put("Authorization", "Bearer 1811645d-87a1-4e47-833a-78a1d5f6d4");
//
//        return headerMap;
//    }
}
