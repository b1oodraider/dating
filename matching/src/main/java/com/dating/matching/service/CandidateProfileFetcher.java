package com.dating.matching.service;


import com.dating.core.profile.grpc.proto.GetProfileRequest;
import com.dating.core.profile.grpc.proto.GetProfilesBatchRequest;
import com.dating.core.profile.grpc.proto.ProfileMessage;
import com.dating.core.profile.grpc.proto.ProfileServiceGrpc;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final int GRPC_DEADLINE_SECONDS = 2;

    private static final Logger log = LoggerFactory.getLogger(CandidateProfileFetcher.class);
    private final ProfileServiceGrpc.ProfileServiceBlockingStub stub;

    public CandidateProfileFetcher(ProfileServiceGrpc.ProfileServiceBlockingStub stub) {
        this.stub = stub;
    }

    public List<ProfileMessage> batchFetchCandidateProfiles(List<UUID> candidateIds) {
        try {
            return stub.withDeadlineAfter(GRPC_DEADLINE_SECONDS, TimeUnit.SECONDS)
                    .getProfilesBatch(GetProfilesBatchRequest
                            .newBuilder()
                            .addAllIds(candidateIds.stream().map(UUID::toString).toList())
                            .build())
                    .getProfilesList();
        } catch (StatusRuntimeException e) {
            log.warn("Uncommited list of matching profile due to connection problems");
            return List.of();
        }
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
            var res = stub.withDeadlineAfter(GRPC_DEADLINE_SECONDS, TimeUnit.SECONDS).getProfile(GetProfileRequest.newBuilder().setId(id.toString()).build());
            return Optional.of(res);
        } catch (StatusRuntimeException e) {
            return Optional.empty();
        }
    }
}
