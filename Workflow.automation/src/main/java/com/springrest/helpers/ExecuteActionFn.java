package com.springrest.helpers;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.springrest.Entities.Connection;

import java.util.Map;

public class ExecuteActionFn extends BaseFunction {

    private final JavaActionExecutor actionExecutor;
    private final Connection connection;

    public ExecuteActionFn(
            JavaActionExecutor actionExecutor,
            Connection connection
    ) {
        this.actionExecutor = actionExecutor;
        this.connection = connection;
    }

    @Override
    public Object call(
            Context cx,
            Scriptable scope,
            Scriptable thisObj,
            Object[] args
    ) {

        if (args.length < 2) {
            return Context.javaToJS(
                    Map.of(
                            "status", "error",
                            "message", "executeAction requires (actionKey, requestJson)"
                    ),
                    scope
            );
        }

        String actionKey = Context.toString(args[0]);
        String requestJson = Context.toString(args[1]);

//        Map<String, Object> result =
//                actionExecutor.execute(actionKey, requestJson, connection);
        String result =
                actionExecutor.execute(actionKey, requestJson, connection);

      
        return Context.javaToJS(result, scope);
    }
}
