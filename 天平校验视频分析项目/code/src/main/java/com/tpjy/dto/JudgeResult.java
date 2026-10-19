package com.tpjy.dto;

import java.util.ArrayList;
import java.util.List;

/** 判定引擎的输出（M5 的输出契约）。 */
public class JudgeResult {

    /** 外部校验结论：true 合格 / false 不合格。 */
    private boolean qualified;
    /** 前置条件是否一票否决。 */
    private boolean vetoedByPrecondition;
    /** 逐示值明细。 */
    private List<ReadingJudge> details = new ArrayList<ReadingJudge>();
    /** 不合格原因（含「哪个砝码不合格」，对应 SRS §3.1.7 双量程流程）。 */
    private List<String> failReasons = new ArrayList<String>();
    /** 较大误差（>1%）预填的备注，默认「无」；可删改，不参与合格判定。 */
    private String remarkPrefill = "无";
    /** 结论文字，如「合格」/「不合格」。 */
    private String resultText;

    public boolean isQualified() {
        return qualified;
    }

    public void setQualified(boolean qualified) {
        this.qualified = qualified;
    }

    public boolean isVetoedByPrecondition() {
        return vetoedByPrecondition;
    }

    public void setVetoedByPrecondition(boolean vetoedByPrecondition) {
        this.vetoedByPrecondition = vetoedByPrecondition;
    }

    public List<ReadingJudge> getDetails() {
        return details;
    }

    public void setDetails(List<ReadingJudge> details) {
        this.details = details;
    }

    public List<String> getFailReasons() {
        return failReasons;
    }

    public void setFailReasons(List<String> failReasons) {
        this.failReasons = failReasons;
    }

    public String getRemarkPrefill() {
        return remarkPrefill;
    }

    public void setRemarkPrefill(String remarkPrefill) {
        this.remarkPrefill = remarkPrefill;
    }

    public String getResultText() {
        return resultText;
    }

    public void setResultText(String resultText) {
        this.resultText = resultText;
    }
}
