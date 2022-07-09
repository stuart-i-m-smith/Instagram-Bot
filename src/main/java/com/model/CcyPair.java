package com.model;

public class CcyPair {

    private final String ccy1;
    private final String ccy2;

    public CcyPair(String ccy1, String ccy2) {
        this.ccy1 = ccy1;
        this.ccy2 = ccy2;
    }

    public String getCcy1() {
        return ccy1;
    }

    public String getCcy2() {
        return ccy2;
    }

    @Override
    public String toString() {
        return ccy1 + ccy2;
    }
}
