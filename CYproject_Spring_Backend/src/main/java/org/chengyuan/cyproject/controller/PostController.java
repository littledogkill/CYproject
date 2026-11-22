package org.chengyuan.cyproject.controller;

import org.chengyuan.cyproject.dto.common.CommentDto;
import org.chengyuan.cyproject.dto.common.PageResponse;
import org.chengyuan.cyproject.dto.common.PostDto;
import org.chengyuan.cyproject.dto.request.CreateCommentRequest;
import org.chengyuan.cyproject.dto.request.CreatePostRequest;
import org.chengyuan.cyproject.dto.response.LikeResponse;
import org.chengyuan.cyproject.dto.response.Response;
import org.chengyuan.cyproject.service.PostService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public Response<PageResponse<PostDto>> getPosts(@RequestParam(defaultValue = "") String q,
                                                    @RequestParam(defaultValue = "") String category,
                                                    @RequestParam(defaultValue = "") String sort,
                                                    @RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "50") Integer pageSize) {
        return Response.ok(postService.getPosts(q, category, sort, page, pageSize));
    }

    @GetMapping("/{postId}")
    public Response<PostDto> getPost(@PathVariable String postId) {
        return Response.ok(postService.getPost(postId));
    }

    @PostMapping
    public Response<PostDto> createPost(@RequestBody CreatePostRequest request) {
        return Response.ok(postService.createPost(request));
    }

    @PutMapping("/{postId}/like")
    public Response<LikeResponse> likePost(@PathVariable String postId) {
        return Response.ok(postService.likePost(postId));
    }

    @DeleteMapping("/{postId}/like")
    public Response<LikeResponse> unlikePost(@PathVariable String postId) {
        return Response.ok(postService.unlikePost(postId));
    }

    @GetMapping("/{postId}/comments")
    public Response<PageResponse<CommentDto>> getComments(@PathVariable String postId) {
        return Response.ok(postService.getComments(postId));
    }

    @PostMapping("/{postId}/comments")
    public Response<CommentDto> createComment(@PathVariable String postId,
                                              @RequestBody CreateCommentRequest request) {
        return Response.ok(postService.createComment(postId, request));
    }

    @DeleteMapping("/{postId}")
    public Response<Void> deletePost(@PathVariable String postId) {
        postService.deletePost(postId);
        return Response.ok(null);
    }
}
