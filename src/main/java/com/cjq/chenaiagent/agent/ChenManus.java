package com.cjq.chenaiagent.agent;

import com.cjq.chenaiagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * Chen超级智能体！
 */

@Component
public class ChenManus extends ToolCallAgent{
    // 一个工具调用，一个chatmodel，注意是阿里的大模型，名字不能错！
    public ChenManus(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("ChenManus");
        String SYSTEM_PROMPT = """  
                You are ChenManus, an all-capable AI assistant, aimed at solving any task presented by the user.  
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.  
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """  
                Based on user needs, proactively select the most appropriate tool or combination of tools.  
                For complex tasks, you can break down the problem and use different tools step by step to solve it.  
                After using each tool, clearly explain the execution results and suggest the next steps.  
                If you want to stop the interaction at any point, use the `terminate` tool/function call.  
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        // 5次测试即可！
        this.setMaxSteps(10);

        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }


}
