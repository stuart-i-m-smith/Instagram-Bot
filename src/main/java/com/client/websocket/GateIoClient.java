package com.client.websocket;

import com.TickEventProcessor;
import com.model.Tick;
import org.apache.commons.math3.util.Precision;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class GateIoClient implements Client {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String currency;
    private final TickEventProcessor tickEventProcessor;

    public GateIoClient(String currency,
                        TickEventProcessor tickEventProcessor){
        this.currency = currency;
        this.tickEventProcessor = tickEventProcessor;
    }

    @Override
    public void connect() {
        try {
            WebSocketClient client = new WebSocketClient(new URI("wss://ws.gate.io/v3/")) {

                private  double lastBid = 0;
                private  double lastAsk = 0;
                private  double lastBidSize = 0;
                private  double lastAskSize = 0;

                @Override
                public void onMessage(String message) {
                    JSONObject json = new JSONObject(message);

                    if(json.has("method") && "depth.update".equals(json.getString("method"))){
                        JSONArray params = json.getJSONArray("params");
                        JSONObject topOfBook = params.getJSONObject(1);

                        if(topOfBook.has("bids") || topOfBook.has("asks")){
                            Tick.Builder tickBuilder = new Tick.Builder()
                                .exchange("gateio");

                            if(topOfBook.has("current")) {
                                tickBuilder.timestamp(Instant.ofEpochMilli(((Number) topOfBook.getDouble("current")).longValue() * 1000));
                            }else{
                                tickBuilder.timestamp(Instant.now());
                            }

                            boolean isUpdated = false;

                            if(topOfBook.has("bids")){
                                double bid = topOfBook.getJSONArray("bids")
                                        .getJSONArray(0)
                                        .getDouble(0);
                                double bidSize = topOfBook.getJSONArray("bids")
                                        .getJSONArray(0)
                                        .getDouble(1);

                                if(!Precision.equals(lastBid, bid) || !Precision.equals(lastBidSize, bidSize)) {
                                    lastBid = bid;
                                    lastBidSize = bidSize;
                                    tickBuilder.bid(bid);
                                    tickBuilder.bidSize(bidSize);
                                    isUpdated = true;
                                }else{
                                    tickBuilder.bid(lastBid);
                                    tickBuilder.bidSize(lastBidSize);
                                }
                            }else{
                                tickBuilder.bid(lastBid);
                                tickBuilder.bidSize(lastBidSize);
                            }

                            if(topOfBook.has("asks")){
                                double ask = topOfBook.getJSONArray("asks")
                                        .getJSONArray(0)
                                        .getDouble(0);
                                double askSize = topOfBook.getJSONArray("asks")
                                        .getJSONArray(0)
                                        .getDouble(1);

                                if(!Precision.equals(lastAsk, ask) || !Precision.equals(lastAskSize, askSize)) {
                                    lastAsk = ask;
                                    lastAskSize = askSize;
                                    tickBuilder.ask(ask);
                                    tickBuilder.askSize(askSize);
                                    isUpdated = true;
                                }else{
                                    tickBuilder.ask(lastAsk);
                                    tickBuilder.askSize(lastAskSize);
                                }
                            }else{
                                tickBuilder.ask(lastAsk);
                                tickBuilder.askSize(lastAskSize);
                            }

                            Tick tick = tickBuilder.build();

                            if(isUpdated) {
                                tickEventProcessor.publishTick(tick);
                            }
                        }
                    }
                }

                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    LOGGER.info("Connected to GateIo.");
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    LOGGER.info("GateIo closed connection");
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };

            client.connectBlocking(10, TimeUnit.SECONDS);

            JSONArray btcUsd = new JSONArray();
            btcUsd.put(currency+"_USDT");
            btcUsd.put(5);
            btcUsd.put("0.01");

            JSONArray params = new JSONArray();
            params.put(btcUsd);

            JSONObject subscribeMessage = new JSONObject();
            subscribeMessage.put("id", 12312);
            subscribeMessage.put("method", "depth.subscribe");
            subscribeMessage.put("params", params);

            client.send(subscribeMessage.toString());

        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}