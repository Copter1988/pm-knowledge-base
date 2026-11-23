package com.tpjy.service.ocr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpjy.config.AppProps;
import com.tpjy.dto.AutoReading;
import com.tpjy.dto.CardOcrResult;
import com.tpjy.dto.RecognizeOptions;
import com.tpjy.service.frame.Frame;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 本地中文 OCR 适配器：Java 通过子进程调用 RapidOCR。
 *
 * <p>只扫描视频前若干秒的少量帧，专门识别人员/天平文字卡，避免对整段视频逐帧 OCR 造成性能不可控。
 */
public class LocalCardOcrService implements CardOcrService {

    private static final Pattern BALANCE_PATTERN =
            Pattern.compile("BALANCE\\s*[:：]?\\s*([A-Z]\\d{3,})", Pattern.CASE_INSENSITIVE);
    private static final Pattern PERSON_PATTERN =
            Pattern.compile("PERSON\\s*[:：]?\\s*([^|\\s]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BALANCE_CODE_PATTERN =
            Pattern.compile("\\b(F\\d{3,})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHECKER_CODE_PATTERN =
            Pattern.compile("\\bJY([0-9OQDISBZL]{3,})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAMEPLATE_PAIR_PATTERN =
            Pattern.compile("([0-9]+(?:\\.[0-9]+)?)MG[\\(（\\[]?([0-9]+(?:\\.[0-9]+)?)G");

    private final AppProps props;
    private final ObjectMapper mapper;

    public LocalCardOcrService(AppProps props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
    }

    @Override
    public CardOcrResult recognize(List<Frame> frames, List<String> knownBalanceCodes,
                                   String expectedPersonName, RecognizeOptions options) {
        if (!props.isCardOcrEnabled() || frames == null || frames.isEmpty()) {
            return CardOcrResult.unavailable();
        }
        File python = new File(props.getOcrPythonPath());
        File worker = new File(props.getOcrWorkerPath());
        if (!python.isFile() || !worker.isFile()) {
            return CardOcrResult.unavailable();
        }

        List<Frame> selected = selectFrames(frames);
        try {
            Map<String, Object> request = new LinkedHashMap<String, Object>();
            List<Map<String, Object>> images = new ArrayList<Map<String, Object>>();
            for (Frame frame : selected) {
                Map<String, Object> item = new LinkedHashMap<String, Object>();
                item.put("path", frame.getFile().getAbsolutePath());
                item.put("second", frame.getSecond());
                images.add(item);
            }
            request.put("images", images);
            request.put("readingImages", readingImages(selectReadingFrames(frames)));
            request.put("knownBalances", knownBalanceCodes == null
                    ? new ArrayList<String>() : knownBalanceCodes);
            request.put("personName", expectedPersonName);
            request.put("expectedCheckerCardCode",
                    options == null ? null : options.getExpectedCheckerCardCode());
            request.put("maxWidth", 480);
            Map<String, Object> roi = new LinkedHashMap<String, Object>();
            roi.put("x", 0.14);
            roi.put("y", 0.665);
            roi.put("width", 0.62);
            roi.put("height", 0.095);
            request.put("readingRoi", roi);
            Map<String, Object> screenRoi = new LinkedHashMap<String, Object>();
            screenRoi.put("x", 0.05);
            screenRoi.put("y", 0.64);
            screenRoi.put("width", 0.83);
            screenRoi.put("height", 0.13);
            request.put("screenRoi", screenRoi);
            request.put("levels", levels(options));

            String json = mapper.writeValueAsString(request);
            ProcessBuilder pb = new ProcessBuilder(python.getAbsolutePath(), worker.getAbsolutePath());
            pb.redirectErrorStream(true);
            pb.environment().put("PYTHONIOENCODING", "UTF-8");
            Process process = pb.start();
            OutputStream input = process.getOutputStream();
            try {
                input.write(json.getBytes(StandardCharsets.UTF_8));
            } finally {
                input.close();
            }

            FutureTask<String> reader = new FutureTask<String>(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return readAll(process.getInputStream());
                }
            });
            new Thread(reader, "local-card-ocr-reader").start();
            boolean finished = process.waitFor(
                    Math.max(10, props.getCardOcrTimeoutSeconds()), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return CardOcrResult.unavailable();
            }
            String output = reader.get(5, TimeUnit.SECONDS);

            String payload = lastJsonLine(output);
            if (payload == null) {
                return CardOcrResult.unavailable();
            }
            JsonNode root = mapper.readTree(payload);
            if (!root.path("ok").asBoolean(false)) {
                return CardOcrResult.unavailable();
            }
            CardOcrResult result = parse(root, knownBalanceCodes, expectedPersonName);
            result.setAutoReadings(parseReadings(root));
            return result;
        } catch (Exception e) {
            System.err.println("文字卡 OCR 失败：" + e.getMessage());
            return CardOcrResult.unavailable();
        }
    }

    CardOcrResult parse(JsonNode root, List<String> knownBalanceCodes,
                        String expectedPersonName) {
        CardOcrResult result = new CardOcrResult();
        result.setAvailable(true);
        result.setMethod("RapidOCR-local");
        Map<String, Double> balanceScores = new LinkedHashMap<String, Double>();
        Map<String, RangeHit> rangeHits = new LinkedHashMap<String, RangeHit>();
        StringBuilder allText = new StringBuilder();

        for (JsonNode frame : root.path("frames")) {
            Integer second = frame.path("second").isNumber() ? frame.path("second").asInt() : null;
            for (JsonNode item : frame.path("texts")) {
                String text = item.path("text").asText("");
                if (text.isEmpty()) {
                    continue;
                }
                if (allText.length() > 0) {
                    allText.append(" | ");
                }
                allText.append(text);
                double score = item.path("score").asDouble(0);
                String compact = text.toUpperCase(Locale.ROOT).replace(" ", "");

                Matcher bm = BALANCE_PATTERN.matcher(compact);
                if (bm.find()) {
                    addBalance(balanceScores, bm.group(1), score + 1.0);
                }
                Matcher fm = BALANCE_CODE_PATTERN.matcher(compact);
                while (fm.find()) {
                    addBalance(balanceScores, fm.group(1), score);
                }
                Matcher jym = CHECKER_CODE_PATTERN.matcher(compact);
                while (jym.find()) {
                    String code = "JY" + normalizeCodeDigits(jym.group(1));
                    if (score > result.getCheckerCardConfidence()) {
                        result.setCheckerCardCode(code);
                        result.setCheckerCardConfidence(score);
                        result.setCheckerCardSecond(second);
                    }
                }
                Matcher pm = PERSON_PATTERN.matcher(text);
                if (pm.find() && result.getPersonName() == null) {
                    result.setPersonName(pm.group(1).trim());
                }
                if (result.getPersonName() == null && expectedPersonName != null
                        && !expectedPersonName.trim().isEmpty()
                        && text.contains(expectedPersonName.trim())) {
                    result.setPersonName(expectedPersonName.trim());
                }
                collectNameplate(compact, score, second, rangeHits);
            }
        }
        result.setRawText(allText.toString());
        // OCR 常把单独一张卡识别成一行 “F002”；也兼容带空格/噪声的整段文本。
        String flattened = allText.toString().toUpperCase(Locale.ROOT).replace(" ", "");
        if (knownBalanceCodes != null) {
            for (String known : knownBalanceCodes) {
                if (known != null && flattened.contains(known.toUpperCase(Locale.ROOT))) {
                    addBalance(balanceScores, known, 0.1);
                }
            }
        }
        double best = Double.NEGATIVE_INFINITY;
        for (Map.Entry<String, Double> e : balanceScores.entrySet()) {
            if (e.getValue() > best) {
                result.setBalanceCode(e.getKey());
                best = e.getValue();
            }
        }
        applyNameplateRanges(result, rangeHits);
        return result;
    }

    private static void collectNameplate(String compact, double score, Integer second,
                                         Map<String, RangeHit> hits) {
        if (score < 0.55 || !compact.contains("MG")) {
            return;
        }
        Matcher matcher = NAMEPLATE_PAIR_PATTERN.matcher(compact);
        while (matcher.find()) {
            try {
                String precision = new BigDecimal(matcher.group(1)).stripTrailingZeros().toPlainString() + "mg";
                BigDecimal capacity = new BigDecimal(matcher.group(2));
                if (capacity.signum() <= 0) {
                    continue;
                }
                String key = precision + "|" + capacity.stripTrailingZeros().toPlainString();
                RangeHit old = hits.get(key);
                if (old == null) {
                    hits.put(key, new RangeHit(precision, capacity, score, second));
                } else {
                    old.observe(score, second);
                }
            } catch (NumberFormatException ignore) {
                // OCR 噪声，继续扫描同一行中的其他量程对。
            }
        }
    }

    private static void applyNameplateRanges(CardOcrResult result, Map<String, RangeHit> hits) {
        if (hits.isEmpty()) {
            return;
        }
        List<RangeHit> candidates = new ArrayList<RangeHit>(hits.values());
        List<RangeHit> repeated = new ArrayList<RangeHit>();
        for (RangeHit hit : candidates) {
            if (hit.count >= 2) {
                repeated.add(hit);
            }
        }
        List<RangeHit> ordered = repeated.size() >= 2
                ? repeated : new ArrayList<RangeHit>(candidates);
        Collections.sort(ordered, new Comparator<RangeHit>() {
            @Override
            public int compare(RangeHit left, RangeHit right) {
                int byCount = Integer.compare(right.count, left.count);
                return byCount != 0 ? byCount : Double.compare(right.score, left.score);
            }
        });
        if (ordered.size() > 2) {
            ordered = new ArrayList<RangeHit>(ordered.subList(0, 2));
        }
        Collections.sort(ordered, new Comparator<RangeHit>() {
            @Override
            public int compare(RangeHit left, RangeHit right) {
                return left.capacity.compareTo(right.capacity);
            }
        });
        RangeHit best = Collections.max(ordered, new Comparator<RangeHit>() {
            @Override
            public int compare(RangeHit left, RangeHit right) {
                return Double.compare(left.score, right.score);
            }
        });
        result.setNameplateConfidence(best.score);
        result.setNameplateSecond(best.second);
        if (ordered.size() == 1) {
            RangeHit only = ordered.get(0);
            result.setNameplateRange1Precision(only.precision);
            result.setNameplateRange1Capacity(only.capacity);
            return;
        }
        RangeHit first = ordered.get(ordered.size() - 1);
        RangeHit second = ordered.get(0);
        result.setNameplateRange1Precision(first.precision);
        result.setNameplateRange1Capacity(first.capacity);
        result.setNameplateRange2Precision(second.precision);
        result.setNameplateRange2Capacity(second.capacity);
    }

    private static String normalizeCodeDigits(String value) {
        return value.toUpperCase(Locale.ROOT)
                .replace('O', '0').replace('Q', '0').replace('D', '0')
                .replace('I', '1').replace('L', '1')
                .replace('Z', '2').replace('S', '5').replace('B', '8');
    }

    private static final class RangeHit {
        private final String precision;
        private final BigDecimal capacity;
        private double score;
        private Integer second;
        private int count;

        private RangeHit(String precision, BigDecimal capacity, double score, Integer second) {
            this.precision = precision;
            this.capacity = capacity;
            this.score = score;
            this.second = second;
            this.count = 1;
        }

        private void observe(double score, Integer second) {
            count++;
            if (score > this.score) {
                this.score = score;
                this.second = second;
            }
        }
    }

    private List<AutoReading> parseReadings(JsonNode root) {
        List<AutoReading> list = new ArrayList<AutoReading>();
        for (JsonNode item : root.path("readings")) {
            AutoReading r = new AutoReading();
            r.setSeq(item.path("seq").asInt());
            r.setLevel(item.path("level").asText());
            r.setRaw(item.path("raw").asText());
            r.setUnit(item.path("unit").asText());
            r.setGrams(new BigDecimal(item.path("grams").asText("0")));
            r.setSecond(item.path("second").isMissingNode() || item.path("second").isNull()
                    ? null : item.path("second").asInt());
            r.setConfidence(item.path("confidence").asDouble());
            r.setStableSamples(item.path("stableSamples").asInt());
            r.setStableSeconds(item.path("stableSeconds").asInt());
            list.add(r);
        }
        return list;
    }

    private List<Map<String, Object>> levels(RecognizeOptions options) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (options == null || options.getWeights() == null || options.getWeights().size() < 3) {
            return result;
        }
        String[] names = new String[]{"L1", "L2", "L3"};
        for (int i = 0; i < 3; i++) {
            BigDecimal nominal = options.getWeights().get(i);
            if (nominal == null) {
                continue;
            }
            Map<String, Object> level = new LinkedHashMap<String, Object>();
            level.put("level", names[i]);
            level.put("nominal", nominal.toPlainString());
            level.put("digitsG", Math.max(0, options.expectedDigitsForLevel(names[i], "g")));
            level.put("digitsMg", Math.max(0, options.expectedDigitsForLevel(names[i], "mg")));
            result.add(level);
        }
        return result;
    }

