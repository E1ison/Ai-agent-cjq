package com.cjq.chenaiagent.agent;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 实现了思考-行动的循环模式！
 */
@Data
@EqualsAndHashCode(callSuper = true)

public abstract class ReActAgent extends BaseAgent{
    // 处理当前状态并决定下一步行动
    public abstract boolean think();

    // 决定执行的行动
    public abstract String act();

    // 单个步骤
    @Override
    public String step() {
        try {
            // 先思考，再执行！
            boolean shouldAct = think();
            if (!shouldAct) {
                return "思考完成 - 无需行动";
            }
            return act();
        } catch (Exception e) {

            e.printStackTrace();
            return "步骤执行失败: " + e.getMessage();
        }
    }
}
