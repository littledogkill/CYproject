package org.chengyuan.cyproject.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)   // isFollowing 为 null 时不输出
public class UserDto {

    private String id;

    private String username;

    private String name;

    private String handle;

    private String email;

    private String avatar;

    private String bio;

    private Boolean online;

    private UserStats stats;

    private Boolean isFollowing;   // 仅 /users/{username} 有值
}
