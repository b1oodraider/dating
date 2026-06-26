package com.dating.core.profile.service;

import com.dating.core.profile.domain.Profile;
import com.dating.core.profile.repo.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
class ProfileCreatorImpl implements ProfileCreator {
    private final ProfileRepository profileRepository;

    public ProfileCreatorImpl(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    @Transactional
    public void createInitialProfile(UUID userId, String displayName) {
        profileRepository.save(new Profile(userId, displayName));
    }

}

