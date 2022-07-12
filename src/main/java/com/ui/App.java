package com.ui;

import com.model.Product;
import com.model.Tick;
import com.orderbook.Book;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class App extends Application {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,###.####");

    private Stage stage;
    private volatile boolean ticksUpdated = false;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("Crypto Aggregator");

        Book.addListener((tickEvent, l, b) -> ticksUpdated = true);

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> Platform.runLater(() -> {

            if(!ticksUpdated){
                return;
            }

            ticksUpdated = false;

            Book book = Book.getBook(Product.Spot);

            ListView<String> bidListView = new ListView<>();
            ObservableList<String> bidItems = bidListView.getItems();

            ListView<String> askListView = new ListView<>();
            ObservableList<String> askItems = askListView.getItems();

            List<Tick> bids = new ArrayList<>(book.getBids());
            List<Tick> asks = new ArrayList<>(book.getAsks());

            int length = Math.max(bids.size(), asks.size());

            bidListView.setCellFactory(stringListView -> new AlignedListViewCell(Pos.CENTER_RIGHT));

            for (int i = 0; i < length; i++) {
                Tick bid = i < bids.size() ? bids.get(i) : null;
                Tick ask = i < asks.size() ? asks.get(i) : null;

                bidItems.add(bid != null ? DECIMAL_FORMAT.format(bid.getBid()) : "");
                askItems.add(ask != null ? DECIMAL_FORMAT.format(ask.getAsk()) : "");
            }

            HBox hBox = new HBox(bidListView, askListView);

            Scene scene = new Scene(hBox, 300, 120);
            stage.setScene(scene);
            stage.show();

        }), 250, 250, TimeUnit.MILLISECONDS);
    }

    public static void showApp(){
        Application.launch();
    }
}
