package com.vfu.chatbot.service.domain;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeoapifyFeature {
    @JsonProperty("properties")
    GeoapifyProperties properties;
}
