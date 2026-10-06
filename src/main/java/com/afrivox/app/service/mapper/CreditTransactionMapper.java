package com.afrivox.app.service.mapper;

import com.afrivox.app.domain.CreditTransaction;
import com.afrivox.app.domain.DatasetVersion;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.service.dto.CreditTransactionDTO;
import com.afrivox.app.service.dto.DatasetVersionDTO;
import com.afrivox.app.service.dto.UserProfileDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link CreditTransaction} and its DTO {@link CreditTransactionDTO}.
 */
@Mapper(componentModel = "spring")
public interface CreditTransactionMapper extends EntityMapper<CreditTransactionDTO, CreditTransaction> {
    @Mapping(target = "profile", source = "profile", qualifiedByName = "userProfileDisplayName")
    @Mapping(target = "datasetVersion", source = "datasetVersion", qualifiedByName = "datasetVersionLabel")
    CreditTransactionDTO toDto(CreditTransaction s);

    @Named("userProfileDisplayName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "displayName", source = "displayName")
    UserProfileDTO toDtoUserProfileDisplayName(UserProfile userProfile);

    @Named("datasetVersionLabel")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "label", source = "label")
    DatasetVersionDTO toDtoDatasetVersionLabel(DatasetVersion datasetVersion);
}
