package com.example.javafxdemo;


import com.example.javafxdemo.components.DetailSearchBox;
import com.example.javafxdemo.components.SearchHeader;
import com.example.javafxdemo.components.SearchTableView;
import com.example.javafxdemo.event.DefaultEventBus;
import com.example.javafxdemo.event.EventSubscriber;
import com.example.javafxdemo.event.EventType;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ElasticSearchClient extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        //初始化，先把已有集群数据加载上来
        LinkClusterCache.init();
        QueryHistoryCache.init();
        //注册事件机制
        EventSubscriber eventSubscriber = new EventSubscriber();
        DefaultEventBus.getInstance().register(eventSubscriber);

        BorderPane root = new BorderPane();

        StackPane stackPane = new StackPane();
        stackPane.setAlignment(Pos.CENTER);
        stackPane.setPadding(new Insets(10));
        SearchHeader searchHeader = new SearchHeader(primaryStage);
        root.setTop(searchHeader);
        //默认第一个展示
        SearchTableView searchTableView = new SearchTableView(stackPane);
        DetailSearchBox detailSearchBox = new DetailSearchBox(stackPane,primaryStage);
        DefaultEventBus.getInstance().registerConsumer(EventType.QUERY_WITH_SPECIAL_INDEX, event -> {
            stackPane.getChildren().clear();
            stackPane.getChildren().add(detailSearchBox);
               });
        stackPane.getChildren().addAll(searchTableView);
        root.setCenter(stackPane);
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("ELASTICSEARCH查询客户端");
        primaryStage.setWidth(1200);
        primaryStage.setHeight(600);
        primaryStage.show();
    }
}
