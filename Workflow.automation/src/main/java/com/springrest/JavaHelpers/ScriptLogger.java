package com.springrest.JavaHelpers;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ScriptLogger {

    private static final Logger log = LoggerFactory.getLogger("JS_SCRIPT");

    public void info(String msg) {
        log.info(msg);
    }

    public void warn(String msg) {
        log.warn(msg);
    }

    public void error(String msg) {
        log.error(msg);
    }
}
