package org.chengyuan.cyproject.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {

    private String id;

    private String senderUsername;

    private String recipientUsername;

    private String content;

    private Instant createdAt;
}