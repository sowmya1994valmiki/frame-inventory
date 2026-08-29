package com.global.ct.frameinventory.exception;

public class DuplicateFrameException extends RuntimeException {

    public DuplicateFrameException(String frameId) {
        super("Frame '" + frameId + "' already exists");
    }
}
