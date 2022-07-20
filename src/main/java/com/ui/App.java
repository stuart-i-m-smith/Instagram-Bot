package com.ui;

import com.model.Product;
import com.model.Tick;
import com.orderbook.Book;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.net.URL;
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

        stage.setOnCloseRequest(e -> {
            stage.close();
            System.exit(0);
        });

        Book.addListener((tickEvent, l, b) -> ticksUpdated = true);

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> Platform.runLater(this::redrawUi),
    250, 250, TimeUnit.MILLISECONDS);
    }

    private void redrawUi() {
        if(!ticksUpdated){
            return;
        }

        ticksUpdated = false;

        Book book = Book.getBook(Product.Spot);

        ListView<String> bidExchangeListView = new ListView<>();
        ObservableList<String> bidExchangeItems = bidExchangeListView.getItems();

        ListView<String> askExchangeListView = new ListView<>();
        ObservableList<String> askExchangeItems = askExchangeListView.getItems();

        ListView<String> bidListView = new ListView<>();
        ObservableList<String> bidItems = bidListView.getItems();

        ListView<String> askListView = new ListView<>();
        ObservableList<String> askItems = askListView.getItems();

        List<Tick> bids = new ArrayList<>(book.getBids());
        List<Tick> asks = new ArrayList<>(book.getAsks());

        double maxBidSize = bids.stream().mapToDouble(Tick::getBidSize).max().orElse(1);
        double maxAskSize = asks.stream().mapToDouble(Tick::getAskSize).max().orElse(1);
        double maxSize = Math.max(maxBidSize, maxAskSize) * 1.2;

        bidListView.setCellFactory(s -> new AlignedListViewCell(Pos.CENTER_RIGHT));
        askExchangeListView.setCellFactory(s -> new AlignedListViewCell(Pos.CENTER_RIGHT));

        StackPane stackPane = new StackPane();
        int length = Math.max(bids.size(), asks.size());

        for (int i = 0; i < length; i++) {
            Tick bid = i < bids.size() ? bids.get(i) : null;
            Tick ask = i < asks.size() ? asks.get(i) : null;

            bidExchangeItems.add(bid != null ? bid.getExchange() : "");
            bidItems.add(bid != null ? DECIMAL_FORMAT.format(bid.getBid()) : "");
            askItems.add(ask != null ? DECIMAL_FORMAT.format(ask.getAsk()) : "");
            askExchangeItems.add(ask != null ? ask.getExchange() : "");

            double bidSizeRatio = bid != null ? bid.getBidSize() / maxSize : 0;
            double askSizeRatio = ask != null ? ask.getAskSize() / maxSize : 0;

            Rectangle rectangleR = new Rectangle(bidSizeRatio * 250, 25);
            rectangleR.setFill(Color.RED);
            StackPane.setMargin(rectangleR, new Insets(0, (bidSizeRatio * 250), 225 - (i * 45), 0));
            stackPane.getChildren().add(rectangleR);

            Rectangle rectangleG = new Rectangle(askSizeRatio * 250, 25);
            rectangleG.setFill(Color.GREEN);
            StackPane.setMargin(rectangleG, new Insets(0, 0, 225 - (i * 45), (askSizeRatio * 250)));
            stackPane.getChildren().add(rectangleG);

        }

        HBox hBox = new HBox(bidExchangeListView, bidListView, askListView, askExchangeListView);

        stackPane.getChildren().add(hBox);

        Scene scene = new Scene(stackPane, 500, 250);

        URL resource = getClass().getResource("/stylesheet.css");
        if(resource != null) {
            scene.getStylesheets().add(resource.toString());
        }

        stage.setScene(scene);
        stage.show();
    }

    public static void showApp(){
        Application.launch();
    }
}
