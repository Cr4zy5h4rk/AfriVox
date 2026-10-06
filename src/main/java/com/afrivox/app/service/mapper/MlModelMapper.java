package com.afrivox.app.service.mapper;

import com.afrivox.app.domain.DatasetVersion;
import com.afrivox.app.domain.Language;
import com.afrivox.app.domain.MlModel;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.service.dto.DatasetVersionDTO;
import com.afrivox.app.service.dto.LanguageDTO;
import com.afrivox.app.service.dto.MlModelDTO;
import com.afrivox.app.service.dto.UserProfileDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link MlModel} and its DTO {@link MlModelDTO}.
 */
@Mapper(componentModel = "spring")
public interface MlModelMapper extends EntityMapper<MlModelDTO, MlModel> {
    @Mapping(target = "owner", source = "owner", qualifiedByName = "userProfileDisplayName")
    @Mapping(target = "language", source = "language", qualifiedByName = "languageCode")
    @Mapping(target = "trainedOn", source = "trainedOn", qualifiedByName = "datasetVersionLabel")
    MlModelDTO toDto(MlModel s);

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

    @Named("datasetVersionLabel")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "label", source = "label")
    DatasetVersionDTO toDtoDatasetVersionLabel(DatasetVersion datasetVersion);
}
