package com.tpjy.service.ocr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpjy.config.AppProps;
import com.tpjy.dto.CardOcrResult;
import com.tpjy.service.frame.Frame;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
    private static final Pattern CODE_PATTERN =
            Pattern.compile("\\b([A-Z]\\d{3,})\\b", Pattern.CASE_INSENSITIVE);

    private final AppProps props;
    private final ObjectMapper mapper;

    public LocalCardOcrService(AppProps props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
    }

    @Override
    public CardOcrResult recognize(List<Frame> frames, List<String> knownBalanceCodes,
                                   String expectedPersonName) {
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
            request.put("knownBalances", knownBalanceCodes == null
                    ? new ArrayList<String>() : knownBalanceCodes);
            request.put("personName", expectedPersonName);
            request.put("maxWidth", 480);

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
            return parse(root, knownBalanceCodes, expectedPersonName);
        } catch (Exception e) {
            System.err.println("文字卡 OCR 失败：" + e.getMessage());
            return CardOcrResult.unavailable();
        }
    }

    private CardOcrResult parse(JsonNode root, List<String> knownBalanceCodes,
                                String expectedPersonName) {
        CardOcrResult result = new CardOcrResult();
        result.setAvailable(true);
        result.setMethod("RapidOCR-local");
        Map<String, Double> balanceScores = new LinkedHashMap<String, Double>();
        StringBuilder allText = new StringBuilder();

        for (JsonNode frame : root.path("frames")) {
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
                Matcher cm = CODE_PATTERN.matcher(compact);
                while (cm.find()) {
                    String code = cm.group(1);
                    if (knownBalanceCodes != null && containsIgnoreCase(knownBalanceCodes, code)) {
                        addBalance(balanceScores, code, score);
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
            }
        }
        result.setRawText(allText.toString());
        double best = Double.NEGATIVE_INFINITY;
        for (Map.Entry<String, Double> e : balanceScores.entrySet()) {
            if (e.getValue() > best) {
                result.setBalanceCode(e.getKey());
                best = e.getValue();
            }
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

    private static void addBalance(Map<String, Double> scores, String code, double score) {
        String normalized = code.toUpperCase(Locale.ROOT);
        Double old = scores.get(normalized);
        scores.put(normalized, old == null ? score : old + score);
    }

    private static boolean containsIgnoreCase(List<String> list, String value) {
        for (String s : list) {
            if (s != null && s.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
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
