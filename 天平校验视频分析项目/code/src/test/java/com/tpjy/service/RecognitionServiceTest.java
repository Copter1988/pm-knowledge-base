package com.tpjy.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.tpjy.dto.AlertItem;
import com.tpjy.dto.CardOcrResult;
import com.tpjy.dto.RecognitionResult;
import com.tpjy.dto.RecognizeOptions;
import com.tpjy.dto.AutoReading;
import com.tpjy.dto.RecognizedReading;
import com.tpjy.service.frame.Frame;
import com.tpjy.service.frame.FrameExtractor;
import com.tpjy.service.ocr.CardOcrService;
import com.tpjy.service.ocr.NoopOcrService;
import com.tpjy.service.qr.QrDecoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 视频识别链测试（对应 AC-02 / AC-03）。
 *
 * <p>不依赖 ffmpeg 与真实视频：用 ZXing 现场生成带人员卡/天平卡的图片帧，
 * 再注入一个「假抽帧器」驱动 {@link RecognitionService}，
 * 从而把「抽帧 → 解码 → 查对 → 预填 → 提示」整条链验证完整。
 * 真实 ffmpeg 调用留给命令行在装好 ffmpeg 的机器上验证。
 */
class RecognitionServiceTest {

    /** 假抽帧器：直接返回预先造好的帧。 */
    private static class StubFrameExtractor implements FrameExtractor {
        private final List<Frame> frames;

        StubFrameExtractor(List<Frame> frames) {
            this.frames = frames;
        }

        @Override
        public List<Frame> extract(File video, File outputDir) {
            return frames;
        }

        @Override
        public String name() {
            return "stub";
        }
    }

    private static class StubCardOcrService implements CardOcrService {
        @Override
        public CardOcrResult recognize(List<Frame> frames, List<String> knownBalanceCodes,
                                       String expectedPersonName, RecognizeOptions options) {
            CardOcrResult result = new CardOcrResult();
            result.setAvailable(true);
            result.setMethod("stub");
            result.setBalanceCode("F002");
            result.setRawText("F002");
            return result;
        }

        @Override
        public String name() {
            return "stub-card-ocr";
        }
    }

    private static class AutoReadingCardOcrService implements CardOcrService {
        @Override
        public CardOcrResult recognize(List<Frame> frames, List<String> knownBalanceCodes,
                                       String expectedPersonName, RecognizeOptions options) {
            CardOcrResult result = new CardOcrResult();
            result.setAvailable(true);
            result.setMethod("stub-auto-reading");
            result.setBalanceCode("F001");
            result.setRawText("OCR auto readings");
            String[] values = new String[]{
                    "200.0006", "199.9998", "200.0011",
                    "50.0003", "49.9997", "50.0002",
                    "10.0001", "10.0000", "9.9999"
            };
            for (int i = 0; i < values.length; i++) {
                AutoReading reading = new AutoReading();
                reading.setSeq(i + 1);
                reading.setLevel(i < 3 ? "L1" : (i < 6 ? "L2" : "L3"));
                reading.setRaw(values[i]);
                reading.setUnit("g");
                reading.setGrams(new BigDecimal(values[i]));
                reading.setSecond(i);
                reading.setConfidence(0.96);
                reading.setStableSamples(5);
                reading.setStableSeconds(4);
                result.getAutoReadings().add(reading);
            }
            return result;
        }

        @Override
        public String name() {
            return "stub-auto-reading";
        }
    }

    private static class CheckerCardOcrService implements CardOcrService {
        private final String checkerCardCode;

        CheckerCardOcrService(String checkerCardCode) {
            this.checkerCardCode = checkerCardCode;
        }

        @Override
        public CardOcrResult recognize(List<Frame> frames, List<String> knownBalanceCodes,
                                       String expectedPersonName, RecognizeOptions options) {
            CardOcrResult result = new CardOcrResult();
            result.setAvailable(true);
            result.setMethod("stub-checker-card");
            result.setBalanceCode("F001");
            result.setCheckerCardCode(checkerCardCode);
            result.setCheckerCardConfidence(0.98);
            result.setRawText("F001 | " + checkerCardCode
                    + " | d=0.01mg(41g)0.1mg(120g)");
            result.setNameplateRange1Precision("0.1mg");
            result.setNameplateRange1Capacity(new BigDecimal("120"));
            result.setNameplateRange2Precision("0.01mg");
            result.setNameplateRange2Capacity(new BigDecimal("41"));
            result.setNameplateConfidence(0.95);
            return result;
        }

