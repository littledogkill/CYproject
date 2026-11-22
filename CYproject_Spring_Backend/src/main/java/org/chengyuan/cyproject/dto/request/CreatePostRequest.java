package org.chengyuan.cyproject.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostRequest {

    private String title;

    private String category;

    private List<String> tags;

    private String content;
}