import com.tick.TickArbitrageDetector;
import com.tick.TickManager;
import com.tick.TickReporter;
import com.client.*;

public class Main {

    public static void main(String[] args) {
        String currency = "SOL";

        TickManager tickManager = new TickManager();
        TickManager futuresTickManager = new TickManager();

        TickReporter tickReporter = new TickReporter();
        TickReporter futuresTickReporter = new TickReporter();

        TickArbitrageDetector tickArbitrageDetector = new TickArbitrageDetector();

        tickManager.addTickObserver(tickReporter);
        //tickManager.addTickObserver(tickArbitrageDetector);

        futuresTickManager.addTickObserver(futuresTickReporter);

        CoinbaseClient coinbaseClient = new CoinbaseClient(currency, tickManager);
        FtxClient ftxClient = new FtxClient(currency, tickManager, futuresTickManager);
        KrakenClient krakenClient = new KrakenClient(currency, tickManager);
        BitstampClient bitstampClient = new BitstampClient(currency, tickManager);
        GateIoClient gateIoClient = new GateIoClient(currency, tickManager);
        BinanceClient binanceClient = new BinanceClient(currency, tickManager);
        BinanceFuturesClient binanceFuturesClient = new BinanceFuturesClient(currency, futuresTickManager);
        BybitClient bybitClient = new BybitClient(currency, tickManager);
        BybitFuturesClient bybitFuturesClient = new BybitFuturesClient(currency, futuresTickManager);

        coinbaseClient.connect();
        ftxClient.connect();
        krakenClient.connect();
        bitstampClient.connect();
        gateIoClient.connect();
        binanceClient.connect();
        bybitClient.connect();
        bybitFuturesClient.connect();

        //        binanceFuturesClient.connect();

        tickReporter.scheduleReport("Spot", 14);
        futuresTickReporter.scheduleReport("Futures", 15);

    }
}
