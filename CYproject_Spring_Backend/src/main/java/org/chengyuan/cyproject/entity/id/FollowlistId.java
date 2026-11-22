package org.chengyuan.cyproject.entity.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class FollowlistId implements Serializable {

    @Column(name = "followeeid", length = 255)
    private String followeeid;

    @Column(name = "followerid",length = 255)
    private String followerid;
}
