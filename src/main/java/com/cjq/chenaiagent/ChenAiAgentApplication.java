package com.cjq.chenaiagent;

import org.springframework.ai.autoconfigure.vectorstore.pgvector.PgVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 注意这个exclue！
@SpringBootApplication(exclude = PgVectorStoreAutoConfiguration.class)
public class ChenAiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChenAiAgentApplication.class, args);
    }

}
