package com.dating.core.profile.service;

import com.dating.core.profile.api.dto.ProfileResponse;
import com.dating.core.profile.domain.Profile;
import com.dating.core.profile.repo.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GrpcProfileServiceTest {

    @Mock
    ProfileRepository repo;

    @InjectMocks
    ProfileService serv;

    @Test
    public void getProfilesBatch_returnsFoundProfiles_mapsProfilesToResponses() {
        Profile p1 = mock(Profile.class);
        Profile p2 = mock(Profile.class);
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        when(repo.findAllById(any())).thenReturn(List.of(p1, p2));
        when(p1.getDisplayName()).thenReturn("АБЫРВАЛГ");
        when(p1.getId()).thenReturn(u1);
        when(p2.getId()).thenReturn(u2);
        when(p2.getDisplayName()).thenReturn("ГЛАВРЫБА");
        var result = serv.getProfilesBatch(List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProfileResponse::displayName).containsExactlyInAnyOrder("АБЫРВАЛГ", "ГЛАВРЫБА");
        assertThat(result).extracting(ProfileResponse::id).containsExactlyInAnyOrder(u1, u2);
    }
}
