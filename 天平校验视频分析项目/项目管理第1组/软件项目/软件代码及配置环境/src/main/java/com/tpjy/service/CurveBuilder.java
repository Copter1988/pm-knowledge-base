package com.tpjy.service;

import com.tpjy.dto.CurveData;
import com.tpjy.dto.CurvePoint;
import com.tpjy.dto.JudgeResult;
import com.tpjy.dto.ReadingJudge;
import com.tpjy.dto.RecognizedReading;

import java.util.List;

/**
 * 趋势图数据构建（FR-016）：保留 L1/L2/L3 三组各 3 次示值，渲染时分组独立显示。
 * 纯逻辑，页面（ECharts）与命令行（Java2D 出 PNG）共用。
 */
public class CurveBuilder {

    public CurveData build(String recordNo, String balanceCode, String checkDate,
                           JudgeResult judge) {
        CurveData data = new CurveData();
        data.setRecordNo(recordNo);
        data.setBalanceCode(balanceCode);
        data.setCheckDate(checkDate);
        data.setResultText(judge == null ? null : judge.getResultText());
        if (judge != null) {
            for (ReadingJudge d : judge.getDetails()) {
                CurvePoint p = new CurvePoint();
                p.setSeq(d.getSeq());
                p.setLevel(d.getLevel());
                p.setNominal(d.getNominal());
                p.setValue(d.getValue());
                p.setLower(d.getLower());
                p.setUpper(d.getUpper());
                p.setQualified(d.isQualified());
                p.setDisplayText(d.getDisplayText());
                p.setMeasureTime(d.getMeasureTime());
                data.getPoints().add(p);
            }
        }
        return data;
    }

    /** 把视频回放时间点回填到曲线点。 */
    public CurveData fillClock(CurveData data, List<RecognizedReading> readings) {
        if (data == null || readings == null) {
            return data;
        }
        for (CurvePoint p : data.getPoints()) {
            for (RecognizedReading r : readings) {
                if (r.getSeq() == p.getSeq()) {
                    p.setClock(r.getClock());
                }
            }
        }
        return data;
    }
}
