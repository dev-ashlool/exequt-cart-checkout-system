package com.exequt.common.exception;

import com.exequt.common.response.ResultCode;

public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super(ResultCode.NOT_FOUND, message);
    }
}
