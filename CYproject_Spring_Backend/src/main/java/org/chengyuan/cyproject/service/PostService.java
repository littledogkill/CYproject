package org.chengyuan.cyproject.service;

import org.chengyuan.cyproject.dto.common.CommentDto;
import org.chengyuan.cyproject.dto.common.PageResponse;
import org.chengyuan.cyproject.dto.common.PostDto;
import org.chengyuan.cyproject.dto.request.CreateCommentRequest;
import org.chengyuan.cyproject.dto.request.CreatePostRequest;
import org.chengyuan.cyproject.dto.response.LikeResponse;

public interface PostService {

    PageResponse<PostDto> getPosts(String q, String category, String sort, Integer page, Integer pageSize);

    PostDto getPost(String postId);

    PostDto createPost(CreatePostRequest request);

    LikeResponse likePost(String workid);

    LikeResponse unlikePost(String workid);

    PageResponse<CommentDto> getComments(String postId);

    CommentDto createComment(String postId, CreateCommentRequest request);

    void deletePost(String postId);
}
