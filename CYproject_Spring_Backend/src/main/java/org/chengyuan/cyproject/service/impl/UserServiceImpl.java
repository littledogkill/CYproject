package org.chengyuan.cyproject.service.impl;

import org.chengyuan.cyproject.dto.common.PageResponse;
import org.chengyuan.cyproject.dto.common.Pagination;
import org.chengyuan.cyproject.dto.common.PostDto;
import org.chengyuan.cyproject.dto.common.UserDto;
import org.chengyuan.cyproject.dto.common.UserSummary;
import org.chengyuan.cyproject.dto.request.UpdateUserRequest;
import org.chengyuan.cyproject.dto.response.FollowResponse;
import org.chengyuan.cyproject.entity.FollowlistEntity;
import org.chengyuan.cyproject.entity.LikelistEntity;
import org.chengyuan.cyproject.entity.UserEntity;
import org.chengyuan.cyproject.entity.WorkEntity;
import org.chengyuan.cyproject.entity.id.FollowlistId;
import org.chengyuan.cyproject.error.ErrorCode;
import org.chengyuan.cyproject.error.errors.BizException;
import org.chengyuan.cyproject.repository.FollowlistRepository;
import org.chengyuan.cyproject.repository.LikelistRepository;
import org.chengyuan.cyproject.repository.UserRepository;
import org.chengyuan.cyproject.repository.WorkRepository;
import org.chengyuan.cyproject.service.UserService;
import org.chengyuan.cyproject.util.GetUserMessage;
import org.chengyuan.cyproject.util.PostAssembler;
import org.chengyuan.cyproject.util.UserContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UserServiceImpl implements UserService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final int DEFAULT_PAGE = 1;

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final LikelistRepository likelistRepository;

    private final UserRepository userRepository;

    private final WorkRepository workRepository;

    private final FollowlistRepository followlistRepository;

    private final GetUserMessage getUserMessage;

    private final PostAssembler postAssembler;

    public UserServiceImpl(GetUserMessage getUserMessage,
                           UserRepository userRepository,
                           FollowlistRepository followlistRepository,
                           WorkRepository workRepository,
                           LikelistRepository likelistRepository,
                           PostAssembler postAssembler) {
        this.getUserMessage = getUserMessage;
        this.userRepository = userRepository;
        this.followlistRepository = followlistRepository;
        this.workRepository = workRepository;
        this.likelistRepository = likelistRepository;
        this.postAssembler = postAssembler;
    }

    @Override
    public UserDto getCurrentUser() {
        String userid = currentUserId();
        Optional<UserDto> cached = getUserMessage.getRedisUserDto(userid);
        if (cached.isPresent()) {
            UserDto userDto = cached.get();
            // 当前用户正在请求，必然在线
            userDto.setOnline(true);
            return userDto;
        }
        UserEntity userEntity = userRepository.findById(userid)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        UserDto userDto = getUserMessage.buildUserDto(userEntity, userid, false, false);
        userDto.setOnline(true);
        return userDto;
    }

    // 当前设计下暂未实现资料修改，保留接口占位
    @Override
    @Transactional
    public UserDto updateCurrentUser(UpdateUserRequest request) {
        String userid = currentUserId();
        String username = trimToNull(request.getUsername());
        String password = trimToNull(request.getPassword());
        try {
            UserEntity user = userRepository.findById(userid)
                    .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
            if (password != null) {

                user.setPassword(passwordEncoder.encode(password));
            }
            if (username != null) {
                user.setUsername(username);
            }
            userRepository.save(user);
            UserDto userDto = getUserMessage.buildUserDto(user, userid, false, false);
            userDto.setOnline(true);
            return userDto;
        } catch (Exception e){
            throw new BizException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public UserDto getUser(String userid) {
        String myuserid = currentUserId();
        UserEntity target = userRepository.findById(userid)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        boolean ifFollow = followlistRepository.existsFollow(myuserid, userid) != 0;
        return getUserMessage.buildUserDto(target, userid, true, ifFollow);
    }

    @Override
    public PageResponse<PostDto> getUserPosts(String userid) {
        userRepository.findById(userid)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        String myuserid = currentUserId();
        List<WorkEntity> workList = workRepository.findByUserid(userid);
        List<PostDto> postDtos = new ArrayList<>();
        for (WorkEntity work : workList) {
            postDtos.add(postAssembler.build(work, myuserid, false));
        }
        return new PageResponse<>(postDtos, Pagination.of(DEFAULT_PAGE, DEFAULT_PAGE_SIZE, postDtos.size()));
    }

    @Override
    public PageResponse<PostDto> getLikedPosts() {
        String myuserid = currentUserId();
        List<LikelistEntity> likedList = likelistRepository.findByUserId(myuserid);
        List<PostDto> postDtos = new ArrayList<>();
        for (LikelistEntity liked : likedList) {
            WorkEntity work = workRepository.findById(liked.getId().getWorkid())
                    .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
            postDtos.add(postAssembler.build(work, myuserid, false));
        }
        return new PageResponse<>(postDtos, Pagination.of(DEFAULT_PAGE, DEFAULT_PAGE_SIZE, postDtos.size()));
    }

    @Override
    public PageResponse<UserSummary> getFollowing() {
        String myuserid = currentUserId();
        List<FollowlistEntity> following = followlistRepository.findByFollowerId(myuserid);
        List<UserSummary> summaries = new ArrayList<>();
        for (FollowlistEntity follow : following) {
            summaries.add(toSummary(follow.getId().getFolloweeid()));
        }
        return new PageResponse<>(summaries, Pagination.of(DEFAULT_PAGE, DEFAULT_PAGE_SIZE, summaries.size()));
    }

    @Override
    public PageResponse<UserSummary> getFollowers() {
        String myuserid = currentUserId();
        List<FollowlistEntity> followers = followlistRepository.findByFolloweeId(myuserid);
        List<UserSummary> summaries = new ArrayList<>();
        for (FollowlistEntity follow : followers) {
            summaries.add(toSummary(follow.getId().getFollowerid()));
        }
        return new PageResponse<>(summaries, Pagination.of(DEFAULT_PAGE, DEFAULT_PAGE_SIZE, summaries.size()));
    }

    @Override
    @Transactional
    public FollowResponse follow(String userid) {
        String myuserid = currentUserId();
        if (userid.equals(myuserid)) {
            throw new BizException(ErrorCode.BAD_REQUEST);
        }
        userRepository.findById(userid)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        FollowlistId id = new FollowlistId(userid, myuserid);
        // 幂等：已关注则不再重复写入
        if (!followlistRepository.existsById(id)) {
            FollowlistEntity follow = new FollowlistEntity();
            follow.setId(id);
            followlistRepository.save(follow);
        }
        int followers = followlistRepository.countFollowers(userid);
        return new FollowResponse(true, followers);
    }

    @Override
    @Transactional
    public FollowResponse unfollow(String userid) {
        String myuserid = currentUserId();
        FollowlistId id = new FollowlistId(userid, myuserid);
        if (followlistRepository.existsById(id)) {
            FollowlistEntity follow = new FollowlistEntity();
            follow.setId(id);
            followlistRepository.delete(follow);
        }
        int followers = followlistRepository.countFollowers(userid);
        return new FollowResponse(false, followers);
    }

    private UserSummary toSummary(String userid) {
        UserEntity user = userRepository.findById(userid)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        boolean online = getUserMessage.getToken(userid) != null;
        return new UserSummary(userid, user.getUsername(), user.getUsername(), user.getAvatar(), user.getBio(), online);
    }

    private String currentUserId() {
        String userid = UserContext.getUserId();
        if (userid == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userid;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
