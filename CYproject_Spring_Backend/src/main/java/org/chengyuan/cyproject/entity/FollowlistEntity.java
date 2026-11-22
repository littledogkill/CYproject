package org.chengyuan.cyproject.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.chengyuan.cyproject.entity.id.FollowlistId;

@Data
@NoArgsConstructor
@Entity
@Table(name = "followlist")
public class FollowlistEntity {

    @EmbeddedId
    private FollowlistId id;
}
