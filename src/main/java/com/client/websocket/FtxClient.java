package com.client.websocket;

import com.TickEventProcessor;
import com.model.CcyPair;
import com.model.Product;
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
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FtxClient implements Client {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CcyPair ccyPair;
    private final TickEventProcessor tickEventProcessor;

    public FtxClient(CcyPair ccyPair, TickEventProcessor tickEventProcessor){
        this.ccyPair = ccyPair;
        this.tickEventProcessor = tickEventProcessor;
    }

    @Override
    public void connect() {
        try {
            WebSocketClient client = new WebSocketClient(new URI("wss://ftx.com/ws/")) {

                private  double lastSpotBid = 0;
                private  double lastSpotAsk = 0;
                private  double lastSpotBidSize = 0;
                private  double lastSpotAskSize = 0;
                private  double lastFutureBid = 0;
                private  double lastFutureAsk = 0;
                private  double lastFutureBidSize = 0;
                private  double lastFutureAskSize = 0;

                @Override
                public void onMessage(String message) {
                    JSONObject json = new JSONObject(message);

                    if(!json.has("channel")){
                        return;
                    }

                    String channel = json.getString("channel");

                    if ("orderbook".equals(channel)) {

                        if(json.has("data")) {
                            JSONObject data = json.getJSONObject("data");
                            JSONArray bids = data.getJSONArray("bids");
                            JSONArray asks = data.getJSONArray("asks");

                            if(json.getString("market").contains("/USD")) {

                                double bid = bids.length() == 0 ? lastSpotBid : bids.getJSONArray(0).getDouble(0);
                                double ask = asks.length() == 0 ? lastSpotAsk : asks.getJSONArray(0).getDouble(0);
                                double bidSize = bids.length() == 0 ? lastSpotBidSize : bids.getJSONArray(0).getDouble(1);
                                double askSize = asks.length() == 0 ? lastSpotAskSize : asks.getJSONArray(0).getDouble(1);

                                if (!Precision.equals(lastSpotBid, bid) ||
                                    !Precision.equals(lastSpotAsk, ask) ||
                                    !Precision.equals(lastSpotBidSize, bidSize) ||
                                    !Precision.equals(lastSpotAskSize, askSize)) {

                                    Tick tick = new Tick.Builder()
                                            .exchange("ftx")
                                            .product(Product.Spot)
                                            .timestamp(Instant.ofEpochMilli(((Number) data.getDouble("time")).longValue() * 1000))
                                            .bid(bid)
                                            .ask(ask)
                                            .bidSize(bidSize)
                                            .askSize(askSize)
                                            .build();

                                    lastSpotBid = bid;
                                    lastSpotAsk = ask;
                                    lastSpotBidSize = bidSize;
                                    lastSpotAskSize = askSize;

                                    tickEventProcessor.publishTicks(Set.of(tick));
                                }
                            }else{

                                double bid = bids.length() == 0 ? lastFutureBid : bids.getJSONArray(0).getDouble(0);
                                double ask = asks.length() == 0 ? lastFutureAsk : asks.getJSONArray(0).getDouble(0);
                                double bidSize = bids.length() == 0 ? lastFutureBidSize : bids.getJSONArray(0).getDouble(1);
                                double askSize = asks.length() == 0 ? lastFutureAskSize : asks.getJSONArray(0).getDouble(1);

                                if (!Precision.equals(lastFutureBid, bid) ||
                                    !Precision.equals(lastFutureAsk, ask) ||
                                    !Precision.equals(lastFutureBidSize, bidSize) ||
                                    !Precision.equals(lastFutureAskSize, askSize)) {

                                    Tick tick = new Tick.Builder()
                                            .exchange("ftx")
                                            .product(Product.Future)
                                            .timestamp(Instant.ofEpochMilli(((Number) data.getDouble("time")).longValue() * 1000))
                                            .bid(bid)
                                            .ask(ask)
                                            .bidSize(bidSize)
                                            .askSize(askSize)
                                            .build();

                                    lastFutureBid = bid;
                                    lastFutureAsk = ask;
                                    lastFutureBidSize = bidSize;
                                    lastFutureAskSize = askSize;

                                    tickEventProcessor.publishTicks(Set.of(tick));
                                }
                            }
                        }
                    }
                }

                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    LOGGER.info("Connected to FTX.");
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    LOGGER.info("FTX closed connection");
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };

            client.connectBlocking(10, TimeUnit.SECONDS);

            JSONObject subscribeMessage = new JSONObject();
            subscribeMessage.put("op", "subscribe");
            subscribeMessage.put("channel", "orderbook");

            subscribeMessage.put("market", ccyPair.getCcy1() +"/"+ ccyPair.getCcy2().replace("USDT", "USD"));
            client.send(subscribeMessage.toString());

            subscribeMessage.put("market", ccyPair.getCcy1() +"-PERP");
            client.send(subscribeMessage.toString());

            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                    () -> client.send("{'op': 'ping'}"),
                    15, 15, TimeUnit.SECONDS);

        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}