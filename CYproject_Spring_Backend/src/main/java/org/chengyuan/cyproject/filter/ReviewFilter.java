package org.chengyuan.cyproject.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.chengyuan.cyproject.dto.request.CreatePostRequest;
import org.chengyuan.cyproject.dto.response.Response;
import org.chengyuan.cyproject.error.ErrorCode;
import org.chengyuan.cyproject.util.AIReview;
import org.chengyuan.cyproject.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReviewFilter extends OncePerRequestFilter {

    private final StringRedisTemplate postRedisTemplate;

    private final AIReview aiReview;

    private final ObjectMapper objectMapper;

    private static final String URL_PREFIX = "uploads";

    @Autowired
    private UserContext userContext;

    public ReviewFilter(ObjectMapper objectMapper, AIReview aiReview, StringRedisTemplate postRedisTemplate) {
        this.objectMapper = objectMapper;
        this.aiReview = aiReview;
        this.postRedisTemplate = postRedisTemplate;
    }

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String userid = UserContext.getUserId();
        if ("POST".equalsIgnoreCase(request.getMethod())
                && "/posts".equals(request.getRequestURI())){
            if (!userid.equals("user_1954839d-96f7-46da-a5b4-2555f359dbfd")){
                CachedBodyRequestWrapper wrapper;
                try {
                    wrapper = new CachedBodyRequestWrapper(request);
                } catch (IOException e) {
                    writeUnauthorized(response, ErrorCode.BAD_REQUEST);
                    return;
                }
                String json = wrapper.getBodyAsString();
                // 3. 解析成 CreatePostRequest
                CreatePostRequest body;
                try {
                    body = objectMapper.readValue(json, CreatePostRequest.class);
                } catch (Exception e) {
                    writeUnauthorized(response, ErrorCode.INTERNAL_ERROR);
                    return;
                }
                String title =  body.getTitle();
                String category =  body.getCategory();
                List<String> tags =  body.getTags();
                String content =  body.getContent();
                StringBuilder allTag = new StringBuilder();
                if(!tags.isEmpty()){
                    for(String tag : tags){
                        allTag.append("#").append(tag);
                    }
                }
                String theTag = allTag.toString();
                String userInput = "\"title\":"+title+";\"tags\":"+theTag+";\"content\":"+content+";";
                String pass = "false";
                String why = "抱歉，服务器出错，请等待";
                //限制每次的发帖数和ai调用数
                String today = LocalDate.now(ZoneId.of("Asia/Shanghai")).toString();
                String limitKeyPost = "post:limit:" + userid + ":" + today;
                String limitKeyAi   = "ai:limit:"   + userid + ":" + today;
                // 到当天结束的剩余秒数
                long secondsUntilEndOfDay = Duration.between(
                        LocalDateTime.now(),
                        LocalDate.now().plusDays(1).atStartOfDay()
                ).getSeconds();
                if (secondsUntilEndOfDay <= 0) secondsUntilEndOfDay = 1;
                //AI 次数上限 4
                Long aiCount = postRedisTemplate.opsForValue().increment(limitKeyAi);
                if (aiCount == null) {
                    writeUnauthorized(response, ErrorCode.INTERNAL_ERROR);
                    return;
                }
                if (aiCount == 1) {
                    postRedisTemplate.expire(limitKeyAi, secondsUntilEndOfDay, TimeUnit.SECONDS);
                }
                if (aiCount > 4) {
                    postRedisTemplate.opsForValue().decrement(limitKeyAi);
                    deleteImagesFromContent(userInput);
                    writeUnauthorized(response, 400, "今日 AI 审核次数已达上限（4次/天）");
                    return;
                }
                //发帖次数上限 2
                Long postCount = postRedisTemplate.opsForValue().increment(limitKeyPost);
                if (postCount == null) {
                    postRedisTemplate.opsForValue().decrement(limitKeyAi);
                    writeUnauthorized(response, ErrorCode.INTERNAL_ERROR);
                    return;
                }
                if (postCount == 1) {
                    postRedisTemplate.expire(limitKeyPost, secondsUntilEndOfDay, TimeUnit.SECONDS);
                }
                if (postCount > 2) {
                    postRedisTemplate.opsForValue().decrement(limitKeyPost);
                    postRedisTemplate.opsForValue().decrement(limitKeyAi);
                    deleteImagesFromContent(userInput);
                    writeUnauthorized(response, 400, "今日发帖已达上限（2篇/天）");
                    return;
                }
                //ai审查开始
                if("work".equals(category)){
                    String result = aiReview.askForWork(userInput);
                    pass = result.substring(result.indexOf("pass:") + 5, result.indexOf(";"));
                    why  = result.substring(result.indexOf("why:") + 4);
                }
                else if("ask".equals(category)){
                    String result = aiReview.askForAsk(userInput);
                    pass = result.substring(result.indexOf("pass:") + 5, result.indexOf(";"));
                    why  = result.substring(result.indexOf("why:") + 4);
                }
                if(!pass.equals("true")) {
                    postRedisTemplate.opsForValue().decrement(limitKeyPost);
                    deleteImagesFromContent(userInput);
                    writeUnauthorized(response, 400, why);
                    return;
                }
                // 审查通过：把包装后的 wrapper 传给下游（Controller 的 @RequestBody 才能正常解析）
                filterChain.doFilter(wrapper, response);
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(errorCode.getHttpStatus().value());
        response.getWriter().write(objectMapper.writeValueAsString(Response.fail(errorCode)));
    }

    private void writeUnauthorized(HttpServletResponse response, int code, String message) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(code);
        response.getWriter().write(objectMapper.writeValueAsString(Response.fail(code, message)));
    }

    private void deleteImagesFromContent(String content) throws IOException {
        if (content == null) return;
        // 提取括号里的 URL
        Pattern p = Pattern.compile("!\\[[^]]*]\\(([^)]+)\\)");
        Matcher m = p.matcher(content);
        while (m.find()) {
            String url = m.group(1);
            String fileName = url.substring(url.lastIndexOf('/') + 1);
            if (fileName.isEmpty()) continue;
            Path filePath = Paths.get(URL_PREFIX, fileName);
            boolean deleted = Files.deleteIfExists(filePath);
            if (!deleted) {
                System.out.println("文件删除失败，文件名：" + fileName);
            }
        }
    }
}
