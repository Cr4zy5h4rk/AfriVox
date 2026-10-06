package com.afrivox.app.service.mapper;

import com.afrivox.app.domain.DatasetVersion;
import com.afrivox.app.domain.DownloadLog;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.service.dto.DatasetVersionDTO;
import com.afrivox.app.service.dto.DownloadLogDTO;
import com.afrivox.app.service.dto.UserProfileDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link DownloadLog} and its DTO {@link DownloadLogDTO}.
 */
@Mapper(componentModel = "spring")
public interface DownloadLogMapper extends EntityMapper<DownloadLogDTO, DownloadLog> {
    @Mapping(target = "datasetVersion", source = "datasetVersion", qualifiedByName = "datasetVersionLabel")
    @Mapping(target = "profile", source = "profile", qualifiedByName = "userProfileDisplayName")
    DownloadLogDTO toDto(DownloadLog s);

    @Named("datasetVersionLabel")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "label", source = "label")
    DatasetVersionDTO toDtoDatasetVersionLabel(DatasetVersion datasetVersion);

    @Named("userProfileDisplayName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "displayName", source = "displayName")
    UserProfileDTO toDtoUserProfileDisplayName(UserProfile userProfile);
}
