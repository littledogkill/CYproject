package org.chengyuan.cyproject.repository;

import org.chengyuan.cyproject.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConversationRepository extends JpaRepository<ConversationEntity, String> {

    @Query(value = """
            SELECT t.id, t.senderid, t.`receiver`, t.publishtime, t.body
            FROM (
                SELECT c.*,
                       CASE WHEN c.senderid = :userId THEN c.`receiver` ELSE c.senderid END AS peerId,
                       ROW_NUMBER() OVER (
                           PARTITION BY CASE WHEN c.senderid = :userId THEN c.`receiver` ELSE c.senderid END
                           ORDER BY c.publishtime DESC, c.id DESC
                       ) AS rn
                FROM conversationlist c
                WHERE c.senderid = :userId OR c.`receiver` = :userId
            ) t
            WHERE t.rn = 1
            ORDER BY t.publishtime DESC
            """, nativeQuery = true)
    List<ConversationEntity> findLatestPerPeer(@Param("userId") String userId);

    /** 查询两个用户之间的全部消息，按时间正序返回 */
    @Query("select f from ConversationEntity f " +
            "where (f.senderid = :userId or f.receiver = :userId) " +
            "and (f.senderid = :peerId or f.receiver = :peerId) " +
            "order by f.publishtime asc, f.id asc")
    List<ConversationEntity> findMessagesBetween(@Param("userId") String userId, @Param("peerId") String peerId);
}
