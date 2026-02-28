package com.vfu.chatbot.service.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AmenityResponse {
    private String id;
    private String groupName;
    private String name;
}
