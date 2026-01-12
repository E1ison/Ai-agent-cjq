package com.cjq.chenaiagent.app;

import com.cjq.chenaiagent.advisor.MyLoggerAdvisor;
import com.cjq.chenaiagent.advisor.ReReadingAdvisor;
import com.cjq.chenaiagent.chatmemory.FileBasedChatMemory;
import com.cjq.chenaiagent.rag.LoveAppRagCustomAdvisorFactory;
import com.cjq.chenaiagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。" +
            "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；" +
            "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。" +
            "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";

    // 初始化Ai客户端！ ChatClient
    public LoveApp(ChatModel dashscopeChatModel) {
        // 基于文件来实现持久化记忆！
        String fileDir = System.getProperty("user.dir") + "/chat-memory";
        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);


        // 基于内存的记忆  Advisor模式对所有请求生效，也可以针对对话Id生效！
//        ChatMemory chatMemory = new InMemoryChatMemory();

        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        // 不指定ID也有默认的，源码里面有！
                        new MessageChatMemoryAdvisor(chatMemory),
//                        new SimpleLoggerAdvisor()
                        // 自定义拦截器！
                        new MyLoggerAdvisor()
                        // 每次发送的时候，把之前的消息带上发送！
//                        new ReReadingAdvisor()
                )
                .build();
    }

    /**
     * Ai基本聊天
     * @param message
     * @param chatId
     * @return
     */
    public String doChat(String message,String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                // CHAT_MEMORY_CONVERSATION_ID_KEY取当前Id的上下文，CHAT_MEMORY_RETRIEVE_SIZE_KEY获取多少历史上下文条数！
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        // 拿到输出信息！
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    // 测试结构化输出！
    record LoveReport(String title, List<String> suggestions) {
    }



    /**
     * Ai基本聊天（SSE流式传输）
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        Flux<String> content = chatClient
                .prompt()
                .user(message)
                // CHAT_MEMORY_CONVERSATION_ID_KEY取当前Id的上下文，CHAT_MEMORY_RETRIEVE_SIZE_KEY获取多少历史上下文条数！
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // SSE流式，call改成stream
                .stream()
                .content();
        return content;
    }



    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                // 结构化输出的核心！
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }


    // RAG 基础！
    @Resource
    private VectorStore loveAppVectorStore;  // 向量数据库！

    // RetrievalAugmentationAdvisor的RAG！
    @Resource
    private Advisor loveAppRagCloudAdvisor;
    @Resource
    private VectorStore pgVectorVectorStore;

    @Resource
    private QueryRewriter queryRewriter;


    // 这个RAG有三种，第一种就是用内存当自己的向量数据库，然后去查并根据Advisor进行向量数据库的检索！还有一种是基于PGVectorVectorStore来实现的！所有RAG都是这个套路，包括阿里云！
    // advisors 就是拦截器！
    // QuestionAnswerAdvisor：一键套餐，“拿 VectorStore 直接用”
    // RetrievalAugmentationAdvisor：自助餐/流程编排，“每一步都能换组件、加策略”
    public String doChatWithRag(String message, String chatId) {
        String rewriteMessage = queryRewriter.doQueryRewrite(message); // 查询重写！
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(rewriteMessage)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))

                .advisors(new MyLoggerAdvisor())
                // RAG+向量数据库！  QuestionAnswerAdvisor是一个简单的问答拦截器实现
                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))  // 默认内存！
                // 基于阿里云知识库服务器的RAG检索！
//                .advisors(loveAppRagCloudAdvisor)
                // 应用RAG 基于PGVectorVectorStore向量存储来检索！
//                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                // 自定义RAG  文档查询器+上下文增强！
//                .advisors(LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(loveAppVectorStore,"单身"))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


    /**
     * 工具能力
     */
    // AI 调用工具能力
    @Resource
    private ToolCallback[] allTools;

    public String doChatWithTools(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyLoggerAdvisor()) // 日志
                .tools(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }




    // 自动获取json里面配置的MCP服务！
    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    public String doChatWithMCP(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyLoggerAdvisor()) // 日志
                .tools(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


}
