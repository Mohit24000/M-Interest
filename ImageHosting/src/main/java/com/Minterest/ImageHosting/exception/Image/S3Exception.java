package com.Minterest.ImageHosting.exception.Image;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class S3Exception extends RuntimeException{
    private final HttpStatus status;
    private final String errorCode;

    public S3Exception(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}
