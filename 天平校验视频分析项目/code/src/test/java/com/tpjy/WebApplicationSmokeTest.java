package com.tpjy;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tpjy.entity.Balance;
import com.tpjy.entity.CalibrationRecord;
import com.tpjy.entity.Reading;
import com.tpjy.entity.SysUser;
import com.tpjy.mapper.BalanceMapper;
import com.tpjy.mapper.CalibrationRecordMapper;
import com.tpjy.mapper.ReadingMapper;
import com.tpjy.mapper.SysUserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.io.File;
import java.util.Date;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WebApplicationSmokeTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private BalanceMapper balanceMapper;
    @Autowired
    private CalibrationRecordMapper recordMapper;
    @Autowired
    private ReadingMapper readingMapper;
    @Autowired
    private SysUserMapper userMapper;

    @Test
    @DisplayName("Spring 上下文、建表、种子账号与登录页可正常启动")
    void loginWorks() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("天平校验系统")));

        MvcResult login = mvc.perform(post("/login")
                        .param("username", "liyi")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();
        HttpSession session = login.getRequest().getSession(false);
        assertNotNull(session);

        mvc.perform(get("/checker/new").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("导入校验视频")));
    }

    @Test
    @DisplayName("复核查询页在空库下也能正常渲染")
    void reviewerRecordsRenders() throws Exception {
        MvcResult login = mvc.perform(post("/login")
                        .param("username", "qiyi")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        HttpSession session = login.getRequest().getSession(false);

        mvc.perform(get("/reviewer/records")
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("记录查询与导出")));
    }

    @Test
    @DisplayName("真实 MP4 经上传接口完成 ffmpeg 抽帧、二维码识别和核对页渲染")
    void uploadAndRecognizeRenders() throws Exception {
        assumeTrue(new File("E:/ffmpeg/bin/ffmpeg.exe").isFile(), "本机未安装 ffmpeg，跳过视频集成测试");
        MvcResult login = mvc.perform(post("/login")
                        .param("username", "liyi")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        HttpSession session = login.getRequest().getSession(false);

        Balance balance = balanceMapper.selectOne(new QueryWrapper<Balance>()
                .eq("balance_code", "F001"));
        ClassPathResource video = new ClassPathResource("videos/smoke.mp4");
        MockMultipartFile upload = new MockMultipartFile(
                "video", "smoke.mp4", "video/mp4", video.getInputStream());

        mvc.perform(multipart("/checker/upload")
                        .file(upload)
                        .param("balanceId", String.valueOf(balance.getId()))
                        .param("manualValues",
                                "200.0006,199.9998,200.0011,50.0003,49.9997,50.0002,10.0001,10.0000,9.9999")
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("校验结果核对")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("200.0006")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("可进入记录核对")));
    }

    @Test
    @DisplayName("已复核记录可查询、看曲线并下载 PDF")
    void reviewedRecordCurveAndPdf() throws Exception {
        Balance balance = balanceMapper.selectOne(new QueryWrapper<Balance>()
                .eq("balance_code", "F001"));
        SysUser checker = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", "liyi"));
        SysUser reviewer = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", "qiyi"));
        assertEquals("JY001", checker.getCardCode());
        assertEquals("JY003", reviewer.getCardCode());

        CalibrationRecord record = new CalibrationRecord();
        record.setRecordNo("JY20261019-777");
        record.setBalanceId(balance.getId());
        record.setCheckDate(new Date());
        record.setCleanOk(1);
        record.setLevelOk(1);
        record.setInternalCalibOk("是");
        record.setResult(1);
        record.setRemark("无");
        record.setCheckerId(checker.getId());
        record.setReviewerId(reviewer.getId());
        record.setStatus("已复核");
        record.setCreatedAt(new Date());
        recordMapper.insert(record);

        String[] values = {"200.0006", "199.9998", "200.0011", "50.0003", "49.9997",
                "50.0002", "10.0001", "10.0000", "9.9999"};
        String[] levels = {"L1", "L1", "L1", "L2", "L2", "L2", "L3", "L3", "L3"};
        String[] nominals = {"200", "200", "200", "50", "50", "50", "10", "10", "10"};
        Date base = new Date();
        for (int i = 0; i < 9; i++) {
            Reading reading = new Reading();
            reading.setRecordId(record.getId());
            reading.setSeq(i + 1);
            reading.setWeightLevel(levels[i]);
            reading.setNominal(new BigDecimal(nominals[i]));
            reading.setReadingValue(new BigDecimal(values[i]));
            if (i >= 3) {
                reading.setDisplayUnit("mg");
                reading.setDisplayValue(new BigDecimal(values[i]).movePointRight(3).toPlainString());
            } else {
                reading.setDisplayUnit("g");
                reading.setDisplayValue(values[i]);
            }
            reading.setMeasureTime(new Date(base.getTime() + i * 60000L));
            reading.setIsQualified(1);
            readingMapper.insert(reading);
        }

        MvcResult login = mvc.perform(post("/login")
                        .param("username", "qiyi")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        HttpSession session = login.getRequest().getSession(false);

        mvc.perform(get("/reviewer/records").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("JY20261019-777")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("下载 PDF")));

        mvc.perform(get("/reviewer/curve")
                        .param("recordId", String.valueOf(record.getId()))
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("称重值趋势图")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("50000.3 mg")));

        mvc.perform(get("/reviewer/export/pdf")
                        .param("recordId", String.valueOf(record.getId()))
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("JY20261019-777.pdf")));
    }
}
