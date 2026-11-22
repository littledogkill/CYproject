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
@Table(name = "conversationlist")
public class ConversationEntity {
    @Id
    @Column(name = "id", length = 255)
    private String id;

    @Column(name = "senderid", length = 255)
    private String senderid;

    @Column(name = "receiver", length = 255)
    private String receiver;

    @Column(name = "body", columnDefinition = "Text")
    private String body;

    @Column(name = "publishtime")
    private LocalDateTime publishtime;
}
