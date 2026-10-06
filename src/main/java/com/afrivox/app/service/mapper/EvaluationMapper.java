package com.afrivox.app.service.mapper;

import com.afrivox.app.domain.DatasetVersion;
import com.afrivox.app.domain.Evaluation;
import com.afrivox.app.domain.MlModel;
import com.afrivox.app.service.dto.DatasetVersionDTO;
import com.afrivox.app.service.dto.EvaluationDTO;
import com.afrivox.app.service.dto.MlModelDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Evaluation} and its DTO {@link EvaluationDTO}.
 */
@Mapper(componentModel = "spring")
public interface EvaluationMapper extends EntityMapper<EvaluationDTO, Evaluation> {
    @Mapping(target = "model", source = "model", qualifiedByName = "mlModelName")
    @Mapping(target = "testSet", source = "testSet", qualifiedByName = "datasetVersionLabel")
    EvaluationDTO toDto(Evaluation s);

    @Named("mlModelName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    MlModelDTO toDtoMlModelName(MlModel mlModel);

    @Named("datasetVersionLabel")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "label", source = "label")
    DatasetVersionDTO toDtoDatasetVersionLabel(DatasetVersion datasetVersion);
}
