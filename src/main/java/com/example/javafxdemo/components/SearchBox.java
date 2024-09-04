package com.example.javafxdemo.components;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class SearchBox extends BorderPane {

    private final TextField searchTextField;

    private final SearchOverviewFetcher searchOverviewFetcher = new SearchOverviewFetcher();

    private Runnable whenClose = null;

    public SearchBox(BorderPane parentContainer, OnNextClick onNextClick, OnPreviousClick onPreviousClick) {
        this.setPadding(new Insets(5, 10, 5, 10));

        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER_LEFT);
        ObservableList<Node> hBoxChildren = hBox.getChildren();

        Label label = new Label("Search:");
        HBox.setMargin(label, new Insets(0, 10, 0, 0));
        searchTextField = new TextField();
        HBox.setMargin(searchTextField, new Insets(0, 10, 0, 0));
        hBoxChildren.add(label);
        hBoxChildren.add(searchTextField);

        Button next = new Button("Next");
        HBox.setMargin(next, new Insets(0, 10, 0, 0));
        hBoxChildren.add(next);

        Button previous = new Button("Previous");
        HBox.setMargin(previous, new Insets(0, 10, 0, 0));
        hBoxChildren.add(previous);

        HBox searchOverviewBox = new HBox();
        searchOverviewBox.setAlignment(Pos.CENTER_LEFT);
        Label searchOverviewResultTitle = new Label("Result: ");
        Label searchOverviewContent = new Label();
        ObservableList<Node> searchOverviewChildren = searchOverviewBox.getChildren();
        searchOverviewChildren.add(searchOverviewResultTitle);
        searchOverviewChildren.add(searchOverviewContent);
        HBox.setMargin(previous, new Insets(0, 10, 0, 0));
        hBoxChildren.add(searchOverviewBox);
        searchOverviewBox.visibleProperty().setValue(false);

        Runnable closeSearch = () -> {
            parentContainer.setTop(null);
            if(whenClose != null){
                whenClose.run();
            }
        };

        this.setLeft(hBox);
        Label close = new Label("×");
        close.setStyle("-fx-font-size: 16; -fx-cursor: hand;");
        close.setOnMouseClicked(mouseEvent -> closeSearch.run());
        this.setRight(close);

        Runnable onNext = () -> {
            searchOverviewFetcher.setSearchOverview(null);
            onNextClick.click(searchTextField.getText(), searchOverviewFetcher);
            SearchOverview searchOverview = searchOverviewFetcher.searchOverview;
            if(searchOverview != null){
                searchOverviewContent.setText((searchOverview.currentIndex + 1) + "/" + (searchOverview.maxIndex + 1));
                searchOverviewBox.visibleProperty().setValue(true);
            }else{
                searchOverviewBox.visibleProperty().setValue(false);
                searchOverviewContent.setText("");
            }
        };

        next.setOnAction(event -> onNext.run());
        searchTextField.setOnKeyPressed(keyEvent -> {
            if(keyEvent.getCode() == KeyCode.ENTER){
                onNext.run();
            }else if(keyEvent.getCode() == KeyCode.ESCAPE){
                closeSearch.run();
            }
        });

        previous.setOnAction(event -> {
            searchOverviewFetcher.setSearchOverview(null);
            onPreviousClick.click(searchTextField.getText(), searchOverviewFetcher);
            SearchOverview searchOverview = searchOverviewFetcher.searchOverview;
            if(searchOverview != null){
                searchOverviewContent.setText((searchOverview.currentIndex + 1) + "/" + (searchOverview.maxIndex + 1));
                searchOverviewBox.visibleProperty().setValue(true);
            }else{
                searchOverviewBox.visibleProperty().setValue(false);
                searchOverviewContent.setText("");
            }
        });
    }

    interface OnNextClick {
        void click(String keyword, SearchOverviewFetcher searchOverviewFetcher);
    }

    interface OnPreviousClick{
        void click(String keyword, SearchOverviewFetcher searchOverviewFetcher);
    }

    public void focusSearch(){
        this.focusSearch(null);
    }

    public void focusSearch(String text){
        if(text != null){
            searchTextField.setText(text);
        }
        searchTextField.requestFocus();
    }

    public static class SearchOverviewFetcher{
        private SearchOverview searchOverview;
        public void setSearchOverview(SearchOverview searchOverview){
            this.searchOverview = searchOverview;
        }
    }

    public static class SearchOverview {
        private final int maxIndex;
        private final int currentIndex;

        public SearchOverview(int maxIndex, int currentIndex) {
            this.maxIndex = maxIndex;
            this.currentIndex = currentIndex;
        }
    }

    public void onClose(Runnable closeRun){
        this.whenClose = closeRun;
    }
}
