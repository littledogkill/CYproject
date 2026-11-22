package org.chengyuan.cyproject.repository;

import org.chengyuan.cyproject.entity.WorkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkRepository extends JpaRepository<WorkEntity, String> {

    @Query("select count(w) from WorkEntity w where w.userid = :userid")
    int countByUserId(@Param("userid") String userid);

    List<WorkEntity> findByUserid(String userid);

    List<WorkEntity> findByWorktype(String worktopic);

    @Query("""
        select w from WorkEntity w
        left join LikelistEntity l on l.id.workid = w.id
        where w.worktype = :worktype
        group by w
        order by count(l) desc
        """)
    List<WorkEntity> findByWorktypeAndLikes(@Param("worktype") String worktype);

    @Query("select w from WorkEntity w where w.worktype = :worktype order by w.views desc")
    List<WorkEntity> findByWorktypeAndViews(@Param("worktype") String worktype);

    @Query("select w from WorkEntity w where w.worktype = :worktype order by w.publishtime desc")
    List<WorkEntity> findByWorktypeAndTime(@Param("worktype") String worktype);

    @Modifying
    @Query("update WorkEntity w set w.views = w.views + 1 where w.id = :id")
    int incrementViews(@Param("id") String id);
}
