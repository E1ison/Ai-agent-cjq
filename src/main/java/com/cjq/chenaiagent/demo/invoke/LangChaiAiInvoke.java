package com.cjq.chenaiagent.demo.invoke;

import dev.langchain4j.community.model.dashscope.QwenChatModel;

public class LangChaiAiInvoke {


    public static void main(String[] args) {
        QwenChatModel qwenChatModel = QwenChatModel.builder()
                .apiKey(TestApiKey.API_KEY)
                .modelName("qwen-plus")
                .build();
        String chat = qwenChatModel.chat("你好，我是学习Agent开发的海豹程序员！");
        System.out.println(chat);
    }
}
