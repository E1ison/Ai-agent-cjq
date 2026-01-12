package com.cjq.chenaiagent.agent;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.cjq.chenaiagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class ToolCallAgent extends ReActAgent{
    // 可用的工具
    private final ToolCallback[] availableTools;

    // 工具调用的响应
    private ChatResponse toolCallChatResponse;

    // 工具调用管理器
    private final ToolCallingManager toolCallingManager;

    // 聊天选项 禁用SpringAI内置的工具调用机制！自己维护工具调用！
    private final ChatOptions chatOptions;
    // 构造函数
    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 创建一个DashScopeChatOptions对象，并设置代理工具调用
        this.chatOptions = DashScopeChatOptions.builder()
                .withProxyToolCalls(true) // 不用SpringAI内置的工具调用机制！
                .build();
    }

    /**
     * 处理当前状态并决定下一步行动！
     * @return
     */
    @Override
    public boolean think() {
        // 如果有下一步提示，则添加到消息列表中
        if (getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }
        // 上下文列表
        List<Message> messageList = getMessageList();
        // 提示词  用自己的AI工具调用逻辑！
        Prompt prompt = new Prompt(messageList, chatOptions);
        try {
            // 调用AI 模型
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .tools(availableTools)
                    .call()
                    .chatResponse();

            // 记录响应结果，用于一会执行
            this.toolCallChatResponse = chatResponse;
            // AI输出文本
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            // 获取工具调用结果
            String result = assistantMessage.getText();
            // 获取工具调用列表
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            log.info(getName() + "的思考: " + result);
            log.info(getName() + "选择了 " + toolCallList.size() + " 个工具来使用");
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("工具名称：%s，参数：%s",
                            toolCall.name(),
                            toolCall.arguments())
                    )
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);
            if (toolCallList.isEmpty()) {
                // 没有工具调用
                getMessageList().add(assistantMessage);
                return false;
            } else {

                return true;
            }
        } catch (Exception e) {
            log.error(getName() + "的思考过程遇到了问题: " + e.getMessage());
            getMessageList().add(
                    new AssistantMessage("处理时遇到错误: " + e.getMessage()));
            return false;
        }
    }

    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "没有工具调用";
        }
        // 当前历史会话消息列表
        Prompt prompt = new Prompt(getMessageList(), chatOptions);
        // 执行工具调用（手动）
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);

        // 更新消息列表conversationHistory会拼接消息和调用工具的消息，放在prompt里面！
        setMessageList(toolExecutionResult.conversationHistory());
        // 获取工具调用结果
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        // 是否调用终止工具？
        boolean doTerminate = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> response.name().equals("doTerminate"));
        if (doTerminate) {
            setState(AgentState.FINISHED);
            log.info("工具调用终止！");
        }
        String results = toolResponseMessage.getResponses().stream()
                .map(response -> "工具 " + response.name() + " 完成了它的任务！结果: " + response.responseData())
                .collect(Collectors.joining("\n"));
        // 打印结果
        log.info(results);
        return results;
    }
}
