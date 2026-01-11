package com.cjq.chenaiagent.demo.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.stereotype.Component;

import java.util.List;

// 扩展搜索！  还是用AI来扩展！
@Component
public class MutiQueryExpanderDemo {

    @Resource
    private final ChatClient.Builder chatClientBuilder;

    public MutiQueryExpanderDemo (ChatModel dashscopeChatModel) {
        this.chatClientBuilder = ChatClient.builder(dashscopeChatModel);
    }

    public List<Query> expand(String query) {
        MultiQueryExpander queryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder)
                .numberOfQueries(3)
                .build();
        List<Query> queries = queryExpander.expand(new Query("谁是程序员鱼皮啊？"));
        return queries;
    }
}
