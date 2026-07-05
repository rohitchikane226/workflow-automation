package com.springrest.helpers;

public class PostScriptResult {

	private String status; 
	private String message; 
	private String raw; 

	public PostScriptResult(String status, String message, String raw) {
		this.status = status;
		this.message = message;
		this.raw = raw;
	}

	public static PostScriptResult success(String raw) {
		return new PostScriptResult("success", "OK", raw);
	}

	public static PostScriptResult softError(String msg, String raw) {
		return new PostScriptResult("soft_error", msg, raw);
	}

	public static PostScriptResult expiredAuth(String msg, String raw) {
		return new PostScriptResult("expired_auth", msg, raw);
	}

	public static PostScriptResult hardError(String msg, String raw) {
		return new PostScriptResult("hard_error", msg, raw);
	}

	public String getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}

	public String getRaw() {
		return raw;
	}
}
