import com.OrderBookManager;
import com.TickEventProcessor;
import com.client.rest.FtxRestClient;
import com.client.websocket.*;

public class Main {

    public static void main(String[] args) {
        String currency = "SOL";

        TickEventProcessor processor = new TickEventProcessor();
        processor.start(new OrderBookManager());

        CoinbaseClient coinbaseClient = new CoinbaseClient(currency, processor);
        FtxClient ftxClient = new FtxClient(currency, processor);
        FtxRestClient ftxRestClient = new FtxRestClient(currency);
        KrakenClient krakenClient = new KrakenClient(currency, processor);
        BitstampClient bitstampClient = new BitstampClient(currency, processor);
        GateIoClient gateIoClient = new GateIoClient(currency, processor);
        BinanceClient binanceClient = new BinanceClient(currency, processor);
        BinanceFuturesClient binanceFuturesClient = new BinanceFuturesClient(currency, processor);
        BybitClient bybitClient = new BybitClient(currency, processor);
        BybitFuturesClient bybitFuturesClient = new BybitFuturesClient(currency, processor);

        coinbaseClient.connect();
        ftxClient.connect();
        ftxRestClient.connect();
        krakenClient.connect();
        bitstampClient.connect();
        gateIoClient.connect();
        binanceClient.connect();
        bybitClient.connect();
        bybitFuturesClient.connect();
        binanceFuturesClient.connect();
    }
}
