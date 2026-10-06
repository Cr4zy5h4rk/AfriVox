package com.afrivox.app.repository;

import com.afrivox.app.domain.DatasetVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface DatasetVersionRepositoryWithBagRelationships {
    Optional<DatasetVersion> fetchBagRelationships(Optional<DatasetVersion> datasetVersion);

    List<DatasetVersion> fetchBagRelationships(List<DatasetVersion> datasetVersions);

    Page<DatasetVersion> fetchBagRelationships(Page<DatasetVersion> datasetVersions);
}
