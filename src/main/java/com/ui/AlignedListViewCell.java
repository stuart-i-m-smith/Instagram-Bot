package com.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;

public class AlignedListViewCell extends ListCell<String> {

    private final Pos pos;

    public AlignedListViewCell(Pos pos){
        this.pos = pos;
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setGraphic(null);
        } else {
            // Create the HBox
            HBox hBox = new HBox();
            hBox.setAlignment(pos);

            // Create centered Label
            Label label = new Label(item);
            label.setAlignment(pos);

            hBox.getChildren().add(label);
            setGraphic(hBox);
        }
    }
}