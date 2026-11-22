package org.chengyuan.cyproject.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.chengyuan.cyproject.entity.id.LikelistId;
import org.springframework.data.jpa.repository.Query;

@Data
@NoArgsConstructor
@Entity
@Table(name = "likelist")
public class LikelistEntity {

    @EmbeddedId
    private LikelistId id;
}
