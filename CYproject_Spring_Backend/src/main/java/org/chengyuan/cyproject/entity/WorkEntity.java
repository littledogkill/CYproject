package org.chengyuan.cyproject.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "work")
public class WorkEntity {

    @Id
    @Column(name = "id", length = 255)
    private String id;

    @Column(name = "userid", length = 255)
    private String userid;

    @Column(name = "worktopic", length = 255)
    private String worktopic;

    @Column(name = "worktype", length = 255)
    private String worktype;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "views")
    private Integer views;

    @Column(name = "publishtime")
    private LocalDateTime publishtime;

    @Column(name = "updatetime")
    private LocalDateTime updatetime;
}
