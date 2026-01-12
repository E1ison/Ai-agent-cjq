package com.cjq.chenaiagent.controller;

import com.cjq.chenaiagent.agent.ChenManus;
import com.cjq.chenaiagent.app.LoveApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

@RequestMapping("/ai")
@RestController
public class AiController {
    @Resource
    private LoveApp loveApp;

    @Resource
    private ChatModel dashscopeChatModel;

    @Resource
    private ToolCallback[] allTools;

    /**
     * 同步调用AI恋爱大师应用
     * @param userPrompt
     * @param chatId
     * @return
     */
    @GetMapping("/love_app/chat/sync")
    public String doChatWithLoveAppSync(String userPrompt, String chatId) {
        return loveApp.doChat(userPrompt,chatId);
    }

    // produces = MediaType.TEXT_EVENT_STREAM_VALUE  注意要声明这个！
    @GetMapping(value = "/love_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSSE(String message, String chatId) {
        return loveApp.doChatByStream(message, chatId);
    }

//    @GetMapping(value = "/love_app/chat/sse")
//    public Flux<ServerSentEvent<String>> doChatWithLoveAppSSE(String message, String chatId) {
//        return loveApp.doChatByStream(message, chatId)
//                .map(chunk -> ServerSentEvent.<String>builder()
//                        .data(chunk)
//                        .build());
//    }


    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        ChenManus chenManus = new ChenManus(allTools, dashscopeChatModel);
        return chenManus.runStream(message);
    }


}
