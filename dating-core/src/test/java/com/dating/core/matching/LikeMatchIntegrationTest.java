package com.dating.core.matching;

import com.dating.core.matching.service.LikeMatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
@SpringBootTest
@Import(TestKafkaMatchConsumer.class)
public class LikeMatchIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafkaContainer =
            new KafkaContainer("apache/kafka:4.3.1");


    @Autowired
    TestKafkaMatchConsumer listener;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    LikeMatchService service;


    @Test
    public void parallel_mutualLike_yieldsExactlyOneMatch_andOneEvent() throws Exception {

        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate  = new CountDownLatch(2);

        Runnable aLikesB = () -> {
            try {
                startGate.await();
                service.setLike(a, b);
                service.setMatch(a, b);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneGate.countDown();
            }
        };

        Runnable bLikesA = () -> {
            try {
                startGate.await();
                service.setLike(b, a);
                service.setMatch(b, a);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneGate.countDown();
            }
        };

        boolean finished;
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            pool.submit(aLikesB);
            pool.submit(bLikesA);

            startGate.countDown();
            finished = doneGate.await(10, TimeUnit.SECONDS);
            pool.shutdown();
        }

        assertThat(finished).isTrue();
        Integer matches = jdbc.queryForObject(
                "select count(*) from matches where user_low = cast(? as uuid) and user_high = cast(? as uuid)",
                Integer.class,
                (a.compareTo(b) < 0 ? a : b).toString(),
                (a.compareTo(b) < 0 ? b : a).toString());
        assertThat(matches).isEqualTo(1);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(()-> assertThat(listener.received).hasSize(1));
    }

    @Test
    void setMatch_afterMatchAlreadyExists_returnsTrueIdempotently() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        service.setLike(a, b);
        service.setLike(b, a);
        boolean firstMatch = service.setMatch(a, b);
        assertThat(firstMatch).isTrue();

        service.setLike(a, b);
        boolean secondMatch = service.setMatch(a, b);

        assertThat(secondMatch).isTrue();

        UUID low  = a.compareTo(b) < 0 ? a : b;
        UUID high = a.compareTo(b) < 0 ? b : a;
        Integer matchCount = jdbc.queryForObject(
                "select count(*) from matches where user_low = cast(? as uuid) and user_high = cast(? as uuid)",
                Integer.class, low.toString(), high.toString());
        assertThat(matchCount).isEqualTo(1);


        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(listener.received).hasSize(1));
    }




}
