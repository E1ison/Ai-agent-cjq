package com.cjq.chenaiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

/**
 * 自定义RAG检索增强工厂！
 */

@Slf4j
public class LoveAppRagCustomAdvisorFactory {
    public static Advisor createLoveAppRagCustomAdvisor(VectorStore vectorStore, String status) {
        // 根据状态来过滤文档！
        Filter.Expression expression = new FilterExpressionBuilder()
                .eq("status", status)
                .build();
        // 文档检索器
        DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .filterExpression(expression)  // 过滤条件
                .similarityThreshold(0.5) // 相似度阈值！
                .topK(3) // 返回的文档数量！
                .build();
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                // 添加过滤器，处理异常！
                .queryAugmenter(LoveAppContextualQueryAugmenterFactory.createLoveAppContextualQueryAugmenter())
                .build();
    }
}