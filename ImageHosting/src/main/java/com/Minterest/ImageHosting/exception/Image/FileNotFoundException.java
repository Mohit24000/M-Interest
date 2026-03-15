package com.Minterest.ImageHosting.exception.Image;

import org.springframework.http.HttpStatus;
public class FileNotFoundException extends S3Exception {
    public FileNotFoundException(String filename) {
        super("File not found: " + filename, HttpStatus.NOT_FOUND, "FILE_NOT_FOUND");
    } }
