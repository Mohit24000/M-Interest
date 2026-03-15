package com.Minterest.ImageHosting.exception.Image;

import org.springframework.http.HttpStatus;

public class InvalidFileTypeException extends S3Exception{
    public InvalidFileTypeException(String message) {
        super(message, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "INVALID_FILE_TYPE");
    }
}
