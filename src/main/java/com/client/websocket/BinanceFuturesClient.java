package com.client.websocket;

import com.TickEventProcessor;
import com.binance.connector.client.impl.WebsocketClientImpl;
import com.binance.connector.client.utils.WebSocketCallback;
import com.model.CcyPair;
import com.model.Product;
import com.model.Tick;
import org.apache.commons.math3.util.Precision;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.Instant;

public class BinanceFuturesClient implements Client {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CcyPair ccyPair;
    private final TickEventProcessor tickEventProcessor;

    public BinanceFuturesClient(CcyPair ccyPair, TickEventProcessor tickEventProcessor){
        this.ccyPair = ccyPair;
        this.tickEventProcessor = tickEventProcessor;
    }

    @Override
    public void connect() {

        WebsocketClientImpl client = new WebsocketClientImpl("wss://fstream.binance.com");

        LOGGER.info("Connected to Binance Futures.");

        client.bookTicker(ccyPair.getCcy1() + ccyPair.getCcy2(), new WebSocketCallback() {
            private  double lastBid = 0;
            private  double lastAsk = 0;
            private  double lastBidSize = 0;
            private  double lastAskSize = 0;

            @Override
            public void onReceive(String message) {
                JSONObject json = new JSONObject(message);

                double bid = json.getDouble("b");
                double ask = json.getDouble("a");
                double bidSize = json.getDouble("B");
                double askSize = json.getDouble("A");

                if(!Precision.equals(lastBid, bid) ||
                    !Precision.equals(lastAsk, ask) ||
                    !Precision.equals(lastBidSize, bidSize) ||
                    !Precision.equals(lastAskSize, askSize)) {

                    Tick tick = new Tick.Builder()
                            .exchange("binance")
                            .product(Product.Future)
                            .timestamp(Instant.now())
                            .bid(bid)
                            .ask(ask)
                            .bidSize(bidSize)
                            .askSize(askSize)
                            .build();

                    lastBid = bid;
                    lastAsk = ask;
                    lastBidSize = bidSize;
                    lastAskSize = askSize;

                    tickEventProcessor.publishTick(tick);
                }
            }
        });
    }
}