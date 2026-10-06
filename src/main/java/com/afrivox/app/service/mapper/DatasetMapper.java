package com.afrivox.app.service.mapper;

import com.afrivox.app.domain.Dataset;
import com.afrivox.app.domain.Language;
import com.afrivox.app.service.dto.DatasetDTO;
import com.afrivox.app.service.dto.LanguageDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Dataset} and its DTO {@link DatasetDTO}.
 */
@Mapper(componentModel = "spring")
public interface DatasetMapper extends EntityMapper<DatasetDTO, Dataset> {
    @Mapping(target = "language", source = "language", qualifiedByName = "languageCode")
    DatasetDTO toDto(Dataset s);

    @Named("languageCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    LanguageDTO toDtoLanguageCode(Language language);
}
