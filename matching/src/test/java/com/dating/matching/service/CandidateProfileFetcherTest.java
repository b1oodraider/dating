package com.dating.matching.service;

import com.dating.core.profile.grpc.proto.GetProfileRequest;
import com.dating.core.profile.grpc.proto.ProfileMessage;
import com.dating.core.profile.grpc.proto.ProfileServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CandidateProfileFetcherTest {
    @Mock
    ProfileServiceGrpc.ProfileServiceBlockingStub stub;

    CandidateProfileFetcher fetcher;

    @BeforeEach
    void setUp() { fetcher = new CandidateProfileFetcher(stub); }

    @Test
    public void fetchCandidateProfiles_allSucceed_returnsAll() {

        UUID uuid1 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        when(stub.getProfile(GetProfileRequest.newBuilder().setId(uuid1.toString()).build())).thenReturn(getPM(uuid1.toString()));
        when(stub.getProfile(GetProfileRequest.newBuilder().setId(uuid2.toString()).build())).thenReturn(getPM(uuid2.toString()));
        when(stub.getProfile(GetProfileRequest.newBuilder().setId(uuid3.toString()).build())).thenReturn(getPM(uuid3.toString()));
        when(stub.withDeadlineAfter(anyLong(), any())).thenReturn(stub);

        assertThat(fetcher.fetchCandidateProfiles(List.of(uuid1,uuid2,uuid3))).hasSize(3);
    }

    @Test
    public void fetchCandidateProfiles_someFail_skipsFailedReturnsRest() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        when(stub.getProfile(GetProfileRequest.newBuilder().setId(uuid1.toString()).build())).thenReturn(getPM(uuid1.toString()));
        when(stub.getProfile(GetProfileRequest.newBuilder().setId(uuid2.toString()).build())).thenReturn(getPM(uuid2.toString()));
        when(stub.getProfile(GetProfileRequest.newBuilder().setId(uuid3.toString()).build())).thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));
        when(stub.withDeadlineAfter(anyLong(), any())).thenReturn(stub);
        assertThat(fetcher.fetchCandidateProfiles(List.of(uuid1,uuid2,uuid3))).hasSize(2);

    }

    @Test
    public void fetchCandidateProfiles_emptyInput_returnsEmpty() {
        assertThat(fetcher.fetchCandidateProfiles(List.of())).isEmpty();
    }

    private ProfileMessage getPM(String uuid) {
        return ProfileMessage.newBuilder().setId(uuid).setCity("Moscow").setGender("male").build();
    }
}
