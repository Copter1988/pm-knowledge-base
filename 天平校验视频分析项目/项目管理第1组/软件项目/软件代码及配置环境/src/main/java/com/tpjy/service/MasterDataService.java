package com.tpjy.service;

import com.tpjy.entity.Balance;
import com.tpjy.entity.RecordTemplate;
import com.tpjy.entity.WeightBox;
import com.tpjy.mapper.BalanceMapper;
import com.tpjy.mapper.RecordTemplateMapper;
import com.tpjy.mapper.WeightBoxMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 天平 / 砝码 / 记录模板维护（WBS 1.3.7，成员乙）。
 * 天平、砝码盒、记录呈现模板三类主数据 CRUD。
 */
@Service
public class MasterDataService {

    private final BalanceMapper balanceMapper;
    private final WeightBoxMapper weightBoxMapper;
    private final RecordTemplateMapper templateMapper;

    public MasterDataService(BalanceMapper balanceMapper, WeightBoxMapper weightBoxMapper,
                             RecordTemplateMapper templateMapper) {
        this.balanceMapper = balanceMapper;
        this.weightBoxMapper = weightBoxMapper;
        this.templateMapper = templateMapper;
    }

    public Balance createBalance(Balance balance) {
        balanceMapper.insert(balance);
        return balance;
    }

    public Balance updateBalance(Balance balance) {
        balanceMapper.updateById(balance);
        return balance;
    }

    public void deleteBalance(Long id) {
        balanceMapper.deleteById(id);
    }

    public Balance getBalance(Long id) {
        return balanceMapper.selectById(id);
    }

    public List<Balance> listBalances() {
        return balanceMapper.selectList(null);
    }

    public WeightBox createWeightBox(WeightBox box) {
        weightBoxMapper.insert(box);
        return box;
    }

    public WeightBox updateWeightBox(WeightBox box) {
        weightBoxMapper.updateById(box);
        return box;
    }

    public WeightBox getWeightBox(Long id) {
        return weightBoxMapper.selectById(id);
    }

    public void deleteWeightBox(Long id) {
        weightBoxMapper.deleteById(id);
    }

    public List<WeightBox> listWeightBoxes() {
        return weightBoxMapper.selectList(null);
    }

    public RecordTemplate createTemplate(RecordTemplate template) {
        templateMapper.insert(template);
        return template;
    }

    public RecordTemplate updateTemplate(RecordTemplate template) {
        templateMapper.updateById(template);
        return template;
    }

    public void deleteTemplate(Long id) {
        templateMapper.deleteById(id);
    }

    public RecordTemplate getTemplate(Long id) {
        return templateMapper.selectById(id);
    }

    public List<RecordTemplate> listTemplates() {
        return templateMapper.selectList(null);
    }
}