        @Override
        public String name() {
            return "stub-checker-card";
        }
    }

    private static QrDecoder decoder = new QrDecoder();

    @Test
    @DisplayName("二维码解码：能读出人员卡与天平卡（含中文姓名）")
    void decodeQr(@TempDir Path tmp) throws Exception {
        File img = writeFrame(tmp.toFile(), "PERSON:李一", "BALANCE:F001", 0);
        List<String> texts = decoder.decode(img);
        assertEquals("李一", decoder.pickByPrefix(texts, "PERSON:"));
        assertEquals("F001", decoder.pickByPrefix(texts, "BALANCE:"));
    }

    @Test
    @DisplayName("正常视频：人员与天平都对得上，无告警，识别耗时被记录")
    void happyPath(@TempDir Path tmp) throws Exception {
        RecognitionResult r = recognize(tmp, "李一", "李一", "F001", "F001", Arrays.asList("F001", "F002"), null, -1);

        assertEquals("李一", r.getPersonFromQr());
        assertEquals("F001", r.getBalanceFromQr());
        assertFalse(r.hasAlert("PERSON_MISMATCH"));
        assertFalse(r.hasAlert("BALANCE_NOT_FOUND"));
        assertTrue(r.canCreateRecord(), "人员与天平均已确定时允许建记录");
        assertTrue(r.getFrameCount() > 0);
        assertTrue(r.getElapsedMillis() >= 0);
        // Noop OCR：9 次示值都拿不到 → 应提示人工补录
        assertTrue(r.hasAlert("OCR_MISSED"));
        assertEquals(9, r.getReadings().size());
    }

    @Test
    @DisplayName("AC-03：拍摄人员与登录账号不一致 → 不予记录并提示")
    void personMismatch(@TempDir Path tmp) throws Exception {
        RecognitionResult r = recognize(tmp, "李二", "李一", "F001", "F001", Arrays.asList("F001", "F002"), null, -1);

        assertTrue(r.hasAlert("PERSON_MISMATCH"), "应提示人员不一致");
        assertFalse(r.canCreateRecord(), "人员与登录账号不一致时必须阻断建记录");
        AlertItem a = null;
        for (AlertItem item : r.getAlerts()) {
            if ("PERSON_MISMATCH".equals(item.getType())) {
                a = item;
            }
        }
        assertNotNull(a);
        assertTrue(a.getMessage().contains("李二"));
        assertNotNull(a.getClock(), "提示应带视频时间点，供一键定位");
    }

    @Test
    @DisplayName("AC-03：天平未登记 → 提示")
    void balanceNotRegistered(@TempDir Path tmp) throws Exception {
        RecognitionResult r = recognize(tmp, "李一", "李一", "F009", "F009", Arrays.asList("F001", "F002"), null, -1);
        assertTrue(r.hasAlert("BALANCE_NOT_FOUND"), "应提示天平未登记");
    }

    @Test
    @DisplayName("人工补录 9 次示值后：读数齐备，且位数不符会提示（精度 0.1mg 应 4 位）")
    void manualValuesAndDigitCheck(@TempDir Path tmp) throws Exception {
        List<String> values = Arrays.asList(
                "200.0006", "199.9998", "200.0011",
                "50.0003", "49.9997", "50.0002",
                "10.001", "10.0000", "9.9999"); // 第 7 次只有 3 位小数 → 位数不符
        RecognizeOptions opt = options(tmp, "李一", "F001", Arrays.asList("F001"));
        opt.setManualValues(values);
        opt.setExpectedDigits(4);
        opt.setPrecisionText("0.1mg");
        opt.setWeights(Arrays.asList(new BigDecimal("200"), new BigDecimal("50"), new BigDecimal("10")));

        RecognitionResult r = new RecognitionService(
                new StubFrameExtractor(frames(tmp, "李一", "F001")), new NoopOcrService(), 1)
                .recognize(new File("stub.mp4"), opt);

        assertTrue(r.allReadingsPresent(), "人工补录后 9 次示值应齐备");
        assertTrue(r.hasAlert("DIGIT_MISMATCH"), "10.001 只有 3 位小数，应提示位数不符");
        boolean levelOk = true;
        int i = 1;
        for (RecognizedReading rd : r.getReadings()) {
            if (rd.getNominal() == null) {
                levelOk = false;
            }
            assertEquals(i, rd.getSeq());
            i++;
        }
        assertTrue(levelOk, "应已按 L1/L2/L3 回填砝码标称值");
    }

