package com.afrivox.app.service.mapper;

import com.afrivox.app.domain.Recording;
import com.afrivox.app.domain.RecordingReview;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.service.dto.RecordingDTO;
import com.afrivox.app.service.dto.RecordingReviewDTO;
import com.afrivox.app.service.dto.UserProfileDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link RecordingReview} and its DTO {@link RecordingReviewDTO}.
 */
@Mapper(componentModel = "spring")
public interface RecordingReviewMapper extends EntityMapper<RecordingReviewDTO, RecordingReview> {
    @Mapping(target = "recording", source = "recording", qualifiedByName = "recordingId")
    @Mapping(target = "reviewer", source = "reviewer", qualifiedByName = "userProfileDisplayName")
    RecordingReviewDTO toDto(RecordingReview s);

    @Named("recordingId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    RecordingDTO toDtoRecordingId(Recording recording);

    @Named("userProfileDisplayName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "displayName", source = "displayName")
    UserProfileDTO toDtoUserProfileDisplayName(UserProfile userProfile);
}
