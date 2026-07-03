package com.dating.core.profile.service;


import com.dating.core.common.error.NotFoundException;
import com.dating.core.profile.api.dto.ProfileResponse;
import com.dating.core.profile.api.dto.UpdateProfileRequest;
import com.dating.core.profile.domain.Profile;
import com.dating.core.profile.domain.ProfileUpdate;
import com.dating.core.profile.repo.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Операции с анкетами пользователей.
 */
@Service
public class ProfileService {

    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    /** Возвращает профиль по id пользователя. */
    @Transactional(readOnly = true)
    public ProfileResponse getByUserId(UUID userId) {
        return toResponse(findByUserId(userId));
    }

    /** Возвращает профиль по его собственному id (для просмотра чужой анкеты). */
    @Transactional(readOnly = true)
    public ProfileResponse getById(UUID profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(()-> new NotFoundException("Profile not found"));
        return toResponse(profile);
    }

    /** Возвращает набор профилей по набору id (в данный момент нужен для связи с сервисом matches по gRPC)*/
    @Transactional(readOnly = true)
    public List<ProfileResponse> getProfilesBatch(List<UUID> profileIds) {
        return profileRepository.findAllById(profileIds).stream().map(this::toResponse).toList();
    }

    /** Обновляет анкету текущего пользователя. */
    @Transactional
    public ProfileResponse update(UUID userId, UpdateProfileRequest request) {
        Profile profile = findByUserId(userId);
        profile.update(new ProfileUpdate(request.displayName(),
                                        request.birthDate(),
                                        request.gender(),
                                        request.bio(),
                                        request.city()));
        return toResponse(profileRepository.save(profile));
    }



    private Profile findByUserId(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Профиль не найден"));
    }

    private ProfileResponse toResponse(Profile profile) {
        return new ProfileResponse(profile.getId(),
                                profile.getUserId(),
                                profile.getDisplayName(),
                                profile.getBirthDate(),
                                profile.getGender(),
                                profile.getBio(),
                                profile.getCity());
    }

}