    @Test
    @DisplayName("双量程：L1 按第一量程 0.1mg（4 位），L2/L3 按第二量程 0.01mg（5 位）")
    void dualRangeDigits(@TempDir Path tmp) throws Exception {
        List<String> values = Arrays.asList(
                "200.0006", "199.9998", "200.0011",
                "50.00031", "49.99971", "50.00021",
                "10.00001", "10.00000", "9.99991");
        RecognizeOptions opt = options(tmp, "李一", "F001", Arrays.asList("F001"));
        opt.setManualValues(values);
        opt.applyRangePrecisions("0.1mg", "0.01mg");
        opt.setWeights(Arrays.asList(new BigDecimal("200"), new BigDecimal("50"), new BigDecimal("10")));

        RecognitionResult r = new RecognitionService(
                new StubFrameExtractor(frames(tmp, "李一", "F001")), new NoopOcrService(), 1)
                .recognize(new File("stub.mp4"), opt);

        assertFalse(r.hasAlert("DIGIT_MISMATCH"), "L1 4 位、L2/L3 5 位应全部符合");

        opt.setManualValues(Arrays.asList(
                "200.0006", "199.9998", "200.0011",
                "50.0003", "49.9997", "50.0002",
                "10.0001", "10.0000", "9.9999"));
        RecognitionResult bad = new RecognitionService(
                new StubFrameExtractor(frames(tmp, "李一", "F001")), new NoopOcrService(), 1)
                .recognize(new File("stub.mp4"), opt);
        assertTrue(bad.hasAlert("DIGIT_MISMATCH"), "L2/L3 仍按 4 位填写时应提示位数不符");
    }

    @Test
    @DisplayName("mg 显示保留实际单位，按 mg 小数位校验，同时换算为 g")
    void milligramDisplay(@TempDir Path tmp) throws Exception {
        RecognizeOptions opt = options(tmp, "李一", "F001", Arrays.asList("F001"));
        opt.applyRangePrecisions("0.1mg", null);
        opt.setWeights(Arrays.asList(new BigDecimal("200"), new BigDecimal("50"), new BigDecimal("10")));
        opt.setCheckDate(new java.util.Date(1782699300000L));
        opt.setManualValues(Arrays.asList(
                "200000.8mg", "199999.8mg", "200001.1mg",
                "50000.3mg", "49999.7mg", "50000.2mg",
                "10000.1mg", "10000.0mg", "9999.9mg"));

        RecognitionResult r = new RecognitionService(
                new StubFrameExtractor(frames(tmp, "李一", "F001")), new NoopOcrService(), 1)
                .recognize(new File("stub.mp4"), opt);

        assertEquals("mg", r.getReadings().get(0).getDisplayUnit());
        assertEquals("200000.8", r.getReadings().get(0).getRawValue());
        assertEquals(0, new BigDecimal("200.0008").compareTo(r.getReadings().get(0).getValue()));
        assertFalse(r.hasAlert("DIGIT_MISMATCH"), "200000.8 mg 是 1 位小数，应符合 0.1mg 精度");
        assertNotNull(r.getReadings().get(0).getMeasureTime(), "应把帧时间换算为测量时间");
    }

    @Test
    @DisplayName("双量程精度按砝码实际落入的量程选择，并检查超量程")
    void precisionByNominalCapacity(@TempDir Path tmp) throws Exception {
        RecognizeOptions opt = options(tmp, "李一", "F002", Arrays.asList("F001", "F002"));
        opt.setWeights(Arrays.asList(new BigDecimal("100"), new BigDecimal("20"), new BigDecimal("2")));
        opt.setRange1CapacityGrams(new BigDecimal("120"));
        opt.setRange2CapacityGrams(new BigDecimal("4"));
        opt.applyRangePrecisions("0.1mg", "0.01mg");

        assertEquals(4, opt.expectedDigitsForLevel("L1", "g"));
        assertEquals(4, opt.expectedDigitsForLevel("L2", "g"));
        assertEquals(5, opt.expectedDigitsForLevel("L3", "g"));
        assertEquals(1, opt.expectedDigitsForLevel("L1", "mg"));
        assertEquals(1, opt.expectedDigitsForLevel("L2", "mg"));
        assertEquals(2, opt.expectedDigitsForLevel("L3", "mg"));

        RecognizeOptions over = options(tmp, "李一", "F002", Arrays.asList("F001", "F002"));
        over.setWeights(Arrays.asList(new BigDecimal("200"), new BigDecimal("50"), new BigDecimal("10")));
        over.setRange1CapacityGrams(new BigDecimal("120"));
        RecognitionResult result = new RecognitionService(
                new StubFrameExtractor(frames(tmp, "李一", "F002")), new NoopOcrService(), 1)
                .recognize(new File("stub.mp4"), over);
        assertTrue(result.hasAlert("RANGE_MISMATCH"), "200g 砝码超过 F002 的 120g 第一量程应提示");
    }

