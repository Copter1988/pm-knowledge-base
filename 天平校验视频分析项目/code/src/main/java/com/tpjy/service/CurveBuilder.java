package com.tpjy.service;

import com.tpjy.dto.CurveData;
import com.tpjy.dto.CurvePoint;
import com.tpjy.dto.JudgeResult;
import com.tpjy.dto.ReadingJudge;
import com.tpjy.dto.RecognizedReading;

import java.util.List;

/**
 * 曲线构建（FR-016）：把判定明细转成曲线数据（横轴 = 示值序号 1~9，含限度上下参考线）。
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
                data.getPoints().add(p);
            }
        }
        return data;
    }

    /** 把读数时间回填到曲线点（查询页展示用；导出 PDF 不打印时间）。 */
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
