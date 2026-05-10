package com.exequt.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResultCode {

    SUCCESS("SUCCESS"),
    BAD_REQUEST("BAD_REQUEST"),
    NOT_FOUND("NOT_FOUND"),
    BUSINESS_CONFLICT("BUSINESS_CONFLICT"),
    VALIDATION_ERROR("VALIDATION_ERROR"),
    DUPLICATE_EVENT("DUPLICATE_EVENT"),
    INTERNAL_ERROR("INTERNAL_ERROR");

    private final String code;
}
