package com.cjq.chenaiagent.agent;

import com.cjq.chenaiagent.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.internal.StringUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author cjq
 * @date 2023/9/27 10:09
 * 抽象基础代理类，用于管理代理状态和执行流程！
 * 子类必须实现step方法！
 */

@Data
@Slf4j
public abstract class BaseAgent {

    // 代理名称
    private String name;

    // 提示词
    private String systemPrompt;
    // 下一步提示词
    private String nextStepPrompt;

    // 代理状态
    private AgentState state = AgentState.IDLE;

    // 最大步骤数
    private int maxSteps = 5;
    // 当前步骤数
    private int currentStep = 0;

    // LLM
    private ChatClient chatClient;

    // Memory记忆 自主维护对话上下文！
    private List<Message> messageList = new ArrayList<>();

    /**
     * 执行代理
     * @param userPrompt 用户输入
     * @return 代理结果
     */
    public String run(String userPrompt) {
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.state);
        }
        if (StringUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }

        state = AgentState.RUNNING;
        // 记录消息上下文！
        messageList.add(new UserMessage(userPrompt));

        List<String> results = new ArrayList<>();
        try {
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step " + stepNumber + "/" + maxSteps);

                String stepResult = step();
                String result = "Step " + stepNumber + ": " + stepResult;
                results.add(result);
            }

            if (currentStep >= maxSteps) {
                state = AgentState.FINISHED;
                results.add("Terminated: Reached max steps (" + maxSteps + ")");
            }
            return String.join("\n", results);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("Error executing agent", e);
            return "执行错误" + e.getMessage();
        } finally {

            this.cleanup();
        }
    }


    public abstract String step();


    protected void cleanup() {

    }


    /**
     * 流式执行代理！
     * @param userPrompt
     * @return 执行结果
     */
    public SseEmitter runStream(String userPrompt) {

        SseEmitter emitter = new SseEmitter(300000L);

        // 异步处理！避免阻塞主线程！
        CompletableFuture.runAsync(() -> {
            try {
                // 基本校验！
                if (this.state != AgentState.IDLE) {
                    emitter.send("错误：无法从状态运行代理: " + this.state);
                    emitter.complete();
                    return;
                }
                if (StringUtil.isBlank(userPrompt)) {
                    emitter.send("错误：不能使用空提示词运行代理");
                    emitter.complete();
                    return;
                }
                // 设置状态为运行中
                state = AgentState.RUNNING;
                // 记录消息上下文
                messageList.add(new UserMessage(userPrompt));
                try {
                    for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                        int stepNumber = i + 1;
                        currentStep = stepNumber;
                        log.info("Executing step " + stepNumber + "/" + maxSteps);
                        // 执行步骤
                        String stepResult = step();
                        String result = "Step " + stepNumber + ": " + stepResult;
                        // 发送结果
                        emitter.send(result);
                    }

                    if (currentStep >= maxSteps) {
                        state = AgentState.FINISHED;
                        emitter.send("执行结束: 达到最大步骤 (" + maxSteps + ")");
                    }
                    // 正常结束
                    emitter.complete();
                } catch (Exception e) {
                    state = AgentState.ERROR;
                    log.error("执行智能体失败", e);
                    try {
                        emitter.send("执行错误: " + e.getMessage());
                        emitter.complete();
                    } catch (Exception ex) {
                        emitter.completeWithError(ex);
                    }
                } finally {
                    // 清理资源
                    this.cleanup();
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        // 超时处理
        emitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE connection timed out");
        });
        // 结束处理
        emitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE connection completed");
        });

        return emitter;
    }


}
