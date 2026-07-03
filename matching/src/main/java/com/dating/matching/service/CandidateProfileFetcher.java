package com.dating.matching.service;


import com.dating.core.profile.grpc.proto.GetProfileRequest;
import com.dating.core.profile.grpc.proto.GetProfilesBatchRequest;
import com.dating.core.profile.grpc.proto.ProfileMessage;
import com.dating.core.profile.grpc.proto.ProfileServiceGrpc;
import io.grpc.StatusRuntimeException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Service
public class CandidateProfileFetcher {

    private final ProfileServiceGrpc.ProfileServiceBlockingStub stub;

    public CandidateProfileFetcher(ProfileServiceGrpc.ProfileServiceBlockingStub stub) {
        this.stub = stub;
    }
// Альтернатива batch: 1 round-trip, но «всё или ничего» по отказу и без демонстрации concurrency. Оправдан для одного источника; fan-out — для мульти-источника + best-effort
    public List<ProfileMessage> batchFetchCandidateProfiles(List<UUID> candidateIds) {
        return stub.getProfilesBatch(GetProfilesBatchRequest
                                    .newBuilder()
                                    .addAllIds(candidateIds.stream().map(UUID::toString).toList())
                                    .build())
                    .getProfilesList();
    }

    public List<ProfileMessage> fetchCandidateProfiles(List<UUID> candidateIds) {
        try(var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Optional<ProfileMessage>>> futures = candidateIds.stream().map(id-> executor.submit(()->fetchOne(id))).toList();
            return futures.stream().map(this::getQuietly).flatMap(Optional::stream).toList();
        }
    }

    private Optional<ProfileMessage> getQuietly(Future<Optional<ProfileMessage>> future) {
        try {
            return future.get();
        } catch (ExecutionException e) {
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private Optional<ProfileMessage> fetchOne(UUID id) {
        try {
            var res = stub.withDeadlineAfter(2, TimeUnit.SECONDS).getProfile(GetProfileRequest.newBuilder().setId(id.toString()).build());
            return Optional.of(res);
        } catch (StatusRuntimeException e) {
            return Optional.empty();
        }
    }
}
