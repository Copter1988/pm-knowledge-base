package com.tpjy.service;

import com.tpjy.entity.Balance;
import com.tpjy.entity.WeightBox;
import com.tpjy.mapper.BalanceMapper;
import com.tpjy.mapper.WeightBoxMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 主数据维护测试（WBS 1.3.7，成员乙，切片 5）。
 * 通过公共 seam {@link MasterDataService} 的天平 CRUD 验证。
 */
@SpringBootTest
class MasterDataServiceTest {

    @Autowired
    private MasterDataService masterDataService;
    @Autowired
    private BalanceMapper balanceMapper;
    @Autowired
    private WeightBoxMapper weightBoxMapper;

    @Test
    @DisplayName("新增天平：落库且砝码标称值正确（大写 L 列）")
    void createBalance_persists() {
        Balance b = newBalance("F099", "200", "50", "10");
        masterDataService.createBalance(b);
        assertNotNull(b.getId(), "新增后应回填主键");

        Balance loaded = balanceMapper.selectById(b.getId());
        assertEquals("F099", loaded.getBalanceCode());
        assertEquals(0, new BigDecimal("200").compareTo(loaded.getWeightL1()));
        assertEquals(0, new BigDecimal("50").compareTo(loaded.getWeightL2()));
        assertEquals(0, new BigDecimal("10").compareTo(loaded.getWeightL3()));
    }

    @Test
    @DisplayName("修改天平：砝码标称值更新")
    void updateBalance_updatesWeights() {
        Balance b = newBalance("F098", "200", "50", "10");
        masterDataService.createBalance(b);

        b.setWeightL1(new BigDecimal("100"));
        masterDataService.updateBalance(b);

        assertEquals(0, new BigDecimal("100").compareTo(balanceMapper.selectById(b.getId()).getWeightL1()));
    }

    @Test
    @DisplayName("删除天平：查不到")
    void deleteBalance_removes() {
        Balance b = newBalance("F097", "200", "50", "10");
        masterDataService.createBalance(b);

        masterDataService.deleteBalance(b.getId());

        assertNull(balanceMapper.selectById(b.getId()), "删除后天平应不存在");
    }

    @Test
    @DisplayName("新增砝码盒：落库")
    void createWeightBox_persists() {
        WeightBox box = new WeightBox();
        box.setBoxNo("FM2512999");
        box.setRemark("测试盒");
        masterDataService.createWeightBox(box);
        assertNotNull(box.getId(), "新增后应回填主键");
        assertEquals("FM2512999", weightBoxMapper.selectById(box.getId()).getBoxNo());
    }

    @Test
    @DisplayName("删除砝码盒：查不到")
    void deleteWeightBox_removes() {
        WeightBox box = new WeightBox();
        box.setBoxNo("FM2512998");
        box.setRemark("测试盒");
        masterDataService.createWeightBox(box);
        masterDataService.deleteWeightBox(box.getId());
        assertNull(weightBoxMapper.selectById(box.getId()), "删除后砝码盒应不存在");
    }

    private Balance newBalance(String code, String w1, String w2, String w3) {
        Balance b = new Balance();
        b.setBalanceCode(code);
        b.setDept("测试室");
        b.setUniqueNo("U" + System.currentTimeMillis());
        b.setRangeCount(1);
        b.setRange1("220g");
        b.setRange1Precision("0.1mg");
        b.setWeightL1(new BigDecimal(w1));
        b.setWeightL2(new BigDecimal(w2));
        b.setWeightL3(new BigDecimal(w3));
        return b;
    }
}
