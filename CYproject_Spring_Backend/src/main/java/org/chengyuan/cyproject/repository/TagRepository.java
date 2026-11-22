package org.chengyuan.cyproject.repository;

import org.chengyuan.cyproject.entity.TagEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<TagEntity, String> {
    Optional<TagEntity> findById(String id);
}
