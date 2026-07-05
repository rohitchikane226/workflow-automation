package com.springrest.helpers;
import java.util.UUID;

public class WebhookUrlUtil {

    public static String generateWebhookUrl() {
        return "http://localhost:8080/webhooks/" + UUID.randomUUID();
    }
    public String getUrl() {
    	return null;
    }
}

