package com.afrivox.app.service.mapper;

import com.afrivox.app.domain.DatasetVersion;
import com.afrivox.app.domain.Prompt;
import com.afrivox.app.domain.Recording;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.service.dto.DatasetVersionDTO;
import com.afrivox.app.service.dto.PromptDTO;
import com.afrivox.app.service.dto.RecordingDTO;
import com.afrivox.app.service.dto.UserProfileDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Recording} and its DTO {@link RecordingDTO}.
 */
@Mapper(componentModel = "spring")
public interface RecordingMapper extends EntityMapper<RecordingDTO, Recording> {
    @Mapping(target = "prompt", source = "prompt", qualifiedByName = "promptId")
    @Mapping(target = "contributor", source = "contributor", qualifiedByName = "userProfileDisplayName")
    @Mapping(target = "datasetVersions", source = "datasetVersions", qualifiedByName = "datasetVersionIdSet")
    RecordingDTO toDto(Recording s);

    @Mapping(target = "datasetVersions", ignore = true)
    @Mapping(target = "removeDatasetVersion", ignore = true)
    Recording toEntity(RecordingDTO recordingDTO);

    @Named("promptId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    PromptDTO toDtoPromptId(Prompt prompt);

    @Named("userProfileDisplayName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "displayName", source = "displayName")
    UserProfileDTO toDtoUserProfileDisplayName(UserProfile userProfile);

    @Named("datasetVersionId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    DatasetVersionDTO toDtoDatasetVersionId(DatasetVersion datasetVersion);

    @Named("datasetVersionIdSet")
    default Set<DatasetVersionDTO> toDtoDatasetVersionIdSet(Set<DatasetVersion> datasetVersion) {
        return datasetVersion.stream().map(this::toDtoDatasetVersionId).collect(Collectors.toSet());
    }
}
