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
        TickArbitrageDetector tickArbitrageDetector = new TickArbitrageDetector();

        tickManager.addTickObserver(tickReporter);
        //tickManager.addTickObserver(tickArbitrageDetector);

        CoinbaseClient coinbaseClient = new CoinbaseClient(tickManager);
        FtxClient ftxClient = new FtxClient(currency, tickManager, futuresTickManager);
        KrakenClient krakenClient = new KrakenClient(tickManager);
        BitstampClient bitstampClient = new BitstampClient(tickManager);
        GateIoClient gateIoClient = new GateIoClient(tickManager);
        BinanceClient binanceClient = new BinanceClient(tickManager);
        BinanceFuturesClient binanceFuturesClient = new BinanceFuturesClient(currency, futuresTickManager);

        //coinbaseClient.connect();
        //ftxClient.connect();
//        krakenClient.connect();
//        bitstampClient.connect();
//        gateIoClient.connect();
//        binanceClient.connect();
        binanceFuturesClient.connect();
//
//        tickReporter.scheduleReport();
    }
}
