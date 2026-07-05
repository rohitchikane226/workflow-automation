package com.springrest.exception;

import java.time.LocalDateTime;

public class ErrorResponse {

	private boolean success = false;
	private String message;
	private int status;
	private LocalDateTime timestamp;

	public ErrorResponse(String message, int status) {
		this.message = message;
		this.status = status;
		this.timestamp = LocalDateTime.now();
	}

	public boolean isSuccess() {
		return success;
	}

	public String getMessage() {
		return message;
	}

	public int getStatus() {
		return status;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}
}