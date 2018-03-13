package com.trick2live.parser.rtf.exception;

/**
 * Represents any exception that can occur during extracting.
 */
public class PlainTextExtractorException extends Exception {
    public PlainTextExtractorException(Throwable cause) {
        super(cause);
    }

    public PlainTextExtractorException(String message) {
        super(message);
    }

    public PlainTextExtractorException(String message, Throwable cause) {
        super(message, cause);
    }
}
