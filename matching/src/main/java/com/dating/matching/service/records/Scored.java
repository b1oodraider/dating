package com.dating.matching.service.records;

import com.dating.core.profile.grpc.proto.ProfileMessage;

public record Scored(ProfileMessage profile, double score) {
}
