package org.chengyuan.cyproject.service;

import org.chengyuan.cyproject.dto.common.PageResponse;
import org.chengyuan.cyproject.dto.common.PostDto;
import org.chengyuan.cyproject.dto.common.UserDto;
import org.chengyuan.cyproject.dto.common.UserSummary;
import org.chengyuan.cyproject.dto.request.UpdateUserRequest;
import org.chengyuan.cyproject.dto.response.FollowResponse;

public interface UserService {

    UserDto getCurrentUser();

    UserDto updateCurrentUser(UpdateUserRequest request);

    UserDto getUser(String userid);

    PageResponse<PostDto> getUserPosts(String userid);

    PageResponse<PostDto> getLikedPosts();

    PageResponse<UserSummary> getFollowing();

    PageResponse<UserSummary> getFollowers();

    FollowResponse follow(String userid);

    FollowResponse unfollow(String userid);
}
