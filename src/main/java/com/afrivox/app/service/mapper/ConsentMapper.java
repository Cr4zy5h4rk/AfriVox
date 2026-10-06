package com.afrivox.app.service.mapper;

import com.afrivox.app.domain.Consent;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.service.dto.ConsentDTO;
import com.afrivox.app.service.dto.UserProfileDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Consent} and its DTO {@link ConsentDTO}.
 */
@Mapper(componentModel = "spring")
public interface ConsentMapper extends EntityMapper<ConsentDTO, Consent> {
    @Mapping(target = "profile", source = "profile", qualifiedByName = "userProfileDisplayName")
    ConsentDTO toDto(Consent s);

    @Named("userProfileDisplayName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "displayName", source = "displayName")
    UserProfileDTO toDtoUserProfileDisplayName(UserProfile userProfile);
}
