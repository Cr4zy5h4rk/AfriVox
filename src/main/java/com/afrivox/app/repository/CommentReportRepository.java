package com.afrivox.app.repository;

import com.afrivox.app.domain.CommentReport;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {
    boolean existsByCommentIdAndReporterId(Long commentId, Long reporterId);

    /** Signalements non traites, du plus recent au plus ancien. */
    @Query("select r from CommentReport r join fetch r.reporter where r.resolved = false order by r.createdAt desc")
    List<CommentReport> findOpen();

    List<CommentReport> findAllByCommentIdInAndResolvedFalse(Collection<Long> commentIds);

    long countByResolvedFalse();
}
