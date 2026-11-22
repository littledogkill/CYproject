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
public class LikelistId implements Serializable {

    @Column(name = "workid", length = 255)
    private String workid;

    @Column(name = "userid", length = 255)
    private String userid;
}
