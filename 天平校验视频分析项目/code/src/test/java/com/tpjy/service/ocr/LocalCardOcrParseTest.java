package com.tpjy.service.ocr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpjy.config.AppProps;
import com.tpjy.dto.CardOcrResult;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalCardOcrParseTest {

    @Test
    void extractsBalanceCodeFromNoisyWorkbookText() throws Exception {
        String json = "{\"ok\":true,\"frames\":[{\"texts\":["
                + "{\"text\":\"25DMax120gd=0.01mg(41g)0.1mg(120g)\",\"score\":0.95},"
                + "{\"text\":\"F002\",\"score\":0.94},"
                + "{\"text\":\"JY002\",\"score\":0.90}"
                + "]}]}";
        LocalCardOcrService service = new LocalCardOcrService(AppProps.defaults(), new ObjectMapper());
        CardOcrResult result = service.parse(new ObjectMapper().readTree(json),
                Arrays.asList("F001", "F002"), "李一");

        assertTrue(result.isAvailable());
        assertEquals("F002", result.getBalanceCode());
    }
}
