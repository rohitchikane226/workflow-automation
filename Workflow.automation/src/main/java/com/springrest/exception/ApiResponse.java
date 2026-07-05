package com.springrest.exception;

import java.time.LocalDateTime;

public class ApiResponse<T> {

	private boolean success;
	private T data;
	private String message;
	private LocalDateTime timestamp;

	public ApiResponse(boolean success, T data, String message) {
		this.success = success;
		this.data = data;
		this.message = message;
		this.timestamp = LocalDateTime.now();
	}

	public boolean isSuccess() {
		return success;
	}

	public T getData() {
		return data;
	}

	public String getMessage() {
		return message;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}
}