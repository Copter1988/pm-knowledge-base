package com.tpjy.service.frame;

import java.io.File;

/**
 * 抽取出来的一帧：第几秒 + 图片文件。
 * 时间用于回填「各次示值测量时间」（分钟级）与「定位到视频 mm:ss」。
 */
public class Frame {

    private final int second;
    private final File file;

    public Frame(int second, File file) {
        this.second = second;
        this.file = file;
    }

    public int getSecond() {
        return second;
    }

    public File getFile() {
        return file;
    }

    /** 形如 02:58，供「定位到视频 02:58」使用。 */
    public String getClock() {
        int m = second / 60;
        int s = second % 60;
        return String.format("%02d:%02d", m, s);
    }

    @Override
    public String toString() {
        return getClock() + " -> " + (file == null ? "" : file.getName());
    }
}
