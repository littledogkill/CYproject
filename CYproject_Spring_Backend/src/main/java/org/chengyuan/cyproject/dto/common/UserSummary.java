package org.chengyuan.cyproject.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSummary {

    private String id;

    private String username;

    private String name;

    private String avatar;

    private String bio;

    private Boolean online;
}
