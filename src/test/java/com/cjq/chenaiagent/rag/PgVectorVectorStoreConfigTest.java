package com.cjq.chenaiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class PgVectorVectorStoreConfigTest {

    @Resource
    private VectorStore pgVectorVectorStore;

    @Test
    void test() {
        List<Document> documents = List.of(
                new Document("鱼皮的编程导航有什么用？学编程！学项目！", Map.of("meta1", "meta1")),
                new Document("程序员鱼皮的原创项目教程！codefather.cn"),
                new Document("鱼皮长得很帅气！", Map.of("meta2", "meta2")));

        pgVectorVectorStore.add(documents);

        List<Document> results = pgVectorVectorStore.similaritySearch(SearchRequest.builder().query("如何学习编程？").topK(5).build());
        Assertions.assertNotNull(results);
    }
}