package com.springrest.services;

import com.springrest.Entities.Connection;
import com.springrest.JavaHelpers.JavaHelpers;
import com.springrest.JavaHelpers.ScriptLogger;
import com.springrest.helpers.ErrorFn;
import com.springrest.helpers.ExecuteActionFn;
import com.springrest.helpers.JavaActionExecutor;
import com.springrest.helpers.PostScriptResult;
import com.springrest.helpers.ScriptResult;
import com.springrest.httpUtils.HttpUtils;
import com.springrest.repository.ActionRepository;
import org.mozilla.javascript.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class ScriptEngineService {

	@Autowired
	ActionRepository actionRepository;
	@Autowired
	AuthGenerationService authService;
	@Autowired
	HttpUtils httpUtils;
	@Autowired
	private ScriptLogger scriptLogger;

	public ScriptResult executePreScript(String script, String url, String body, String rawBody,
			Map<String, String> headers, Map<String, String> queryParams, Connection connection) {

		if (script == null || script.trim().isEmpty()) {
			return ScriptResult.empty();
		}
		Context cx = Context.enter();
		try {
			Scriptable scope = cx.initStandardObjects();
			Object javaHelperObj = Context.javaToJS(JavaHelpers.class, scope);
			ScriptableObject.putProperty(scope, "JavaHelpers", javaHelperObj);
			scope.put("log", scope, scriptLogger);
			JavaActionExecutor executor = new JavaActionExecutor(actionRepository, authService, this, httpUtils);
			scope.put("executeAction", scope, new ExecuteActionFn(executor, connection));
			cx.evaluateString(scope, script, "script", 1, null);
			Scriptable requestObj = cx.newObject(scope);

			ScriptableObject.putProperty(requestObj, "Url", url);
			ScriptableObject.putProperty(requestObj, "Body", body);
			ScriptableObject.putProperty(requestObj, "RawBody", rawBody);

			Scriptable headersObj = cx.newObject(scope);
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				ScriptableObject.putProperty(headersObj, entry.getKey(), entry.getValue());
			}
			ScriptableObject.putProperty(requestObj, "Headers", headersObj);

			Scriptable queryObj = cx.newObject(scope);
			for (Map.Entry<String, String> entry : queryParams.entrySet()) {
				ScriptableObject.putProperty(queryObj, entry.getKey(), entry.getValue());
			}
			ScriptableObject.putProperty(requestObj, "QueryParams", queryObj);
			Scriptable actionObj = (Scriptable) scope.get("action", scope);
			Function preFn = (Function) ScriptableObject.getProperty(actionObj, "pre");
			Object resultObj = preFn.call(cx, scope, actionObj, new Object[] { requestObj });
			if (resultObj instanceof Scriptable res) {
				String newUrl = getString(res, "Url");
				String newBody = getString(res, "Body");
				String newRaw = getString(res, "RawBody");

				Map<String, String> newHeaders = toJavaMap(res, "Headers");
				Map<String, String> newQuery = toJavaMap(res, "QueryParams");

				return new ScriptResult(newUrl, newBody, newRaw, newHeaders, newQuery);
			}

		} catch (Exception e) {
			System.err.println("⚠️ Pre-script error: " + e.getMessage());
		} finally {
			Context.exit();
		}

		return ScriptResult.empty();
	}

	public PostScriptResult executePostScript(String script, int statusCode, String responseBody,
			Connection connection) {
		if (script == null || script.trim().isEmpty()) {
			return PostScriptResult.success(responseBody);
		}

		Context cx = Context.enter();

		try {
			Scriptable scope = cx.initStandardObjects();
			scope.put("success", scope, new ErrorFn("success"));
			scope.put("softError", scope, new ErrorFn("soft_error"));
			scope.put("expiredAuth", scope, new ErrorFn("expired_auth"));
			scope.put("hardError", scope, new ErrorFn("hard_error"));
			scope.put("log", scope, scriptLogger);
			JavaActionExecutor executor = new JavaActionExecutor(actionRepository, authService, this, httpUtils);

			scope.put("executeAction", scope, new ExecuteActionFn(executor, connection));
			cx.evaluateString(scope, script, "script", 1, null);
			Scriptable respObj = cx.newObject(scope);
			ScriptableObject.putProperty(respObj, "StatusCode", statusCode);
			ScriptableObject.putProperty(respObj, "Body", responseBody);
			System.out.println("JS success function = " + scope.get("success", scope).getClass());
			Object actionObjRaw = scope.get("action", scope);
			if (!(actionObjRaw instanceof Scriptable actionObj)) {
				return PostScriptResult.success(responseBody);
			}

			Object postFnObj = ScriptableObject.getProperty(actionObj, "post");
			if (!(postFnObj instanceof Function postFn)) {
				return PostScriptResult.success(responseBody);
			}
			Object resultObj = postFn.call(cx, scope, actionObj, new Object[] { respObj });
			if (resultObj == null || resultObj == Undefined.instance) {
				return PostScriptResult.success(responseBody);
			}
			if (resultObj instanceof Scriptable js) {
				String status = getString(js, "status");
				System.out.println("status " + status);
				String message = getString(js, "message");
				System.out.println("message " + message);
				String raw = getString(js, "raw");
				System.out.println("raw " + raw);
				if (status == null) {
					return PostScriptResult.success(responseBody);
				}

				return new PostScriptResult(status, message, raw);
			}
			return PostScriptResult.success(Context.toString(resultObj));

		} catch (Exception ex) {
			ex.printStackTrace();
			return PostScriptResult.hardError("Post script crashed", responseBody);

		} finally {
			Context.exit();
		}
	}

	private Map<String, String> toJavaMap(Scriptable obj, String key) {
		Map<String, String> map = new HashMap<>();
		Object nested = ScriptableObject.getProperty(obj, key);
		if (nested instanceof Scriptable) {
			Scriptable s = (Scriptable) nested;
			for (Object k : s.getIds()) {
				String kStr = k.toString();
				String vStr = Context.toString(ScriptableObject.getProperty(s, kStr));
				map.put(kStr, vStr);
			}
		}
		return map;
	}

	private String getString(Scriptable obj, String key) {
		Object val = ScriptableObject.getProperty(obj, key);
		return val == null || val == Undefined.instance ? null : Context.toString(val);
	}

	public String executeInputFields(String script, String paramsJson, Connection connection) {
		if (script == null || script.trim().isEmpty()) {
			return "[]";
		}
		Context cx = Context.enter();
		try {
			Scriptable scope = cx.initStandardObjects();
			Object javaHelperObj = Context.javaToJS(JavaHelpers.class, scope);
			ScriptableObject.putProperty(scope, "JavaHelpers", javaHelperObj);
			scope.put("log", scope, scriptLogger);
			JavaActionExecutor executor = new JavaActionExecutor(actionRepository, authService, this, httpUtils);

			scope.put("executeAction", scope, new ExecuteActionFn(executor, connection));
			cx.evaluateString(scope, script, "script", 1, null);

			Object actionObjRaw = scope.get("action", scope);
			if (!(actionObjRaw instanceof Scriptable actionObj)) {
				return "[]";
			}

			Object fnObj = ScriptableObject.getProperty(actionObj, "inputFields");
			if (!(fnObj instanceof Function fn)) {
				return "[]";
			}

			Object result = fn.call(cx, scope, actionObj, new Object[] { paramsJson });

			if (result == null || result == Undefined.instance) {
				return "[]";
			}
			return Context.toString(result);

		} catch (Exception e) {
			e.printStackTrace();
			return "[]";
		} finally {
			Context.exit();
		}
	}

	public Map<String, Object> executeWebhookScript(String script, String functionName, Map<String, Object> request,
			Connection connection) {
		Context cx = Context.enter();
		try {
			Scriptable scope = cx.initStandardObjects();
			scope.put("log", scope, scriptLogger);

			Object javaHelperObj = Context.javaToJS(JavaHelpers.class, scope);
			ScriptableObject.putProperty(scope, "JavaHelpers", javaHelperObj);
			JavaActionExecutor executor = new JavaActionExecutor(actionRepository, authService, this, httpUtils);
			scope.put("executeAction", scope, new ExecuteActionFn(executor, connection));
			cx.evaluateString(scope, script, "script", 1, null);

			Object actionObjRaw = scope.get("action", scope);
			if (!(actionObjRaw instanceof Scriptable action)) {
				throw new RuntimeException("script must define action");
			}
			Object fnRaw = ScriptableObject.getProperty(action, functionName);
			if (!(fnRaw instanceof Function fn)) {
				throw new RuntimeException("action." + functionName + "() not found");
			}
			Object jsRequest = Context.javaToJS(request, scope);
			Object result = fn.call(cx, scope, action, new Object[] { jsRequest });
			Map<String, Object> response = new HashMap<>();

			if (result instanceof Scriptable jsObj) {
				for (Object id : jsObj.getIds()) {
					String key = id.toString();
					Object val = ScriptableObject.getProperty(jsObj, key);
					response.put(key, Context.toString(val));
				}
			}
			return response;
		} finally {
			Context.exit();
		}
	}
	public String executeOutputFields(String appScript, String actionScript, String paramsJson, Connection connection) {
	    if ((actionScript == null || actionScript.trim().isEmpty()) &&
	        (appScript == null || appScript.trim().isEmpty())) {
	        return "[]";
	    }

	    Context cx = Context.enter();
	    try {
	        Scriptable scope = cx.initStandardObjects();

	
	        Object javaHelperObj = Context.javaToJS(JavaHelpers.class, scope);
	        ScriptableObject.putProperty(scope, "JavaHelpers", javaHelperObj);
	        scope.put("log", scope, scriptLogger);

	        JavaActionExecutor executor = new JavaActionExecutor(actionRepository, authService, this, httpUtils);
	        scope.put("executeAction", scope, new ExecuteActionFn(executor, connection));
	        String finalScript = "";

	        if (appScript != null && !appScript.trim().isEmpty()) {
	            finalScript += appScript + "\n";
	        }

	        if (actionScript != null) {
	            finalScript += actionScript;
	        }

	        cx.evaluateString(scope, finalScript, "script", 1, null);

	        // Get action object
	        Object actionObjRaw = scope.get("action", scope);
	        if (!(actionObjRaw instanceof Scriptable actionObj)) {
	            return "[]";
	        }

	        // Get outputFields function
	        Object fnObj = ScriptableObject.getProperty(actionObj, "outputFields");
	        if (!(fnObj instanceof Function fn)) {
	            return "[]";
	        }

	        // Convert params
	        Object jsParams = Context.javaToJS(paramsJson, scope);
	        Object jsConnection = Context.javaToJS(connection, scope);

	        // Call JS
	        Object result = fn.call(cx, scope, actionObj, new Object[] { jsParams, jsConnection });

	        if (result == null || result == Undefined.instance) {
	            return "[]";
	        }

	        return Context.toString(result);

	    } catch (Exception e) {
	        e.printStackTrace();
	        return "[]";
	    } finally {
	        Context.exit();
	    }
	}
	public String executeOutputFields(String script, String paramsJson, Connection connection) {

	    if (script == null || script.trim().isEmpty()) {
	        return "[]";
	    }

	    Context cx = Context.enter();
	    try {
	        Scriptable scope = cx.initStandardObjects();

	        // ✅ Helpers
	        Object javaHelperObj = Context.javaToJS(JavaHelpers.class, scope);
	        ScriptableObject.putProperty(scope, "JavaHelpers", javaHelperObj);

	        scope.put("log", scope, scriptLogger);

	        JavaActionExecutor executor = new JavaActionExecutor(actionRepository, authService, this, httpUtils);
	        scope.put("executeAction", scope, new ExecuteActionFn(executor, connection));

	        // ✅ Load script
	        cx.evaluateString(scope, script, "script", 1, null);

	        Object actionObjRaw = scope.get("action", scope);
	        if (!(actionObjRaw instanceof Scriptable actionObj)) {
	            return "[]";
	        }

	        // ✅ Get outputFields function
	        Object fnObj = ScriptableObject.getProperty(actionObj, "outputFields");
	        if (!(fnObj instanceof Function fn)) {
	            return "[]";
	        }

	        // ✅ IMPORTANT: pass params AS STRING (like inputFields)
	        Object result = fn.call(cx, scope, actionObj, new Object[]{paramsJson});

	        if (result == null || result == Undefined.instance) {
	            return "[]";
	        }

	        return Context.toString(result);

	    } catch (Exception e) {
	        e.printStackTrace();
	        return "[]";
	    } finally {
	        Context.exit();
	    }
	}
}
