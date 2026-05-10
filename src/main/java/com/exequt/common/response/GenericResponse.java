package com.exequt.common.response;

import lombok.Getter;

@Getter
public final class GenericResponse<T> extends Response {

    private final T data;

    public GenericResponse(boolean error, String statusCode, String description, T data) {
        super(error, statusCode, description);
        this.data = data;
    }

    public static <T> GenericResponse<T> success(T data) {
        return new GenericResponse<>(false, ResultCode.SUCCESS.getCode(), "Success", data);
    }

    public static <T> GenericResponse<T> failure(ResultCode resultCode, String description) {
        return new GenericResponse<>(true, resultCode.getCode(), description, null);
    }
}
