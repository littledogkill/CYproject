package org.chengyuan.cyproject.repository;

import org.chengyuan.cyproject.entity.LikelistEntity;
import org.chengyuan.cyproject.entity.id.LikelistId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LikelistRepository extends JpaRepository<LikelistEntity, LikelistId> {

    @Query("select count(l) from LikelistEntity l where l.id.workid in (select w.id from WorkEntity w where w.userid = :userid)")
    Integer countLikesByUser(@Param("userid") String userid);

    @Query("select count(l) from LikelistEntity l where l.id.workid = :workid")
    Integer countLikesByWork(@Param("workid") String workid);

    @Query("select count(l) from LikelistEntity l where l.id.userid = :userid and l.id.workid = :workid")
    int existsByUserAndWork(@Param("userid") String userid, @Param("workid") String workid);

    @Query("select l from LikelistEntity l where l.id.userid = :userid")
    List<LikelistEntity> findByUserId(@Param("userid") String userid);

    @Modifying
    @Query("delete from LikelistEntity l where l.id.workid = :workid")
    void deleteByWorkid(@Param("workid") String workid);
}
