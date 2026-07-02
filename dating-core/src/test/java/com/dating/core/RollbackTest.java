package com.dating.core;

import com.dating.core.auth.api.events.UserRegistered;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
public class RollbackTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafkaContainer =
            new KafkaContainer("apache/kafka:4.3.1");

    @Autowired
    MockMvc mockMvc;


    @Autowired
    JdbcTemplate template;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @TestConfiguration
    static class FailingListenerConfig {
        @EventListener
        void onUserRegistered(UserRegistered event) {
            throw new RuntimeException("boom"); // роняем транзакцию register
        }
    }

    @Test
    void testRollback() throws Exception {
        var register = Map.of("email", "a2@mail.ru", "password", "password123",
                "displayName", "Аня2");
        Integer user_count = template.queryForObject("select count(*) from users", Integer.class);
        Integer events_count = template.queryForObject("select count(*) from event_publication", Integer.class);

        assertThatThrownBy(() -> mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register))))
                .hasRootCauseMessage("boom");

        assertThat(user_count).isEqualTo(template.queryForObject("select count(*) from users", Integer.class));
        assertThat(events_count).isEqualTo(template.queryForObject("select count(*) from event_publication", Integer.class));
    }
}
