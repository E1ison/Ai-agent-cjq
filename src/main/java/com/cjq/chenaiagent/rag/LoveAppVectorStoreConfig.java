package com.cjq.chenaiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class LoveAppVectorStoreConfig {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    @Resource
    private MyKeyWordEcricher myKeywordEnricher;

    // 创建向量存储！
    @Bean
    VectorStore loveAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        // 基于内存的向量数据库！ 把文本变成向量！需要一个embeddingmodel！一定要引入EmbeddingModel属于springai的依赖！用的是阿里的dashscopeEmbeddingModel！
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel)
                .build();
        // 加载文档
        List<Document> documents = loveAppDocumentLoader.loadMarkdowns();

//        // loader完文档之后，我们对它切词，再存入向量数据库！
//        documents = myTokenTextSplitter.splitCustomized(documents);

        // 利用AI自动补充文档的关键词元信息！
        List<Document> enrichedDocuments = myKeywordEnricher.enrichDocuments(documents);

        simpleVectorStore.add(enrichedDocuments);
        return simpleVectorStore;
    }
}

