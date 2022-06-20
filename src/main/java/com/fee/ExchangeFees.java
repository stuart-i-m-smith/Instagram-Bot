package com.fee;

import java.util.HashMap;
import java.util.Map;

public class ExchangeFees {

    private static final Map<String, Double> EXCHANGE_FEES_MAP = new HashMap<>();

    static{
        EXCHANGE_FEES_MAP.put("Coinbase", .001);
        EXCHANGE_FEES_MAP.put("FTX", .001);
        EXCHANGE_FEES_MAP.put("Kraken", .001);
        EXCHANGE_FEES_MAP.put("Bistamp", .001);
        EXCHANGE_FEES_MAP.put("GateIo", .001);
        EXCHANGE_FEES_MAP.put("Binance", .001);
    }

    public static double getFee(String exchange){
        return EXCHANGE_FEES_MAP.getOrDefault(exchange, 1d);
    }
}