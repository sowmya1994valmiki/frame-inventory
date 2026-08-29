package com.global.ct.frameinventory.exception;

public class FrameNotFoundException extends RuntimeException {

    public FrameNotFoundException(String frameId) {
        super("Frame '" + frameId + "' was not found");
    }
}
