package org.chengyuan.cyproject.repository;


import org.chengyuan.cyproject.entity.CommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<CommentEntity, String> {
    List<CommentEntity> findByWorkid(String workid);

    List<CommentEntity> findByWorkidOrderByPublishtimeAsc(String workid);

    @Modifying
    @Query("delete from CommentEntity c where c.workid = :workid")
    void deleteByWorkid(@Param("workid") String workid);
 }
