package com.exequt.common.exception;

import com.exequt.common.response.ResultCode;

public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(ResultCode.BUSINESS_CONFLICT, message);
    }
}
