package com.cjq.chenaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class WebSearchToolTest {
    @Value(value = "${search-api.api-key}")
    private String serachApi;
    @Test
    void searchWeb() {
        WebSearchTool tool = new WebSearchTool(serachApi);
        String query = "西安交通大学";
        String result = tool.searchWeb(query);
        Assertions.assertNotNull(result);
    }
}