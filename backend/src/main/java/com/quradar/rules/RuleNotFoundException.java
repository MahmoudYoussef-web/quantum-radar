package com.quradar.rules;

public class RuleNotFoundException extends RuntimeException {

    public RuleNotFoundException(String code) {
        super("Unknown rule: " + code);
    }
}
