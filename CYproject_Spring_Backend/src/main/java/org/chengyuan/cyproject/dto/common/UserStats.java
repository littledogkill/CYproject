package org.chengyuan.cyproject.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStats {

    private Integer posts;

    private Integer likes;

    private Integer followers;
}