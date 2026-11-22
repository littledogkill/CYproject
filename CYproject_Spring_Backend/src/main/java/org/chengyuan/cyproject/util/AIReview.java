package org.chengyuan.cyproject.util;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class AIReview {

    private final ChatClient chatClient;

    public AIReview(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }
    public String askForWork(String text) {
        String TIP = """
                    You are a reviewer responsible for screening technical posts related to AI/Agents and their derivative projects. 
                    Input format: the user will provide the post as follows: "title": ...; "tags": #..#..#..; "content": ...; 
                    Your task: determine whether the post is relevant technical content. Review criteria: Be lenient. 
                    The author's technical literacy and writing ability may be limited, so do not judge too strictly.
                     A post qualifies as long as it is related to AI/Agents or their derivative projects, including tutorials, plugin sharing, skill sharing, and similar content. 
                     As long as the post offers some technical value, it should be accepted. Output format (plain string only): 
                     "pass:true/false;why:brief reason(If it is true, this is empty.)"
                """;
        return chatClient.prompt()
                .system(TIP)
                .user(text)      // 只放用户问题，不加系统提示、不加历史
                .call()
                .content();          // 直接返回纯文本
    }
    public String askForAsk(String text) {
        String TIP = """
                    You are a reviewer responsible for screening inquiry posts related to AI/Agents and their derivative projects.
                    Input format: the user will provide the post as follows: "title": ...; "tags": #..#..#..; "content": ...;
                    Your task: determine whether the post is an inquiry related to the topic (AI/Agents and their derivative projects).
                    Review criteria: Be lenient. The author's technical literacy and writing ability may be limited, so do not judge too strictly. 
                    A post qualifies as long as its question is related to AI/Agents or their derivative projects, including questions about how to download, how to use, basic principles, and similar content. 
                    As long as the post asks something related to our topic, it should be accepted.
                    Output format (plain string only): "pass:true/false;why:brief reason (If it is true, this is empty.)"
                
                """;
        return chatClient.prompt()
                .system(TIP)
                .user(text)      // 只放用户问题，不加系统提示、不加历史
                .call()
                .content();          // 直接返回纯文本
    }

}
