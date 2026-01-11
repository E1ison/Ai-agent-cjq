package com.cjq.chenaiagent.demo.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.rag.Query;
import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MutiQueryExpanderDemoTest {
    @Resource
    private MutiQueryExpanderDemo mutiQueryExpanderDemo;

    @Test
    void expand() {
        List<Query> expand = mutiQueryExpanderDemo.expand("谁是程序员鱼皮啊？");
        Assertions.assertNotNull(expand);
    }
}