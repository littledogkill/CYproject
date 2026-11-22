package org.chengyuan.cyproject.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostDto {

    private String id;

    private String title;

    private String excerpt;

    private String content;// 仅详情有

    private AuthorSummary author;

    private String category;// work / ask

    private List<String> tags;

    private Integer views;

    private Integer likes;

    private Boolean likedByMe;

    private Instant publishedAt;

    private Instant updatedAt;     // 仅详情有
}
