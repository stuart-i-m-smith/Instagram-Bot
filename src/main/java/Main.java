import com.orderbook.Book;
import com.TickEventProcessor;
import com.client.rest.FtxRestClient;
import com.client.websocket.*;
import com.orderbook.BookReporter;

import static com.model.Product.Future;
import static com.model.Product.Spot;

public class Main {

    public static void main(String[] args) {
        String currency = "SOL";

        Book spotBook = new Book(Spot);
        Book futureBook = new Book(Future);

        TickEventProcessor processor = new TickEventProcessor();
        processor.start(spotBook, futureBook);

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

        BookReporter spotBookReporter = new BookReporter(spotBook);
        spotBookReporter.scheduleReport(5);

        BookReporter futureBookReporter = new BookReporter(futureBook);
        futureBookReporter.scheduleReport(8);
    }
}
