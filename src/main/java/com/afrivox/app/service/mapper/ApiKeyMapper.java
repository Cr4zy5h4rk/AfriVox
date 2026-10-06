package com.afrivox.app.service.mapper;

import com.afrivox.app.domain.ApiKey;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.service.dto.ApiKeyDTO;
import com.afrivox.app.service.dto.UserProfileDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ApiKey} and its DTO {@link ApiKeyDTO}.
 */
@Mapper(componentModel = "spring")
public interface ApiKeyMapper extends EntityMapper<ApiKeyDTO, ApiKey> {
    @Mapping(target = "owner", source = "owner", qualifiedByName = "userProfileDisplayName")
    ApiKeyDTO toDto(ApiKey s);

    @Named("userProfileDisplayName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "displayName", source = "displayName")
    UserProfileDTO toDtoUserProfileDisplayName(UserProfile userProfile);
}
