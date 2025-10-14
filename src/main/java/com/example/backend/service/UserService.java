package com.example.backend.service;

import com.example.backend.entity.User;
import com.example.backend.entity.Topic;
import com.example.backend.repository.TopicRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;

    public UserService(UserRepository userRepository, TopicRepository topicRepository) {
        this.userRepository = userRepository;
        this.topicRepository = topicRepository;
    }

    public List<User> getUsersByTopicName(String topicName) {
        Topic topic = topicRepository.findByName(topicName)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 토픽: " + topicName));
        return userRepository.findByTopic(topic);
    }
}
