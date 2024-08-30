package com.example.javafxdemo.components;

import com.example.javafxdemo.*;
import com.example.javafxdemo.event.DefaultEventBus;
import com.example.javafxdemo.event.Event;
import com.example.javafxdemo.event.EventType;
import com.google.gson.Gson;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchTableView extends VBox implements SelfDefineComponent{

    private TableView<IndexInfo> tableView;
    private StackPane parentContainer;

    public SearchTableView(StackPane parentContainer) {
        this.setPadding(new Insets(10));
        this.parentContainer = parentContainer;
        initChildren();
        initEvent();
    }

    @Override
    public void initChildren() {

        tableView = new TableView<>();
        tableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        TableColumn<IndexInfo, String> indexNameColumn = new TableColumn<>("Index");
        indexNameColumn.setPrefWidth(200);
        indexNameColumn.setResizable(true);
        indexNameColumn.setCellValueFactory(new PropertyValueFactory<>("indexName"));
        TableColumn<IndexInfo, String> aliasNameColumn = new TableColumn<>("Alias");
        aliasNameColumn.setPrefWidth(280);
        aliasNameColumn.setResizable(true);
        aliasNameColumn.setCellValueFactory(new PropertyValueFactory<>("aliasName"));

        TableColumn<IndexInfo, Void> actionColumn = buildActionColumn();
        tableView.getColumns().addAll(indexNameColumn, aliasNameColumn, actionColumn);
        getChildren().add(tableView);
    }

    @Override
    public void initEvent() {
        //集群连接成功，拿到所有索引以及别名，刷新table视图
        DefaultEventBus.getInstance().registerConsumer(EventType.CONNECT_SUCCESS_CLUSTER,event->{
            LinkClusterInfo linkClusterInfo = (LinkClusterInfo) event.getEventData();
            //发请求去拿到所有的索引
            ResponseResult<String> indexResponse = RemoteCallApi.queryDsl(linkClusterInfo.getBaseUrl(),
                    linkClusterInfo.getAuthHead(),"GET","_alias","",null);
            if (indexResponse.getHttpStatus() == 200) {

                initTableView(indexResponse,linkClusterInfo);
            }
        });
    }
    private   void initTableView(ResponseResult<String> indexResponse,LinkClusterInfo linkClusterInfo) {
        HashMap<String, Object> indexMap = new Gson().fromJson(indexResponse.getData(),
                HashMap.class);
        List<IndexInfo> indexInfoList = new ArrayList<>();
        List<String> indexList = new ArrayList<>();
        indexMap.forEach((k, v) -> {
            IndexInfo indexInfo = new IndexInfo();
            indexInfo.setIndexName(k);
            indexList.add(k);
            if (v != null) {
                HashMap<String, Map> aliasMap = new Gson().fromJson(v.toString(), HashMap.class);
                Map<String, Object> aliasNameMap = aliasMap.get("aliases");
                if (aliasNameMap != null) {
                    indexInfo.setAliasName(String.join(",", aliasNameMap.keySet()));
                }

            }
            indexInfoList.add(indexInfo);
        });
        ObservableList<IndexInfo> tableData = FXCollections.observableArrayList(indexInfoList);
        tableView.setItems(tableData);
        //发送事件初始化
        SearchContext searchContext = new SearchContext();
        searchContext.setIndexInfoList(indexInfoList);
        searchContext.setLinkClusterInfo(linkClusterInfo);
        Event<SearchContext>event = new Event<>(EventType.INIT_CLUSTER_INDEX,searchContext);
        DefaultEventBus.getInstance().sendEvent(event);


    }

    private TableColumn<IndexInfo, Void> buildActionColumn( ) {
        TableColumn<IndexInfo, Void> actionColumn = new TableColumn<>("操作");
        actionColumn.setMinWidth(160);
        actionColumn.setResizable(true);
        actionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button delBtn = new Button("隐藏");
            private final Button searchBtn = new Button("查询");

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(delBtn, searchBtn);
                    hbox.setSpacing(15);
                    hbox.setPadding(new Insets(10));
                    setGraphic(hbox);
                    delBtn.setOnAction(actionEvent -> {
                        IndexInfo indexInfo = getTableView().getItems().get(getIndex());
                        // 从表格中删除
                        getTableView().getItems().remove(indexInfo);
                    });
                    searchBtn.setOnAction(actionEvent -> {
                        //使用当前索引做查询
                        IndexInfo indexInfo = getTableView().getItems().get(getIndex());
                        Event<IndexInfo>event = new Event<>(EventType.QUERY_WITH_SPECIAL_INDEX,indexInfo);
                        DefaultEventBus.getInstance().sendEvent(event);
                    });
                }
            }

        });
        return actionColumn;
    }
}
