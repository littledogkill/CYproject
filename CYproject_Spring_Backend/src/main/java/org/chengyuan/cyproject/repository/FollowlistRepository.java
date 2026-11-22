package org.chengyuan.cyproject.repository;

import org.chengyuan.cyproject.entity.FollowlistEntity;
import org.chengyuan.cyproject.entity.id.FollowlistId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FollowlistRepository extends JpaRepository<FollowlistEntity, FollowlistId> {

    @Query("select count(f) from FollowlistEntity f where f.id.followeeid = :userid")
    Integer countFollowers(@Param("userid") String userid);

    @Query("select count(f) from FollowlistEntity f where f.id.followerid = :followerid and f.id.followeeid = :followeeid")
    int existsFollow(@Param("followerid") String followerid, @Param("followeeid") String followeeid);

    @Query("select f from FollowlistEntity f where f.id.followerid = :userid")
    List<FollowlistEntity> findByFollowerId(@Param("userid") String userid);

    @Query("select f from FollowlistEntity f where f.id.followeeid = :userid")
    List<FollowlistEntity> findByFolloweeId(@Param("userid") String userid);
}