    private List<Map<String, Object>> readingImages(List<Frame> frames) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Frame frame : frames) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("path", frame.getFile().getAbsolutePath());
            item.put("second", frame.getSecond());
            result.add(item);
        }
        return result;
    }

    private List<Frame> selectFrames(List<Frame> frames) {
        List<Frame> window = new ArrayList<Frame>();
        for (Frame f : frames) {
            if (f.getSecond() <= props.getCardOcrWindowSeconds()) {
                window.add(f);
            }
        }
        if (window.isEmpty()) {
            window = frames;
        }
        int max = Math.max(1, props.getCardOcrMaxFrames());
        if (window.size() <= max) {
            return window;
        }
        List<Frame> selected = new ArrayList<Frame>();
        int step = Math.max(1, window.size() / max);
        for (int i = 0; i < window.size() && selected.size() < max; i += step) {
            selected.add(window.get(i));
        }
        return selected;
    }

    private List<Frame> selectReadingFrames(List<Frame> frames) {
        int max = Math.max(1, props.getNumericOcrMaxFrames());
        if (frames.size() <= max) {
            return frames;
        }
        List<Frame> selected = new ArrayList<Frame>();
        int step = (int) Math.ceil(frames.size() / (double) max);
        for (int i = 0; i < frames.size() && selected.size() < max; i += step) {
            selected.add(frames.get(i));
        }
        return selected;
    }

    private static void addBalance(Map<String, Double> scores, String code, double score) {
        String normalized = code.toUpperCase(Locale.ROOT);
        Double old = scores.get(normalized);
        scores.put(normalized, old == null ? score : old + score);
    }

    private static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String lastJsonLine(String output) {
        String[] lines = output.split("\\r?\\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.startsWith("{") && line.endsWith("}")) {
                return line;
            }
        }
        return null;
    }

    @Override
    public String name() {
        return "RapidOCR-local";
    }
}
