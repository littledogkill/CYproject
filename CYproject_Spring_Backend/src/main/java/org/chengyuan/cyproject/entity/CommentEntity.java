package org.chengyuan.cyproject.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@Table(name = "commentlist")
public class CommentEntity {
    @Id
    @Column(name = "id", length = 255)
    private String id;

    @Column(name = "workid",length = 255)
    private String workid;

    @Column(name = "userid",length = 255)
    private String userid;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "publishtime")
    private LocalDateTime publishtime;
}
