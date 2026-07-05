package com.springrest.helpers;
import org.mozilla.javascript.*;

public class ErrorFn extends BaseFunction {

    private final String status;

    public ErrorFn(String status) {
        this.status = status;
    }

    @Override
    public Object call(Context cx, Scriptable scope,
                       Scriptable thisObj, Object[] args) {

        String message = args.length > 0 ? Context.toString(args[0]) : "";
        String raw = args.length > 1 ? Context.toString(args[1]) : "";

        Scriptable obj = cx.newObject(scope);
        ScriptableObject.putProperty(obj, "status", status);
        ScriptableObject.putProperty(obj, "message", message);
        ScriptableObject.putProperty(obj, "raw", raw);

        return obj;
    }
}
