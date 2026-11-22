package org.chengyuan.cyproject.util;

import org.chengyuan.cyproject.dto.common.AuthorSummary;
import org.chengyuan.cyproject.dto.common.PostDto;
import org.chengyuan.cyproject.entity.UserEntity;
import org.chengyuan.cyproject.entity.WorkEntity;
import org.chengyuan.cyproject.error.ErrorCode;
import org.chengyuan.cyproject.error.errors.BizException;
import org.chengyuan.cyproject.repository.LikelistRepository;
import org.chengyuan.cyproject.repository.TagRepository;
import org.chengyuan.cyproject.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 负责把 WorkEntity 组装成 PostDto，收敛原本散落在多个 Service 中的重复代码。
 */
@Component
public class PostAssembler {

    private final UserRepository userRepository;

    private final TagRepository tagRepository;

    private final LikelistRepository likelistRepository;

    public PostAssembler(UserRepository userRepository,
                         LikelistRepository likelistRepository,
                         TagRepository tagRepository) {
        this.userRepository = userRepository;
        this.likelistRepository = likelistRepository;
        this.tagRepository = tagRepository;
    }

    /**
     * @param work        帖子实体
     * @param currentUser 当前登录用户 id（用于计算 likedByMe）
     * @param detail      是否返回详情字段（content / updatedAt）
     */
    public PostDto build(WorkEntity work, String currentUser, boolean detail) {
        String userid = work.getUserid();
        UserEntity authorEntity = userRepository.findById(userid)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        AuthorSummary author = new AuthorSummary(
                authorEntity.getId(), authorEntity.getUsername(), authorEntity.getUsername(), authorEntity.getAvatar());

        String workid = work.getId();
        String tags = tagRepository.findById(workid).map(t -> t.getTags()).orElse("");
        List<String> tagList = Arrays.stream(tags.split("#"))
                .filter(str -> !str.isEmpty())
                .toList();
        int likes = likelistRepository.countLikesByWork(workid);
        boolean likedByMe = currentUser != null && likelistRepository.existsByUserAndWork(currentUser, workid) != 0;
        return PostDto.builder()
                .id(workid)
                .title(work.getWorktopic())
                .excerpt(buildExcerpt(work.getBody()))
                .content(detail ? work.getBody() : null)
                .author(author)
                .category(work.getWorktype())
                .tags(tagList)
                .views(work.getViews() == null ? 0 : work.getViews())
                .likes(likes)
                .likedByMe(likedByMe)
                .publishedAt(TimeUtil.toInstant(work.getPublishtime()))
                .updatedAt(detail ? TimeUtil.toInstant(work.getUpdatetime()) : null)
                .build();
    }

    private String buildExcerpt(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        // 去掉 Markdown 换行便于列表预览，限制长度
        String singleLine = content.replaceAll("\\s+", " ").trim();
        if (singleLine.length() <= 120) {
            return singleLine;
        }
        return singleLine.substring(0, 120) + "…";
    }
}
