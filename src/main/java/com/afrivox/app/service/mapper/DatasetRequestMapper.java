package com.afrivox.app.service.mapper;

import com.afrivox.app.domain.DatasetRequest;
import com.afrivox.app.domain.Language;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.service.dto.DatasetRequestDTO;
import com.afrivox.app.service.dto.LanguageDTO;
import com.afrivox.app.service.dto.UserProfileDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link DatasetRequest} and its DTO {@link DatasetRequestDTO}.
 */
@Mapper(componentModel = "spring")
public interface DatasetRequestMapper extends EntityMapper<DatasetRequestDTO, DatasetRequest> {
    @Mapping(target = "language", source = "language", qualifiedByName = "languageCode")
    @Mapping(target = "requester", source = "requester", qualifiedByName = "userProfileDisplayName")
    DatasetRequestDTO toDto(DatasetRequest s);

    @Named("languageCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    LanguageDTO toDtoLanguageCode(Language language);

    @Named("userProfileDisplayName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "displayName", source = "displayName")
    UserProfileDTO toDtoUserProfileDisplayName(UserProfile userProfile);
}
