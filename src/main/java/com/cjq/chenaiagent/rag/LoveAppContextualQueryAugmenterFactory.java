package com.cjq.chenaiagent.rag;


import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;

// 创建上下文查询增强器的工厂

public class LoveAppContextualQueryAugmenterFactory {
    // 检索不到的异常,修改模板！
    public static ContextualQueryAugmenter createLoveAppContextualQueryAugmenter() {
        PromptTemplate promptTemplate = new PromptTemplate("""
                你应该输出下面的内容：
                抱歉，我只能回答恋爱相关的问题，别的没办法帮到您哦，
                请你线下私聊海豹呢！
                """);
        return ContextualQueryAugmenter.builder()
                .allowEmptyContext(false)
                .emptyContextPromptTemplate(promptTemplate)
                .build();
    }

}
