package com.dating.core;

import com.dating.core.auth.domain.User;
import com.dating.core.auth.repo.UserRepository;
import com.dating.core.profile.domain.Profile;
import com.dating.core.profile.grpc.proto.GetProfileRequest;
import com.dating.core.profile.grpc.proto.GetProfilesBatchRequest;
import com.dating.core.profile.grpc.proto.ProfileMessage;
import com.dating.core.profile.grpc.proto.ProfileServiceGrpc;
import com.dating.core.profile.repo.ProfileRepository;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.LocalGrpcServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

// TODO: gRPC-сервер поднимается на фиксированном порту 9090 — тест упадёт, если порт занят
//  (например, локально запущен core). Правильно: spring.grpc.server.port=0 в @SpringBootTest
//  properties + @LocalGrpcPort в поле (аналогично RANDOM_PORT для HTTP).
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = "spring.grpc.server.port=0")
@Testcontainers
public class GrpcIntegrationTest {
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    private static final KafkaContainer kafkaContainer = new KafkaContainer("apache/kafka:4.3.1");

    @Autowired
    private ProfileRepository repo;

    @Autowired
    private UserRepository userRepo;

    @LocalGrpcServerPort
    private int grpcPort;

    private ManagedChannel channel;
    private  ProfileServiceGrpc.ProfileServiceBlockingStub stub;

    @BeforeEach
    void setupChannel() {
        channel = ManagedChannelBuilder.forAddress("localhost", grpcPort).usePlaintext().build();
        stub = ProfileServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
    }

    @BeforeEach
    void cleanDB() {
        repo.deleteAll();
    }


    @Test
    public void getProfile_returnsProfileFromDb() {
        var user = userRepo.save(new User("a@mail.su", "12333333"));
        var saved = repo.save(new Profile(user.getId(), "TestProfile1"));

        var result = stub.getProfile(GetProfileRequest.newBuilder().setId(saved.getId().toString()).build());

        assertThat(result.getDisplayName()).isEqualTo("TestProfile1");


    }

    @Test
    public void getProfile_notFound_throwsNotFound() {
        assertThatThrownBy(() -> stub.getProfile(GetProfileRequest.newBuilder().setId(UUID.randomUUID().toString()).build()))
                .isInstanceOf(StatusRuntimeException.class)
                .extracting(e -> ((StatusRuntimeException) e).getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
    }

    @Test
    public void getProfilesBatch_returnsFound() {
        var user1 = userRepo.save(new User("a1@mail.su", "12333333"));
        var user2 = userRepo.save(new User("a2@mail.su", "12333333"));
        Profile profile1 = repo.save(new Profile(user1.getId(), "TestProfile1"));
        Profile profile2 = repo.save(new Profile(user2.getId(), "TestProfile2"));

        var result = stub.getProfilesBatch(GetProfilesBatchRequest.newBuilder()
                                            .addAllIds(List.of(profile1.getId().toString(),
                                                                profile2.getId().toString(),
                                                                UUID.randomUUID().toString()))
                                            .build());
        assertThat(result.getProfilesList())
                .hasSize(2)
                .extracting(ProfileMessage::getDisplayName)
                .containsExactlyInAnyOrder("TestProfile1", "TestProfile2");
    }
}
