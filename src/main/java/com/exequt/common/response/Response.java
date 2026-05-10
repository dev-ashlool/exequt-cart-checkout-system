package com.exequt.common.response;

import lombok.Getter;

@Getter
public abstract class Response {

    private final boolean error;
    private final String statusCode;
    private final String description;

    protected Response(boolean error, String statusCode, String description) {
        this.error = error;
        this.statusCode = statusCode;
        this.description = description;
    }
}
