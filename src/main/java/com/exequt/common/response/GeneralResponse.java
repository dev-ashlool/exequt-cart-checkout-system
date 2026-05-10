package com.exequt.common.response;

import lombok.Getter;

@Getter
public final class GeneralResponse extends Response {

    public GeneralResponse(boolean error, String statusCode, String description) {
        super(error, statusCode, description);
    }

    public static GeneralResponse success() {
        return new GeneralResponse(false, ResultCode.SUCCESS.getCode(), "Success");
    }

    public static GeneralResponse failure(ResultCode resultCode, String description) {
        return new GeneralResponse(true, resultCode.getCode(), description);
    }
}
