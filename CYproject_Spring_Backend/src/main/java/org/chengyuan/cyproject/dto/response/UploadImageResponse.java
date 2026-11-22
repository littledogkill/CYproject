package org.chengyuan.cyproject.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadImageResponse {

    private String url;

    private Integer width;

    private Integer height;

    private String mime;

    private Long size;
}
