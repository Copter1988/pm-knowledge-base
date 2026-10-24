package com.tpjy.service;

import com.tpjy.entity.Balance;
import com.tpjy.entity.WeightBox;
import com.tpjy.mapper.BalanceMapper;
import com.tpjy.mapper.WeightBoxMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 天平 / 砝码 / 记录模板维护（WBS 1.3.7，成员乙）。
 * 天平与砝码盒主数据 CRUD（记录模板字段显隐/顺序为 FR-019 的后续扩展）。
 */
@Service
public class MasterDataService {

    private final BalanceMapper balanceMapper;
    private final WeightBoxMapper weightBoxMapper;

    public MasterDataService(BalanceMapper balanceMapper, WeightBoxMapper weightBoxMapper) {
        this.balanceMapper = balanceMapper;
        this.weightBoxMapper = weightBoxMapper;
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

    public void deleteWeightBox(Long id) {
        weightBoxMapper.deleteById(id);
    }

    public List<WeightBox> listWeightBoxes() {
        return weightBoxMapper.selectList(null);
    }
}
