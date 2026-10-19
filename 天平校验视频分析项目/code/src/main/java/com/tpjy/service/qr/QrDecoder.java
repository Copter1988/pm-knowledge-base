package com.tpjy.service.qr;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ZXing 本地二维码识别（不依赖外部网络，SRS §3.2 现有资源利用性）。
 * 一帧可能同时拍到人员卡与天平卡，故返回一帧内的全部识别结果。
 */
public class QrDecoder {

    /**
     * 识别一张图片中的全部二维码文本（去重）。
     * 依次尝试：原图 → 逆时针旋转 90/180/270 → 反色，提高手机拍摄的容错率。
     */
    public List<String> decode(File image) {
        List<String> texts = new ArrayList<String>();
        BufferedImage img = readImage(image);
        if (img == null) {
            return texts;
        }
        Set<String> hit = new HashSet<String>();
        for (BufferedImage variant : variants(img)) {
            Result result = tryDecode(variant);
            if (result != null && result.getText() != null) {
                String text = result.getText().trim();
                if (!text.isEmpty() && hit.add(text)) {
                    texts.add(text);
                }
            }
        }
        return texts;
    }

    /** 一帧里若识别到人员卡/天平卡，按前缀取出（找不到返回 null）。 */
    public String pickByPrefix(List<String> texts, String prefix) {
        if (texts == null) {
            return null;
        }
        for (String t : texts) {
            String upper = t.toUpperCase();
            if (upper.startsWith(prefix.toUpperCase())) {
                return t.substring(prefix.length()).trim();
            }
        }
        return null;
    }

    private Result tryDecode(BufferedImage img) {
        Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        LuminanceSource source = new BufferedImageLuminanceSource(img);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        MultiFormatReader reader = new MultiFormatReader();
        try {
            return reader.decode(bitmap, hints);
        } catch (NotFoundException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private List<BufferedImage> variants(BufferedImage src) {
        List<BufferedImage> list = new ArrayList<BufferedImage>();
        list.add(src);
        list.add(rotate(src, 90));
        list.add(rotate(src, 180));
        list.add(rotate(src, 270));
        list.add(invert(src));
        return list;
    }

    private BufferedImage rotate(BufferedImage src, int degree) {
        double rad = Math.toRadians(degree);
        int w = src.getWidth();
        int h = src.getHeight();
        boolean swap = degree == 90 || degree == 270;
        int nw = swap ? h : w;
        int nh = swap ? w : h;
        BufferedImage dst = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        AffineTransform at = new AffineTransform();
        at.translate(nw / 2.0, nh / 2.0);
        at.rotate(rad);
        at.translate(-w / 2.0, -h / 2.0);
        g.drawImage(src, at, null);
        g.dispose();
        return dst;
    }

    private BufferedImage invert(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = 255 - ((rgb >> 16) & 0xFF);
                int gg = 255 - ((rgb >> 8) & 0xFF);
                int b = 255 - (rgb & 0xFF);
                dst.setRGB(x, y, (r << 16) | (gg << 8) | b);
            }
        }
        return dst;
    }

    private BufferedImage readImage(File file) {
        try {
            return ImageIO.read(file);
        } catch (Exception e) {
            return null;
        }
    }
}
