package com.vfu.chatbot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
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
