package com.afrivox.app.service.mapper;

import com.afrivox.app.domain.Dataset;
import com.afrivox.app.domain.DatasetVersion;
import com.afrivox.app.domain.Recording;
import com.afrivox.app.service.dto.DatasetDTO;
import com.afrivox.app.service.dto.DatasetVersionDTO;
import com.afrivox.app.service.dto.RecordingDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link DatasetVersion} and its DTO {@link DatasetVersionDTO}.
 */
@Mapper(componentModel = "spring")
public interface DatasetVersionMapper extends EntityMapper<DatasetVersionDTO, DatasetVersion> {
    @Mapping(target = "dataset", source = "dataset", qualifiedByName = "datasetSlug")
    @Mapping(target = "recordings", source = "recordings", qualifiedByName = "recordingIdSet")
    DatasetVersionDTO toDto(DatasetVersion s);

    @Mapping(target = "removeRecording", ignore = true)
    DatasetVersion toEntity(DatasetVersionDTO datasetVersionDTO);

    @Named("datasetSlug")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "slug", source = "slug")
    DatasetDTO toDtoDatasetSlug(Dataset dataset);

    @Named("recordingId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    RecordingDTO toDtoRecordingId(Recording recording);

    @Named("recordingIdSet")
    default Set<RecordingDTO> toDtoRecordingIdSet(Set<Recording> recording) {
        return recording.stream().map(this::toDtoRecordingId).collect(Collectors.toSet());
    }
}
