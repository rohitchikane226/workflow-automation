package com.springrest.helpers;

import com.springrest.Entities.Action;
import com.springrest.Entities.Connection;
import com.springrest.helpers.PostScriptResult;
import com.springrest.helpers.ScriptResult;
import com.springrest.httpUtils.HttpUtils;
import com.springrest.repository.ActionRepository;
import com.springrest.services.AuthGenerationService;
import com.springrest.services.ScriptEngineService;

import java.util.HashMap;
import java.util.Map;



public class JavaActionExecutor {

    private final ActionRepository actionRepository;
    private final AuthGenerationService authService;
    private final ScriptEngineService scriptEngineService;
    private final HttpUtils httpUtils;

    public JavaActionExecutor(
            ActionRepository actionRepository,
            AuthGenerationService authService,
            ScriptEngineService scriptEngineService,
            HttpUtils httpUtils
    ) {
        this.actionRepository = actionRepository;
        this.authService = authService;
        this.scriptEngineService = scriptEngineService;
        this.httpUtils = httpUtils;
    }

    /**
     * Called from JS: executeAction(actionKey, requestJson)
     */
    public String execute(
            String actionKey,
            String rawRequestBody,
            Connection connection
    ) {

        try {
            Action action = actionRepository.findByActionKey(actionKey)
                    .orElseThrow(() -> new RuntimeException("Action not found: " + actionKey));

            /*
             * 1️⃣ AUTH
             */
            AuthGenerationService.AuthResult auth =
                    authService.applyAuth(connection.getId(), action.getApiEndpoint());

            String finalUrl = auth.getFinalUrl();
            Map<String, String> headers = new HashMap<>(auth.getHeaders());
            Map<String, String> params  = new HashMap<>(auth.getQueryParams());

            /*
             * 2️⃣ PRE SCRIPT
             */
            ScriptResult pre = scriptEngineService.executePreScript(
                    action.getScript(),
                    finalUrl,
                    "{}",
                    rawRequestBody,
                    headers,
                    params,
                    connection
            );

            if (pre.getUrl() != null) {
                finalUrl = pre.getUrl().trim();
            }
            headers.putAll(pre.getHeaders());
            params.putAll(pre.getQueryParams());

    
            Map<String, Object> apiResponse = httpUtils.callApiWithPlacement(
                    finalUrl,
                    action.getHttpMethod(),
                    headers,
                    params,
                    pre.getBody()
            );

            int statusCode = (int) apiResponse.get("statusCode");
            String responseBody = (String) apiResponse.get("body");

            /*
             * 4️⃣ AUTH FAILURE
             */
            if (statusCode == 401 || statusCode == 403) {
                throw new RuntimeException("Authentication expired");
            }

            /*
             * 5️⃣ POST SCRIPT
             */
            PostScriptResult post = scriptEngineService.executePostScript(
                    action.getScript(),
                    statusCode,
                    responseBody,
                    connection
            );

            if (!"success".equalsIgnoreCase(post.getStatus())) {
                throw new RuntimeException(post.getMessage());
            }

            /*
             * 6️⃣ RETURN RAW RESPONSE
             */
            return post.getMessage() != null
                    ? post.getMessage()
                    : responseBody;

        } catch (Exception e) {
            throw new RuntimeException("executeAction failed: " + e.getMessage());
        }
    }

}
