package org.chengyuan.cyproject.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.chengyuan.cyproject.dto.common.UserDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UserDto user;
}