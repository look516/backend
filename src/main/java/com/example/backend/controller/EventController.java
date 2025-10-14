package com.example.backend.controller;

import com.example.backend.request.EventRequest;
import com.example.backend.response.ApiResponse;
import com.example.backend.service.EventService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/publish")
    public ApiResponse publishEvent(@RequestBody EventRequest eventRequest) {
        System.out.println("controller호출" + eventRequest);


        try {
            eventService.publishEvent(eventRequest); // Service 호출
            return new ApiResponse(true, "이벤트 발행 완료");
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse(false, "이벤트 발행 실패: " + e.getMessage());
        }
    }
}