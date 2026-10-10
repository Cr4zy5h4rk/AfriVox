package com.afrivox.app.repository;

import com.afrivox.app.domain.TestReference;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TestReferenceRepository extends JpaRepository<TestReference, Long> {
    List<TestReference> findAllByVersionIdOrderByRecordingId(Long versionId);

    long countByVersionId(Long versionId);

    /** Versions ayant un jeu de test : { id de version, nombre de phrases }. */
    @Query("select t.versionId, count(t) from TestReference t group by t.versionId")
    List<Object[]> countsByVersion();
}
