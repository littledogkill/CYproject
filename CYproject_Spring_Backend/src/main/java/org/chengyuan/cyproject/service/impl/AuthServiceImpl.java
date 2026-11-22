package org.chengyuan.cyproject.service.impl;

import org.chengyuan.cyproject.dto.common.UserDto;
import org.chengyuan.cyproject.dto.request.LoginRequest;
import org.chengyuan.cyproject.dto.request.RegisterRequest;
import org.chengyuan.cyproject.dto.response.AuthResponse;
import org.chengyuan.cyproject.entity.UserEntity;
import org.chengyuan.cyproject.error.ErrorCode;
import org.chengyuan.cyproject.error.errors.BizException;
import org.chengyuan.cyproject.repository.UserRepository;
import org.chengyuan.cyproject.service.AuthService;
import org.chengyuan.cyproject.util.GetUserMessage;
import org.chengyuan.cyproject.util.JwtUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class AuthServiceImpl implements AuthService {

    private final StringRedisTemplate userRedisTemplate;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final String DEFAULT_AVATAR = "http://localhost:8080/avatarFiles/avatar_";

    private static final String DEFAULT_BIO = "hello world(现在不支持设置个性标签)";

    private final UserRepository userRepository;

    private final JwtUtil jwtUtil;

    private final GetUserMessage getUserMessage;

    private static final String LOGIN_FAIL_KEY = "login:fail:";

    private static final String LOGIN_LOCK_KEY = "login:lock:";

    private static final int MAX_FAIL_COUNT = 5;

    private static final int LOCK_MINUTES = 15;

    public AuthServiceImpl(UserRepository userRepository, JwtUtil jwtUtil,
                           GetUserMessage getUserMessage,  StringRedisTemplate userRedisTemplate) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.getUserMessage = getUserMessage;
        this.userRedisTemplate = userRedisTemplate;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = trimToNull(request.getEmail());
        String password = request.getPassword();
        String lockKey = LOGIN_LOCK_KEY + email;
        String failKey = LOGIN_FAIL_KEY + email;
        String isLocked = userRedisTemplate.opsForValue().get(lockKey);
        if (isLocked != null) {
            throw new BizException(ErrorCode.TOO_MANY_REQUESTS);
        }
        if (email == null || password == null || password.isEmpty()) {
            handleLoginFailure(email, failKey, lockKey);
            throw new BizException(ErrorCode.BAD_REQUEST);
        }

        Optional<UserEntity> userEntityOpt = userRepository.findByEmail(email);
        if (userEntityOpt.isEmpty()) {
            handleLoginFailure(email, failKey, lockKey);
            throw new BizException(ErrorCode.WRONG_CREDENTIALS);
        }
        UserEntity userEntity = userEntityOpt.get();
        if (!passwordMatches(password, userEntity.getPassword())) {
            handleLoginFailure(email, failKey, lockKey);
            throw new BizException(ErrorCode.WRONG_CREDENTIALS);
        }

        // 历史明文密码登录成功后自动升级为 BCrypt 哈希
        upgradePlaintextPassword(userEntity, password);

        // 每次登录都重新签发一个全新 token，这里避免用redis，目前用处不大
        return issueToken(userEntity, userEntity.getId());
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        String username = trimToNull(request.getUsername());
        String email = trimToNull(request.getEmail());
        String password = request.getPassword();

        validateRegister(username, email, password);

        if (userRepository.findByEmail(email).isPresent()) {
            throw new BizException(ErrorCode.EMAIL_EXISTS);
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        Random random = new Random();
        int n = random.nextInt(6) + 1;
        String avatarUrl =  DEFAULT_AVATAR + n + ".png";
        // 注意：当前主键规则为 email + password（历史设计），这里保持一致,《但是现在改成用UUID》
        String userid = "user_" + UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setId(userid);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setAvatar(avatarUrl);
        user.setBio(DEFAULT_BIO);
        userRepository.save(user);

        return issueToken(user, userid);
    }

    private void validateRegister(String username, String email, String password) {
        if (username == null || username.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST);
        }
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BizException(ErrorCode.BAD_REQUEST);
        }
        if (password == null || password.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST);
        }
    }

    private AuthResponse issueToken(UserEntity userEntity, String userid) {
        String token = jwtUtil.generateToken(userid);
        UserDto userDto = getUserMessage.buildUserDto(userEntity, userid, false, false);
        // 刚登录/注册，用户必然在线
        userDto.setOnline(true);
        getUserMessage.cacheUser(userid, userDto, token, jwtUtil.getExpirationSeconds());
        return new AuthResponse(token, userDto);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }
        // BCrypt 哈希以 $2a$ / $2b$ / $2y$ 开头
        if (storedPassword.startsWith("$2")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        // 兼容历史明文密码
        return storedPassword.equals(rawPassword);
    }

    private void upgradePlaintextPassword(UserEntity userEntity, String rawPassword) {
        String stored = userEntity.getPassword();
        if (stored != null && !stored.startsWith("$2")) {
            userEntity.setPassword(passwordEncoder.encode(rawPassword));
            userRepository.save(userEntity);
        }
    }

    private void handleLoginFailure(String email, String failKey, String lockKey) {
        Long failCount = userRedisTemplate.opsForValue().increment(failKey);
        if (failCount != null && failCount == 1) {
            userRedisTemplate.expire(failKey, LOCK_MINUTES, TimeUnit.MINUTES);
        }
        if (failCount != null && failCount >= MAX_FAIL_COUNT) {
            userRedisTemplate.opsForValue().set(lockKey, "locked", LOCK_MINUTES, TimeUnit.MINUTES);
            userRedisTemplate.delete(failKey);
            throw new BizException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }
}
