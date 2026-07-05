package com.springrest.helpers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ScriptResult {

    private final String url;       
    private final String body;
    private final String rawBody;    

    private final Map<String, String> headers;
    private final Map<String, String> queryParams;

    public ScriptResult(String body,
                        Map<String, String> headers,
                        Map<String, String> queryParams) {
        this(null, body, null, headers, queryParams);
    }

    public ScriptResult(String url,
                        String body,
                        String rawBody,
                        Map<String, String> headers,
                        Map<String, String> queryParams) {
        this.url = url;
        this.body = body;
        this.rawBody = rawBody;
        this.headers = headers != null ? headers : Collections.emptyMap();
        this.queryParams = queryParams != null ? queryParams : Collections.emptyMap();
    }

    public static ScriptResult empty() {
        return new ScriptResult(null, null, null,
                Collections.emptyMap(), Collections.emptyMap());
    }

    @SuppressWarnings("unchecked")
    public static ScriptResult fromScriptObject(Map<String, Object> obj) {

        String url     = obj.containsKey("Url") ? (String) obj.get("Url") : null;
        String body    = obj.containsKey("Body") ? (String) obj.get("Body") : null;
        String rawBody = obj.containsKey("RawBody") ? (String) obj.get("RawBody") : null;

        Map<String, String> headers = new HashMap<>();
        if (obj.get("Headers") instanceof Map<?, ?> rawHeaders) {
            rawHeaders.forEach((k, v) ->
                    headers.put(String.valueOf(k), String.valueOf(v)));
        }

        Map<String, String> query = new HashMap<>();
        if (obj.get("QueryParams") instanceof Map<?, ?> rawQuery) {
            rawQuery.forEach((k, v) ->
                    query.put(String.valueOf(k), String.valueOf(v)));
        }

        return new ScriptResult(url, body, rawBody, headers, query);
    }

    public String getUrl() {
        return url;
    }

    public String getBody() {
        return body;
    }

    public String getRawBody() {
        return rawBody;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }
}
