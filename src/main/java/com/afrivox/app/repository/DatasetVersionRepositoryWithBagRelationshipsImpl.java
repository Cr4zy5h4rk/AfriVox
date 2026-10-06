package com.afrivox.app.repository;

import com.afrivox.app.domain.DatasetVersion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

/**
 * Utility repository to load bag relationships based on https://vladmihalcea.com/hibernate-multiplebagfetchexception/
 */
public class DatasetVersionRepositoryWithBagRelationshipsImpl implements DatasetVersionRepositoryWithBagRelationships {

    private static final String ID_PARAMETER = "id";
    private static final String DATASETVERSIONS_PARAMETER = "datasetVersions";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<DatasetVersion> fetchBagRelationships(Optional<DatasetVersion> datasetVersion) {
        return datasetVersion.map(this::fetchRecordings);
    }

    @Override
    public Page<DatasetVersion> fetchBagRelationships(Page<DatasetVersion> datasetVersions) {
        return new PageImpl<>(
            fetchBagRelationships(datasetVersions.getContent()),
            datasetVersions.getPageable(),
            datasetVersions.getTotalElements()
        );
    }

    @Override
    public List<DatasetVersion> fetchBagRelationships(List<DatasetVersion> datasetVersions) {
        return Optional.of(datasetVersions).map(this::fetchRecordings).orElse(List.of());
    }

    DatasetVersion fetchRecordings(DatasetVersion result) {
        return entityManager
            .createQuery(
                "select datasetVersion from DatasetVersion datasetVersion left join fetch datasetVersion.recordings where datasetVersion.id = :id",
                DatasetVersion.class
            )
            .setParameter(ID_PARAMETER, result.getId())
            .getSingleResult();
    }

    List<DatasetVersion> fetchRecordings(List<DatasetVersion> datasetVersions) {
        HashMap<Object, Integer> order = new HashMap<>();
        IntStream.range(0, datasetVersions.size()).forEach(index -> order.put(datasetVersions.get(index).getId(), index));
        List<DatasetVersion> result = entityManager
            .createQuery(
                "select datasetVersion from DatasetVersion datasetVersion left join fetch datasetVersion.recordings where datasetVersion in :datasetVersions",
                DatasetVersion.class
            )
            .setParameter(DATASETVERSIONS_PARAMETER, datasetVersions)
            .getResultList();
        result.sort((o1, o2) -> Integer.compare(order.get(o1.getId()), order.get(o2.getId())));
        return result;
    }
}
