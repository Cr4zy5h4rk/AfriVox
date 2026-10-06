package com.afrivox.app.service.mapper;

import com.afrivox.app.domain.Campaign;
import com.afrivox.app.domain.Prompt;
import com.afrivox.app.service.dto.CampaignDTO;
import com.afrivox.app.service.dto.PromptDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Prompt} and its DTO {@link PromptDTO}.
 */
@Mapper(componentModel = "spring")
public interface PromptMapper extends EntityMapper<PromptDTO, Prompt> {
    @Mapping(target = "campaign", source = "campaign", qualifiedByName = "campaignTitle")
    PromptDTO toDto(Prompt s);

    @Named("campaignTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    CampaignDTO toDtoCampaignTitle(Campaign campaign);
}
