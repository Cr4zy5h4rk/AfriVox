package com.afrivox.app.service.mapper;

import com.afrivox.app.domain.Campaign;
import com.afrivox.app.domain.DatasetRequest;
import com.afrivox.app.domain.Language;
import com.afrivox.app.service.dto.CampaignDTO;
import com.afrivox.app.service.dto.DatasetRequestDTO;
import com.afrivox.app.service.dto.LanguageDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Campaign} and its DTO {@link CampaignDTO}.
 */
@Mapper(componentModel = "spring")
public interface CampaignMapper extends EntityMapper<CampaignDTO, Campaign> {
    @Mapping(target = "language", source = "language", qualifiedByName = "languageCode")
    @Mapping(target = "request", source = "request", qualifiedByName = "datasetRequestId")
    CampaignDTO toDto(Campaign s);

    @Named("languageCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    LanguageDTO toDtoLanguageCode(Language language);

    @Named("datasetRequestId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    DatasetRequestDTO toDtoDatasetRequestId(DatasetRequest datasetRequest);
}
