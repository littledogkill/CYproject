package org.chengyuan.cyproject.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowResponse {

    private Boolean following;

    private Integer followers;
}
