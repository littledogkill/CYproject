package org.chengyuan.cyproject.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.chengyuan.cyproject.dto.common.MessageDto;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageListResponse {

    private List<MessageDto> items;

    private String nextCursor;
}