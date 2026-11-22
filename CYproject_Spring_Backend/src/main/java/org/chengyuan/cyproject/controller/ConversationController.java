package org.chengyuan.cyproject.controller;

import org.chengyuan.cyproject.dto.common.ConversationItem;
import org.chengyuan.cyproject.dto.common.MessageDto;
import org.chengyuan.cyproject.dto.common.PageResponse;
import org.chengyuan.cyproject.dto.request.SendMessageRequest;
import org.chengyuan.cyproject.dto.response.MessageListResponse;
import org.chengyuan.cyproject.dto.response.Response;
import org.chengyuan.cyproject.dto.response.UnreadCountResponse;
import org.chengyuan.cyproject.service.ConversationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public Response<PageResponse<ConversationItem>> getConversations() {
        return Response.ok(conversationService.getConversations());
    }

    @GetMapping("/{peerId}/messages")
    public Response<MessageListResponse> getMessages(@PathVariable String peerId) {
        return Response.ok(conversationService.getMessages(peerId));
    }

    @PostMapping("/{peerId}/messages")
    public Response<MessageDto> sendMessage(@PathVariable String peerId,
                                            @RequestBody SendMessageRequest request) {
        return Response.ok(conversationService.sendMessage(peerId, request));
    }

    @PostMapping("/{peerId}/read")
    public Response<UnreadCountResponse> markAsRead(@PathVariable String peerId) {
        return Response.ok(conversationService.markAsRead(peerId));
    }
}
