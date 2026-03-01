package com.vfu.chatbot.exception;

public class AiToolException extends Exception {
    public AiToolException() {
        super();
    }

    public AiToolException(String message) {
        super(message);
    }

    public AiToolException(String message, Throwable cause) {
        super(message, cause);
    }

    public AiToolException(Throwable cause) {
    }
}
