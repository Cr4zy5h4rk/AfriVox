package com.afrivox.app.repository;

import com.afrivox.app.domain.ContactRequest;
import com.afrivox.app.domain.enumeration.ContactStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRequestRepository extends JpaRepository<ContactRequest, Long> {
    @Query("select r from ContactRequest r join fetch r.sender where r.recipient.id = :id order by r.createdAt desc, r.id desc")
    List<ContactRequest> findReceived(@Param("id") Long recipientId);

    @Query("select r from ContactRequest r join fetch r.recipient where r.sender.id = :id order by r.createdAt desc, r.id desc")
    List<ContactRequest> findSent(@Param("id") Long senderId);

    /** Une seule demande en attente par couple expediteur / destinataire. */
    boolean existsBySenderIdAndRecipientIdAndStatus(Long senderId, Long recipientId, ContactStatus status);

    /** Pour limiter le rythme : demandes envoyees par une organisation depuis un instant donne. */
    long countBySenderIdAndCreatedAtAfter(Long senderId, Instant since);

    long countByRecipientIdAndStatus(Long recipientId, ContactStatus status);
}
