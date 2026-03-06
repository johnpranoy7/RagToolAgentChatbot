package com.vfu.chatbot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class AiToolException extends RuntimeException {
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
