package com.baris.solocircuit.state;

public class FillLimitState {
    private static int fillLimit = 32768; // 默认 32,768，重启后恢复

    public static int getFillLimit() {
        return fillLimit;
    }

    public static void setFillLimit(int value) {
        fillLimit = value;
    }
}
