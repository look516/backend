package com.example.backend.service;

import com.example.backend.entity.Topic;
import com.example.backend.entity.User;
import com.example.backend.repository.TopicRepository;
import com.example.backend.request.EventRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final MailService mailService;
    private final UserService userService;
    private final TopicRepository topicRepository;

    @Transactional
    public void publishEvent(EventRequest eventRequest) {

        Topic topic = topicRepository.findByName(eventRequest.getTopicName())
                .orElseThrow(() -> new RuntimeException(eventRequest.getTopicName() + "은 존재하지 않는 토픽입니다."));

        //System.out.println("주제명 " + topic.getName());

        List<User> users = userService.getUsersByTopicName(topic.getName());
        System.out.println(users);
        System.out.println("구독자 수 " + users.size());

        for (User user : users) {
            System.out.print("메일 발송 시도: " + user.getEmail());
            mailService.sendMail(
                    user.getEmail(),
                    "다음 주제에 대한 새 소식: " + topic.getName(),
                    eventRequest.getContent()
            );
        }
    }
}