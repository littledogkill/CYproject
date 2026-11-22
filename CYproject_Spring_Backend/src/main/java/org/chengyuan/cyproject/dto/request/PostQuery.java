package org.chengyuan.cyproject.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostQuery {

    private String q;

    private String category;

    private String sort;

    private Integer page;

    private Integer pageSize;
}
