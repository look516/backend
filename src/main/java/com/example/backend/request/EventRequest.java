package com.example.backend.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventRequest {
    private String topicName;
    private String content;
}