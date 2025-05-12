package com.rishee.java21apps.utils.exception;

import lombok.Data;
import lombok.Getter;

@Getter
public class Java21AppException extends RuntimeException {

    private final String appName;

    public Java21AppException(String appName, String message) {
        super(message);
        this.appName = appName;
    }

    public Java21AppException(String appName, String message, Throwable cause) {
        super(message, cause);
        this.appName = appName;
    }
}
