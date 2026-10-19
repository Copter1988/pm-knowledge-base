package com.tpjy.config;

import com.tpjy.common.Constants;
import com.tpjy.entity.Balance;
import com.tpjy.entity.SysUser;
import com.tpjy.entity.WeightBox;
import com.tpjy.mapper.BalanceMapper;
import com.tpjy.mapper.SysUserMapper;
import com.tpjy.mapper.WeightBoxMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * 演示数据初始化（幂等：仅当表为空时插入）。
 * 用户密码由 Java 用 BCrypt 现算，避免在 SQL 里写死哈希。
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    @Resource
    private SysUserMapper userMapper;
    @Resource
    private WeightBoxMapper weightBoxMapper;
    @Resource
    private BalanceMapper balanceMapper;
    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        seedMasterData();
    }

    private void seedUsers() {
        if (userMapper.selectCount(null) > 0) {
            return;
        }
        userMapper.insert(buildUser("admin", "管理员", Constants.Role.ADMIN));
        userMapper.insert(buildUser("liyi", "李一", Constants.Role.CHECKER));
        userMapper.insert(buildUser("lier", "李二", Constants.Role.CHECKER));
        userMapper.insert(buildUser("qiyi", "齐一", Constants.Role.REVIEWER));
        log.info("已初始化演示账号（密码均为 123456）：admin / liyi / lier / qiyi");
    }

    private SysUser buildUser(String username, String realName, String role) {
        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("123456"));
        u.setRealName(realName);
        u.setRole(role);
        u.setEnabled(1);
        return u;
    }

    private void seedMasterData() {
        if (weightBoxMapper.selectCount(null) == 0) {
            WeightBox b1 = new WeightBox();
            b1.setBoxNo("FM2512920");
            weightBoxMapper.insert(b1);
            WeightBox b2 = new WeightBox();
            b2.setBoxNo("FM2512921");
            weightBoxMapper.insert(b2);
        }
        if (balanceMapper.selectCount(null) > 0) {
            return;
        }
        // 单量程 220g / 0.1mg：200g、50g、10g
        Balance f1 = new Balance();
        f1.setBalanceCode("F001");
        f1.setDept("化学室");
        f1.setUniqueNo("DZTP170911C");
        f1.setRangeCount(1);
        f1.setRange1("220g");
        f1.setRange1Precision("0.1mg");
        f1.setInternalCalib("手动触发");
        f1.setCheckFreq("每天");
        f1.setWeightBoxId(1L);
        f1.setWeightL1(new BigDecimal("200"));
        f1.setWeightL2(new BigDecimal("50"));
        f1.setWeightL3(new BigDecimal("10"));
        balanceMapper.insert(f1);
        // 双量程 120g / 41g（0.1mg / 0.01mg，设备铭牌）：100g、20g、2g
        Balance f2 = new Balance();
        f2.setBalanceCode("F002");
        f2.setDept("化学室");
        f2.setUniqueNo("SJS2605002");
        f2.setRangeCount(2);
        f2.setRange1("120g");
        f2.setRange1Precision("0.1mg");
        f2.setRange2("41g");
        f2.setRange2Precision("0.01mg");
        f2.setInternalCalib("自动触发");
        f2.setCheckFreq("每天");
        f2.setWeightBoxId(2L);
        f2.setWeightL1(new BigDecimal("100"));
        f2.setWeightL2(new BigDecimal("20"));
        f2.setWeightL3(new BigDecimal("2"));
        balanceMapper.insert(f2);
        log.info("已初始化天平主数据：F001（单量程）、F002（双量程）");
    }
}
