package org.chengyuan.cyproject.service.impl;

import org.chengyuan.cyproject.dto.common.ConversationItem;
import org.chengyuan.cyproject.dto.common.MessageDto;
import org.chengyuan.cyproject.dto.common.PageResponse;
import org.chengyuan.cyproject.dto.common.Pagination;
import org.chengyuan.cyproject.dto.request.SendMessageRequest;
import org.chengyuan.cyproject.dto.response.MessageListResponse;
import org.chengyuan.cyproject.dto.response.UnreadCountResponse;
import org.chengyuan.cyproject.entity.ConversationEntity;
import org.chengyuan.cyproject.entity.UserEntity;
import org.chengyuan.cyproject.error.ErrorCode;
import org.chengyuan.cyproject.error.errors.BizException;
import org.chengyuan.cyproject.repository.ConversationRepository;
import org.chengyuan.cyproject.repository.UserRepository;
import org.chengyuan.cyproject.service.ConversationService;
import org.chengyuan.cyproject.util.GetUserMessage;
import org.chengyuan.cyproject.util.TimeUtil;
import org.chengyuan.cyproject.util.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ConversationServiceImpl implements ConversationService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final GetUserMessage getUserMessage;

    private final ConversationRepository conversationRepository;

    private final UserRepository userRepository;

    public ConversationServiceImpl(ConversationRepository conversationRepository,
                                   UserRepository userRepository,
                                   GetUserMessage getUserMessage) {
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.getUserMessage = getUserMessage;
    }

    @Override
    public PageResponse<ConversationItem> getConversations() {
        String userid = currentUserId();
        List<ConversationEntity> latestList = conversationRepository.findLatestPerPeer(userid);
        List<ConversationItem> items = new ArrayList<>();
        for (ConversationEntity conversation : latestList) {
            String peerId = resolvePeerId(conversation, userid);
            UserEntity peer = userRepository.findById(peerId)
                    .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
            boolean online = getUserMessage.getToken(peerId) != null;
            // 未读计数暂未实现，固定返回 0
            items.add(new ConversationItem(
                    peerId,
                    peer.getUsername(),
                    peer.getUsername(),
                    peer.getAvatar(),
                    online,
                    conversation.getBody(),
                    TimeUtil.toInstant(conversation.getPublishtime()),
                    0));
        }
        return new PageResponse<>(items, Pagination.of(DEFAULT_PAGE, DEFAULT_PAGE_SIZE, items.size()));
    }

    @Override
    public MessageListResponse getMessages(String peerId) {
        String userid = currentUserId();
        userRepository.findById(peerId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        List<ConversationEntity> conversationList = conversationRepository.findMessagesBetween(userid, peerId);
        List<MessageDto> messages = new ArrayList<>();
        for (ConversationEntity conversation : conversationList) {
            messages.add(toMessageDto(conversation));
        }
        return new MessageListResponse(messages, null);
    }

    @Override
    @Transactional
    public MessageDto sendMessage(String peerId, SendMessageRequest request) {
        String content = request.getContent();
        if (content == null || content.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST);
        }
        String userid = currentUserId();
        userRepository.findById(peerId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        ConversationEntity conversation = new ConversationEntity();
        conversation.setId(UUID.randomUUID().toString());
        conversation.setSenderid(userid);
        conversation.setReceiver(peerId);
        conversation.setPublishtime(now);
        conversation.setBody(content);
        conversationRepository.save(conversation);

        return toMessageDto(conversation);
    }

    @Override
    public UnreadCountResponse markAsRead(String peerId) {
        // 未读计数暂未实现，标记已读直接返回 0
        return new UnreadCountResponse(0);
    }

    private String resolvePeerId(ConversationEntity conversation, String userid) {
        return userid.equals(conversation.getSenderid())
                ? conversation.getReceiver()
                : conversation.getSenderid();
    }

    private MessageDto toMessageDto(ConversationEntity conversation) {
        UserEntity sender = userRepository.findById(conversation.getSenderid())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        UserEntity receiver = userRepository.findById(conversation.getReceiver())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        return new MessageDto(
                conversation.getId(),
                sender.getUsername(),
                receiver.getUsername(),
                conversation.getBody(),
                TimeUtil.toInstant(conversation.getPublishtime()));
    }

    private String currentUserId() {
        String userid = UserContext.getUserId();
        if (userid == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userid;
    }
}
