package com.Minterest.ImageHosting.exception.Image;

import org.springframework.http.HttpStatus;

public class FileUploadException extends S3Exception{
    public FileUploadException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "FILE_UPLOAD_ERROR");
    }
}
