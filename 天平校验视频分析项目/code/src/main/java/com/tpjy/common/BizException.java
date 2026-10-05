package com.tpjy.common;

/** 业务异常。 */
public class BizException extends RuntimeException {
    public BizException(String message) {
        super(message);
    }
}
