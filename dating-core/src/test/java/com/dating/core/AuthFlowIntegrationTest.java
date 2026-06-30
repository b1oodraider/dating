package com.dating.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Map;


import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(TestKafkaConsumer.class)
class AuthFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafkaContainer =
            new KafkaContainer("apache/kafka:4.3.1");

    @Autowired MockMvc mockMvc;

    @Autowired TestKafkaConsumer listener;

    @Autowired
    JdbcTemplate template;


    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void register_login_accessProtected_flow() throws Exception {
        var register = Map.of("email", "a@mail.ru", "password", "password123",
                "displayName", "Аня");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());


        var login = Map.of("email", "a@mail.ru", "password", "password123");
        String body = mockMvc.perform(post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(login)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").exists())
                                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(body).get("accessToken").asText();


        mockMvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Аня"));

        mockMvc.perform(get("/api/profiles/me"))
                .andExpect(status().isUnauthorized());
    }





    @Test
    void events_consistent_flow() throws Exception {
        var register = Map.of("email", "a1@mail.ru", "password", "password123",
                "displayName", "Аня");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());


        Integer number = template.queryForObject("select count(*) from event_publication", Integer.class);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isConflict());
        assertThat(number).isEqualTo(template.queryForObject("select count(*) from event_publication", Integer.class));
    }

    @Test
    void isKafkaListening() throws Exception {
        var register = Map.of("email", "testkafka@mail.ru", "password", "password123",
                "displayName", "Аня");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());
        await().atMost(Duration.ofSeconds(15)).untilAsserted(()-> assertThat(listener.received)
                                                                        .anyMatch(m -> m.email().equals("testkafka@mail.ru")));
    }
}
