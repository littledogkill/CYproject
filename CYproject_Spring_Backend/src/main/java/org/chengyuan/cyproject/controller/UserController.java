package org.chengyuan.cyproject.controller;

import org.chengyuan.cyproject.dto.common.PageResponse;
import org.chengyuan.cyproject.dto.common.PostDto;
import org.chengyuan.cyproject.dto.common.UserDto;
import org.chengyuan.cyproject.dto.common.UserSummary;
import org.chengyuan.cyproject.dto.request.UpdateUserRequest;
import org.chengyuan.cyproject.dto.response.FollowResponse;
import org.chengyuan.cyproject.dto.response.Response;
import org.chengyuan.cyproject.service.UserService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public Response<UserDto> getCurrentUser() {
        return Response.ok(userService.getCurrentUser());
    }

    @PatchMapping("/me")
    public Response<UserDto> updateCurrentUser(@RequestBody UpdateUserRequest request) {
        return Response.ok(userService.updateCurrentUser(request));
    }

    @GetMapping("/{userid}")
    public Response<UserDto> getUser(@PathVariable String userid) {
        return Response.ok(userService.getUser(userid));
    }

    @GetMapping("/{userid}/posts")
    public Response<PageResponse<PostDto>> getUserPosts(@PathVariable String userid) {
        return Response.ok(userService.getUserPosts(userid));
    }

    @GetMapping("/me/likes")
    public Response<PageResponse<PostDto>> getLikedPosts() {
        return Response.ok(userService.getLikedPosts());
    }

    @GetMapping("/me/following")
    public Response<PageResponse<UserSummary>> getFollowing() {
        return Response.ok(userService.getFollowing());
    }

    @GetMapping("/me/followers")
    public Response<PageResponse<UserSummary>> getFollowers() {
        return Response.ok(userService.getFollowers());
    }

    @PutMapping("/{userid}/follow")
    public Response<FollowResponse> follow(@PathVariable String userid) {
        return Response.ok(userService.follow(userid));
    }

    @DeleteMapping("/{userid}/follow")
    public Response<FollowResponse> unfollow(@PathVariable String userid) {
        return Response.ok(userService.unfollow(userid));
    }
}
