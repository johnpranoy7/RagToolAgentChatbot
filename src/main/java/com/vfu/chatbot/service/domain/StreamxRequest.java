package com.vfu.chatbot.service.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StreamxRequest {
    public String methodName;
    public Map<String, String> params;
}
