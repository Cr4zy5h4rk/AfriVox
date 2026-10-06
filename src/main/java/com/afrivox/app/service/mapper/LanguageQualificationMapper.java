package com.afrivox.app.service.mapper;

import com.afrivox.app.domain.Language;
import com.afrivox.app.domain.LanguageQualification;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.service.dto.LanguageDTO;
import com.afrivox.app.service.dto.LanguageQualificationDTO;
import com.afrivox.app.service.dto.UserProfileDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link LanguageQualification} and its DTO {@link LanguageQualificationDTO}.
 */
@Mapper(componentModel = "spring")
public interface LanguageQualificationMapper extends EntityMapper<LanguageQualificationDTO, LanguageQualification> {
    @Mapping(target = "profile", source = "profile", qualifiedByName = "userProfileDisplayName")
    @Mapping(target = "language", source = "language", qualifiedByName = "languageCode")
    LanguageQualificationDTO toDto(LanguageQualification s);

    @Named("userProfileDisplayName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "displayName", source = "displayName")
    UserProfileDTO toDtoUserProfileDisplayName(UserProfile userProfile);

    @Named("languageCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    LanguageDTO toDtoLanguageCode(Language language);
}
