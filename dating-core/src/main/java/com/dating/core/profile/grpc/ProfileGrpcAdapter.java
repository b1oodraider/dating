package com.dating.core.profile.grpc;

import com.dating.core.common.error.NotFoundException;
import com.dating.core.profile.api.dto.ProfileResponse;
import com.dating.core.profile.grpc.proto.*;
import com.dating.core.profile.service.ProfileService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

import java.util.List;
import java.util.UUID;

@GrpcService
public class ProfileGrpcAdapter extends ProfileServiceGrpc.ProfileServiceImplBase {
    private final ProfileService profileService;

    public ProfileGrpcAdapter(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Override
    public void getProfile(GetProfileRequest request, StreamObserver<ProfileMessage> responseObserver) {
        try {
            UUID id = UUID.fromString(request.getId());
            ProfileResponse dto = profileService.getById(id);
            responseObserver.onNext(toProto(dto));
            responseObserver.onCompleted();
        } catch (NotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("Profile not found").asRuntimeException());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withCause(e).asRuntimeException());
        }
    }

    @Override
    public void getProfilesBatch(GetProfilesBatchRequest request,
                                 StreamObserver<GetProfilesBatchResponse> responseObserver) {
        try {
            List<UUID> ids = request.getIdsList().stream().map(UUID::fromString).toList();
            List<ProfileResponse> dtos = profileService.getProfilesBatch(ids);
            GetProfilesBatchResponse resp = GetProfilesBatchResponse.newBuilder().addAllProfiles(dtos.stream().map(this::toProto).toList()).build();
            responseObserver.onNext(resp);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("One or more of IDs are invalid\n" + e.getMessage()).asRuntimeException());
        }catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withCause(e).asRuntimeException());
        }
    }

    private ProfileMessage toProto(ProfileResponse dto) {
        ProfileMessage.Builder builder = ProfileMessage.newBuilder();
        builder.setId(dto.id().toString()).setUserId(dto.userId().toString()).setDisplayName(dto.displayName());
        if(dto.bio()!= null) {
            builder.setBio(dto.bio());
        }
        if(dto.birthDate() != null) {
            builder.setBirthDate(dto.birthDate().toString());
        }
        if(dto.gender() != null) {
            builder.setGender(dto.gender());
        }
        if(dto.city() != null) {
            builder.setCity(dto.city());
        }
        return builder.build();
    }
}
