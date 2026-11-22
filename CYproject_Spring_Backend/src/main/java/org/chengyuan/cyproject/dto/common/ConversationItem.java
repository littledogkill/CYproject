package org.chengyuan.cyproject.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationItem {

    private String id;

    private String username;

    private String name;

    private String avatar;

    private Boolean online;

    private String lastMessage;

    private Instant lastMessageAt;

    private Integer unreadCount;
}