package com.quradar.ingestion;

public class DuplicateEventException extends RuntimeException {

    public DuplicateEventException(String eventId) {
        super("Duplicate submission: event " + eventId + " was already processed");
    }
}
