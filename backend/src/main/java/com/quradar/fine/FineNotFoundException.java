package com.quradar.fine;

public class FineNotFoundException extends RuntimeException {

    public FineNotFoundException(Long id) {
        super("Unknown fine: " + id);
    }
}
