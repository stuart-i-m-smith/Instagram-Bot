import com.tick.TickArbitrageDetector;
import com.tick.TickManager;
import com.tick.TickReporter;
import com.client.*;

public class Main {

    public static void main(String[] args) {

        TickManager tickManager = new TickManager();
        TickReporter tickReporter = new TickReporter();
        TickArbitrageDetector tickArbitrageDetector = new TickArbitrageDetector();

        tickManager.addTickObserver(tickReporter);
        tickManager.addTickObserver(tickArbitrageDetector);

        CoinbaseClient coinbaseClient = new CoinbaseClient(tickManager);
        FtxClient ftxClient = new FtxClient(tickManager);
        KrakenClient krakenClient = new KrakenClient(tickManager);
        BitstampClient bitstampClient = new BitstampClient(tickManager);
        GateIoClient gateIoClient = new GateIoClient(tickManager);
        BinanceClient binanceClient = new BinanceClient(tickManager);

        coinbaseClient.connect();
        ftxClient.connect();
        krakenClient.connect();
        bitstampClient.connect();
        gateIoClient.connect();
        binanceClient.connect();

        tickReporter.scheduleReport();
    }
}
