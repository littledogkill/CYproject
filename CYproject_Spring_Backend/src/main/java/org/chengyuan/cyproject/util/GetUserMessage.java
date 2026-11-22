package org.chengyuan.cyproject.util;

import org.chengyuan.cyproject.dto.common.UserDto;
import org.chengyuan.cyproject.dto.common.UserStats;
import org.chengyuan.cyproject.entity.UserEntity;
import org.chengyuan.cyproject.repository.FollowlistRepository;
import org.chengyuan.cyproject.repository.LikelistRepository;
import org.chengyuan.cyproject.repository.WorkRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 负责用户资料 DTO 的构建与 Redis 缓存。
 */
@Component
public class GetUserMessage {

    private static final String TOKEN_PREFIX = "user:Token:";

    private static final String USER_DTO_PREFIX = "user:UserDto:";

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper mapper = new ObjectMapper();

    private final WorkRepository workRepository;

    private final FollowlistRepository followlistRepository;

    private final LikelistRepository likelistRepository;

    public GetUserMessage(WorkRepository workRepository, FollowlistRepository followlistRepository,
                          LikelistRepository likelistRepository, StringRedisTemplate stringRedisTemplate) {
        this.workRepository = workRepository;
        this.followlistRepository = followlistRepository;
        this.likelistRepository = likelistRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 缓存用户资料与 token。ttlSeconds 必须不大于 JWT 自身有效期，
     * 否则缓存里的 token 会比 JWT 更晚过期，登录会返回已过期 token。
     */
    public void cacheUser(String userid, UserDto userDto, String token, long ttlSeconds) {
        String userDtoString = mapper.writeValueAsString(userDto);
        long ttl = Math.max(ttlSeconds, 1);
        stringRedisTemplate.opsForValue().set(USER_DTO_PREFIX + userid, userDtoString, ttl, TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(TOKEN_PREFIX + userid, token, ttl, TimeUnit.SECONDS);
    }

    public Optional<UserDto> getRedisUserDto(String userid) {
        String userDtoString = stringRedisTemplate.opsForValue().get(USER_DTO_PREFIX + userid);
        if (userDtoString == null) {
            return Optional.empty();
        }
        UserDto userDto = mapper.readValue(userDtoString, UserDto.class);
        return Optional.ofNullable(userDto);
    }

    public String getToken(String userid) {
        return stringRedisTemplate.opsForValue().get(TOKEN_PREFIX + userid);
    }

    /**
     * 根据实体构建完整用户 DTO。
     *
     * @param userEntity 用户实体
     * @param userid     用户 id（用于统计与在线状态判断）
     * @param withFollow 是否计算 isFollowing 字段（仅查看他人主页时需要）
     * @param ifFollow   当前用户是否关注了该用户
     */
    public UserDto buildUserDto(UserEntity userEntity, String userid, boolean withFollow, boolean ifFollow) {
        Boolean follow = withFollow ? ifFollow : null;
        Integer likeCount = likelistRepository.countLikesByUser(userid);
        Integer followCount = followlistRepository.countFollowers(userid);
        Integer workCount = workRepository.countByUserId(userid);
        UserStats userStats = new UserStats(workCount, likeCount, followCount);

        return UserDto.builder()
                .id(userid)
                .username(userEntity.getUsername())
                .name(userEntity.getUsername())
                .handle(userEntity.getUsername())
                .email(userEntity.getEmail())
                .avatar(userEntity.getAvatar())
                .bio(userEntity.getBio())
                .online(getToken(userid) != null)
                .stats(userStats)
                .isFollowing(follow)
                .build();
    }
}