    @Test
    @DisplayName("二维码缺失时用文字卡 OCR 识别天平编号")
    void textCardFallback(@TempDir Path tmp) throws Exception {
        RecognizeOptions opt = options(tmp, "李一", null, Arrays.asList("F001", "F002"));
        List<Frame> plain = new ArrayList<Frame>();
        for (int i = 0; i < 3; i++) {
            BufferedImage img = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 300, 300);
            g.dispose();
            File file = new File(tmp.toFile(), "plain_" + i + ".jpg");
            ImageIO.write(img, "jpg", file);
            plain.add(new Frame(i, file));
        }

        RecognitionResult r = new RecognitionService(new StubFrameExtractor(plain),
                new NoopOcrService(), 1, new StubCardOcrService())
                .recognize(new File("stub.mp4"), opt);

        assertEquals("F002", r.getBalanceFromQr());
        assertEquals("F002", r.getEffectiveBalanceCode());
        assertTrue(r.getIdentityMethod().contains("文字卡"));
    }

    @Test
    @DisplayName("稳定示值 OCR 结果自动预填 9 次读数并保留原屏单位")
    void automaticReadingsArePrefilled(@TempDir Path tmp) throws Exception {
        RecognizeOptions opt = options(tmp, "李一", "F001", Arrays.asList("F001"));
        opt.setWeights(Arrays.asList(
                new BigDecimal("200"), new BigDecimal("50"), new BigDecimal("10")));
        opt.setCheckDate(new java.util.Date(1782699300000L));

        RecognitionResult r = new RecognitionService(
                new StubFrameExtractor(frames(tmp, "李一", "F001")),
                new NoopOcrService(), 1, new AutoReadingCardOcrService())
                .recognize(new File("stub.mp4"), opt);

        assertTrue(r.allReadingsPresent(), "自动 OCR 应预填全部 9 次示值");
        assertFalse(r.hasAlert("OCR_MISSED"), "自动预填成功后不应再提示示值缺失");
        assertTrue(r.hasAlert("AUTO_READING_OCR"));
        assertEquals("200.0006", r.getReadings().get(0).getRawValue());
        assertEquals("g", r.getReadings().get(0).getDisplayUnit());
        assertTrue(r.getReadings().get(8).isAutoRecognized());
        assertNotNull(r.getReadings().get(0).getMeasureTime());
    }

    @Test
    @DisplayName("校验员实体卡与登录账号一致时允许继续")
    void checkerCardMatchesLoginUser(@TempDir Path tmp) throws Exception {
        RecognizeOptions opt = options(tmp, "李一", "F001", Arrays.asList("F001"));
        opt.setExpectedCheckerCardCode("JY001");
        opt.setWeights(Arrays.asList(
                new BigDecimal("200"), new BigDecimal("50"), new BigDecimal("10")));

        RecognitionResult r = new RecognitionService(
                new StubFrameExtractor(frames(tmp, "李一", "F001")),
                new NoopOcrService(), 1, new CheckerCardOcrService("JY001"))
                .recognize(new File("stub.mp4"), opt);

        assertEquals(Boolean.TRUE, r.getCheckerCardMatched());
        assertTrue(r.hasAlert("CHECKER_CARD_MATCH"));
        assertTrue(r.canCreateRecord());
    }

    @Test
    @DisplayName("校验员实体卡与登录账号不一致时阻断记录")
    void checkerCardMismatchBlocksRecord(@TempDir Path tmp) throws Exception {
        RecognizeOptions opt = options(tmp, "李一", "F001", Arrays.asList("F001"));
        opt.setExpectedCheckerCardCode("JY001");
        opt.setWeights(Arrays.asList(
                new BigDecimal("200"), new BigDecimal("50"), new BigDecimal("10")));

        RecognitionResult r = new RecognitionService(
                new StubFrameExtractor(frames(tmp, "李一", "F001")),
                new NoopOcrService(), 1, new CheckerCardOcrService("JY002"))
                .recognize(new File("stub.mp4"), opt);

        assertEquals(Boolean.FALSE, r.getCheckerCardMatched());
        assertTrue(r.hasAlert("CHECKER_MISMATCH"));
        assertFalse(r.canCreateRecord());
    }

    @Test
    @DisplayName("铭牌双量程可读时与数据库比对并采用铭牌精度")
    void nameplatePrecisionOverridesDatabaseAndWarnsMismatch(@TempDir Path tmp) throws Exception {
        RecognizeOptions opt = options(tmp, "李一", "F001", Arrays.asList("F001"));
        opt.setExpectedCheckerCardCode("JY001");
        opt.setWeights(Arrays.asList(
                new BigDecimal("100"), new BigDecimal("20"), new BigDecimal("2")));
        opt.setRange1CapacityGrams(new BigDecimal("120"));
        opt.setRange2CapacityGrams(new BigDecimal("41"));
        opt.setDatabaseRange1Precision("0.1mg");
        opt.setDatabaseRange2Precision("0.1mg");
        opt.setDatabaseRange1CapacityGrams(new BigDecimal("120"));
        opt.setDatabaseRange2CapacityGrams(new BigDecimal("41"));
        opt.applyRangePrecisions("0.1mg", "0.1mg");

        RecognitionResult r = new RecognitionService(
                new StubFrameExtractor(frames(tmp, "李一", "F001")),
                new NoopOcrService(), 1, new CheckerCardOcrService("JY001"))
                .recognize(new File("stub.mp4"), opt);

        assertTrue(r.hasAlert("BALANCE_NAMEPLATE_MISMATCH"));
        assertEquals("天平铭牌 OCR", r.getPrecisionSource());
        assertEquals(5, opt.expectedDigitsForLevel("L2", "g"));
    }

    // ---------------- 辅助 ----------------

    private RecognitionResult recognize(Path tmp, String qrPerson, String loginPerson,
                                        String qrBalance, String selectedBalance,
                                        List<String> known, List<String> manual, int digits) throws Exception {
        RecognizeOptions opt = options(tmp, loginPerson, selectedBalance, known);
        opt.setExpectedDigits(digits);
        if (manual != null) {
            opt.setManualValues(manual);
        }
        return new RecognitionService(new StubFrameExtractor(frames(tmp, qrPerson, qrBalance)),
                new NoopOcrService(), 1).recognize(new File("stub.mp4"), opt);
    }

    private RecognizeOptions options(Path tmp, String person, String balance, List<String> known) {
        RecognizeOptions opt = new RecognizeOptions();
        // 页面/命令行传入的「当前登录人员」与「选定天平」：这里刻意与二维码内容分开传
        opt.setPersonName(person);
        opt.setBalanceCode(balance);
        opt.setKnownBalanceCodes(known);
        opt.setVideoNo("V-20260629-0043");
        opt.setFrameOutputDir(tmp.toFile());
        return opt;
    }

    /** 造 3 帧：第 1 帧同时含人员卡与天平卡，后两帧只是画面延续（用来验证时间点映射）。 */
    private List<Frame> frames(Path tmp, String personInQr, String balanceInQr) throws Exception {
        List<Frame> list = new ArrayList<Frame>();
        for (int i = 0; i < 3; i++) {
            list.add(new Frame(i, writeFrame(tmp.toFile(), "PERSON:" + personInQr, "BALANCE:" + balanceInQr, i)));
        }
        return list;
    }

    private static File writeFrame(File dir, String personText, String balanceText, int index) throws Exception {
        int w = 900;
        int h = 500;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        // 人员卡（左上）
        g.drawImage(qr(personText, 260), 30, 30, null);
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString(personText, 30, 330);
        // 天平卡（右侧）
        g.drawImage(qr(balanceText, 260), 560, 30, null);
        g.drawString(balanceText, 560, 330);
        // 模拟天平示值区域
        g.setFont(new Font("SansSerif", Font.PLAIN, 28));
        g.drawString("0.0000 g", 400, 430);
        g.dispose();

        File file = new File(dir, String.format("frame_%05d.jpg", index + 1));
        ImageIO.write(img, "jpg", file);
        return file;
    }

    private static BufferedImage qr(String text, int size) throws WriterException {
        Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }
}
