package com.example.javafxdemo;


import com.example.javafxdemo.components.DetailSearchBox;
import com.example.javafxdemo.components.SearchHeader;
import com.example.javafxdemo.components.SearchTableView;
import com.example.javafxdemo.event.DefaultEventBus;
import com.example.javafxdemo.event.EventSubscriber;
import com.example.javafxdemo.event.EventType;
import com.example.javafxdemo.util.GlobalApplicationLifecycleUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
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
        //
        Label messageLabel = new Label("提示");
        MessageEmitter messageEmitter = new MessageEmitter(messageLabel);
        root.setBottom(messageLabel);
        //默认第一个展示
        SearchTableView searchTableView = new SearchTableView( );
        DetailSearchBox detailSearchBox = new DetailSearchBox(messageEmitter,primaryStage);
        DefaultEventBus.getInstance().registerConsumer(EventType.QUERY_WITH_SPECIAL_INDEX, event -> {
            stackPane.getChildren().clear();
            stackPane.getChildren().add(detailSearchBox);
               });
        DefaultEventBus.getInstance().registerConsumer(EventType.BACK_TO_OVERVIEW,event -> {
            stackPane.getChildren().clear();
            stackPane.getChildren().add(searchTableView);
        });
        DefaultEventBus.getInstance().registerConsumer(EventType.CONNECT_FAIL_CLUSTER,event -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText("连接集群失败");
            alert.setContentText("请确认网络以及用户名密码后重试");
            alert.showAndWait();
        });
        stackPane.getChildren().addAll(searchTableView);
        root.setCenter(stackPane);



        Scene scene = new Scene(root);
        scene.getStylesheets().add(ElasticSearchClient.class.getResource("css/json-assist.css").toExternalForm());
        scene.getStylesheets().add(ElasticSearchClient.class.getResource("css/common.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("ELASTICSEARCH查询客户端");
        primaryStage.setWidth(1200);
        primaryStage.setHeight(600);
        primaryStage.show();
        primaryStage.setOnCloseRequest(e -> {
            GlobalApplicationLifecycleUtil.stop();
            Platform.exit();
            System.exit(0);
        });
    }
}
