package org.chengyuan.cyproject.service;

import org.chengyuan.cyproject.dto.common.ConversationItem;
import org.chengyuan.cyproject.dto.common.MessageDto;
import org.chengyuan.cyproject.dto.common.PageResponse;
import org.chengyuan.cyproject.dto.request.SendMessageRequest;
import org.chengyuan.cyproject.dto.response.MessageListResponse;
import org.chengyuan.cyproject.dto.response.UnreadCountResponse;

public interface ConversationService {

    PageResponse<ConversationItem> getConversations();

    MessageListResponse getMessages(String peerId);

    MessageDto sendMessage(String peerId, SendMessageRequest request);

    UnreadCountResponse markAsRead(String peerId);
}
