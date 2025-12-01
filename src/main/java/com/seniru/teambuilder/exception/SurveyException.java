package com.seniru.teambuilder.exception;

public class SurveyException extends Exception {
    public SurveyException(String message) {
        super(message);
    }

    public SurveyException(String message, Throwable cause) {
        super(message, cause);
    }
}