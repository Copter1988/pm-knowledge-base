package com.tpjy.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;

/** JSON 输出（命令行测试用；Jackson 由 spring-boot-starter-web 提供）。 */
public final class Json {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private Json() {
    }

    public static String toJson(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 序列化失败：" + e.getMessage(), e);
        }
    }

    public static void write(File file, Object o) {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            MAPPER.writeValue(file, o);
        } catch (Exception e) {
            throw new IllegalStateException("写入 JSON 失败：" + file + "，" + e.getMessage(), e);
        }
    }
}
