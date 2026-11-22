package org.chengyuan.cyproject.service.impl;

import org.chengyuan.cyproject.dto.common.AuthorSummary;
import org.chengyuan.cyproject.dto.common.CommentDto;
import org.chengyuan.cyproject.dto.common.PageResponse;
import org.chengyuan.cyproject.dto.common.Pagination;
import org.chengyuan.cyproject.dto.common.PostDto;
import org.chengyuan.cyproject.dto.request.CreateCommentRequest;
import org.chengyuan.cyproject.dto.request.CreatePostRequest;
import org.chengyuan.cyproject.dto.response.LikeResponse;
import org.chengyuan.cyproject.entity.*;
import org.chengyuan.cyproject.entity.id.LikelistId;
import org.chengyuan.cyproject.error.ErrorCode;
import org.chengyuan.cyproject.error.errors.BizException;
import org.chengyuan.cyproject.repository.CommentRepository;
import org.chengyuan.cyproject.repository.LikelistRepository;
import org.chengyuan.cyproject.repository.TagRepository;
import org.chengyuan.cyproject.repository.UserRepository;
import org.chengyuan.cyproject.repository.WorkRepository;
import org.chengyuan.cyproject.service.PostService;
import org.chengyuan.cyproject.util.PostAssembler;
import org.chengyuan.cyproject.util.SearchUtil;
import org.chengyuan.cyproject.util.TimeUtil;
import org.chengyuan.cyproject.util.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PostServiceImpl implements PostService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final CommentRepository commentRepository;

    private final WorkRepository workRepository;

    private final UserRepository userRepository;

    private final LikelistRepository likelistRepository;

    private final TagRepository tagRepository;

    private final PostAssembler postAssembler;

    private final SearchUtil searchUtil;

    public PostServiceImpl(WorkRepository workRepository,
                           UserRepository userRepository,
                           LikelistRepository likelistRepository,
                           CommentRepository commentRepository,
                           TagRepository tagRepository,
                           PostAssembler postAssembler,
                           SearchUtil searchUtil) {
        this.workRepository = workRepository;
        this.userRepository = userRepository;
        this.likelistRepository = likelistRepository;
        this.commentRepository = commentRepository;
        this.tagRepository = tagRepository;
        this.postAssembler = postAssembler;
        this.searchUtil = searchUtil;
    }

    @Override
    public PageResponse<PostDto> getPosts(String q, String category, String sort, Integer page, Integer pageSize) {
        String currentUser = UserContext.getUserId();
        String normalizedCategory = trimToNull(category);
        String normalizedSort = trimToNull(sort);
        String normalizedQ = trimToNull(q);

        List<WorkEntity> workEntities;
        if (normalizedCategory != null) {
            workEntities = switch (normalizedSort == null ? "" : normalizedSort) {
                case "likes" -> workRepository.findByWorktypeAndLikes(normalizedCategory);
                case "views" -> workRepository.findByWorktypeAndViews(normalizedCategory);
                case "time" -> workRepository.findByWorktypeAndTime(normalizedCategory);
                default -> workRepository.findByWorktype(normalizedCategory);
            };
        } else {
            workEntities = workRepository.findAll();
        }

        if (normalizedQ != null) {
            workEntities = searchUtil.doSearch(workEntities, normalizedQ);
        }

        List<PostDto> postDtos = new ArrayList<>();
        for (WorkEntity work : workEntities) {
            postDtos.add(postAssembler.build(work, currentUser, false));
        }

        int currentPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int currentPageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : pageSize;
        List<PostDto> paged = paginate(postDtos, currentPage, currentPageSize);
        return new PageResponse<>(paged, Pagination.of(currentPage, currentPageSize, postDtos.size()));
    }

    @Override
    @Transactional
    public PostDto getPost(String workid) {
        WorkEntity work = workRepository.findById(workid)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        workRepository.incrementViews(workid);
        // 批量更新不会同步内存中的实体，这里手动 +1，保证返回的浏览数准确
        work.setViews((work.getViews() == null ? 0 : work.getViews()) + 1);
        return postAssembler.build(work, UserContext.getUserId(), true);
    }

    @Override
    @Transactional
    public PostDto createPost(CreatePostRequest request) {
        String title = request.getTitle();
        String content = request.getContent();
        String category = request.getCategory();
        List<String> tags = request.getTags();
        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST);
        }
        if (category == null || category.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST);
        }
        StringBuilder finalTag = new StringBuilder();
        if (tags != null) {
            for (String tag : tags) {
                if (tag != null && !tag.isBlank()) {
                    finalTag.append("#").append(tag);
                }
            }
        }
        String id = UUID.randomUUID().toString();
        String userid = currentUserId();
        LocalDateTime now = LocalDateTime.now();
        WorkEntity work = new WorkEntity();
        work.setId(id);
        work.setUserid(userid);
        work.setWorktopic(title);
        work.setWorktype(category);
        work.setBody(content);
        work.setViews(0);
        work.setPublishtime(now);
        work.setUpdatetime(now);
        workRepository.save(work);

        TagEntity tagEntity = new TagEntity();
        tagEntity.setId(id);
        tagEntity.setTags(finalTag.toString());
        tagRepository.save(tagEntity);

        return postAssembler.build(work, userid, true);
    }

    @Override
    @Transactional
    public LikeResponse likePost(String workid) {
        return doLike(workid, true);
    }

    @Override
    @Transactional
    public LikeResponse unlikePost(String workid) {
        return doLike(workid, false);
    }

    private LikeResponse doLike(String workid, boolean like) {
        workRepository.findById(workid)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        String userid = currentUserId();
        LikelistId id = new LikelistId(workid, userid);
        boolean alreadyLiked = likelistRepository.existsById(id);

        if (like && !alreadyLiked) {
            LikelistEntity entity = new LikelistEntity();
            entity.setId(id);
            likelistRepository.save(entity);
        } else if (!like && alreadyLiked) {
            LikelistEntity entity = new LikelistEntity();
            entity.setId(id);
            likelistRepository.delete(entity);
        }

        return new LikeResponse(like, likelistRepository.countLikesByWork(workid));
    }

    @Override
    public PageResponse<CommentDto> getComments(String postId) {
        workRepository.findById(postId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        List<CommentEntity> commentList = commentRepository.findByWorkidOrderByPublishtimeAsc(postId);
        List<CommentDto> commentDtos = new ArrayList<>();
        for (CommentEntity comment : commentList) {
            commentDtos.add(toCommentDto(comment));
        }
        return new PageResponse<>(commentDtos, Pagination.of(DEFAULT_PAGE, DEFAULT_PAGE_SIZE, commentDtos.size()));
    }

    @Override
    @Transactional
    public CommentDto createComment(String postId, CreateCommentRequest request) {
        String content = request.getContent();
        if (content == null || content.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST);
        }
        workRepository.findById(postId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        String userid = currentUserId();
        LocalDateTime now = LocalDateTime.now();
        CommentEntity comment = new CommentEntity();
        comment.setId(UUID.randomUUID().toString());
        comment.setWorkid(postId);
        comment.setUserid(userid);
        comment.setBody(content);
        comment.setPublishtime(now);
        commentRepository.save(comment);

        return toCommentDto(comment);
    }

    @Override
    @Transactional
    public void deletePost(String postId) {
        WorkEntity work = workRepository.findById(postId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        String userid = currentUserId();
        if (!userid.equals(work.getUserid())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        // 依次删除点赞、评论、标签，最后删除帖子本身，避免外键约束失败
        likelistRepository.deleteByWorkid(postId);
        commentRepository.deleteByWorkid(postId);
        tagRepository.findById(postId).ifPresent(tagRepository::delete);
        workRepository.delete(work);
    }

    private CommentDto toCommentDto(CommentEntity comment) {
        UserEntity user = userRepository.findById(comment.getUserid())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        return new CommentDto(
                comment.getId(),
                new AuthorSummary(user.getId(), user.getUsername(), user.getUsername(), user.getAvatar()),
                comment.getBody(),
                TimeUtil.toInstant(comment.getPublishtime()));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private <T> List<T> paginate(List<T> items, int page, int pageSize) {
        int fromIndex = Math.min((page - 1) * pageSize, items.size());
        int toIndex = Math.min(fromIndex + pageSize, items.size());
        if (fromIndex >= toIndex) {
            return List.of();
        }
        return new ArrayList<>(items.subList(fromIndex, toIndex));
    }

    private String currentUserId() {
        String userid = UserContext.getUserId();
        if (userid == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userid;
    }
}
