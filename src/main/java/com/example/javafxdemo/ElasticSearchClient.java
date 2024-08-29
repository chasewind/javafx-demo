package com.example.javafxdemo;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Pair;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.SearchableComboBox;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ElasticSearchClient extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    private static TableColumn<IndexInfo, Void> buildActionColumn(StackPane stackPane, VBox vbox,
                                                                  ComboBox<String> indexComboBox) {
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
                    delBtn.setOnAction(event -> {
                        IndexInfo indexInfo = getTableView().getItems().get(getIndex());
                        //TODO RPC操作
                        getTableView().getItems().remove(indexInfo); // 从表格中删除
                    });
                    searchBtn.setOnAction(event -> {
                        IndexInfo indexInfo = getTableView().getItems().get(getIndex());
                        //TODO RPC操作
                        //切换显示界面
                        indexComboBox.getSelectionModel().select(indexInfo.getIndexName());
                        stackPane.getChildren().clear();
                        stackPane.getChildren().add(vbox);
                    });
                }
            }

        });
        return actionColumn;
    }

    private static void initTableView(ResponseResult<String> indexResponse, TableView<IndexInfo> tableView,
                                      ComboBox<String> indexComboBox) {
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
        ObservableList<String> indexData = FXCollections.observableArrayList(indexList);
        indexComboBox.setItems(indexData);

    }

    private static void buildLoginDialog(Stage primaryStage, String baseUrl, TableView<IndexInfo> tableView,
                                         ComboBox<String> indexComboBox) {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("登录");
        dialog.setHeaderText("请输入用户名和密码");
        TextField username = new TextField();
        username.setPromptText("用户名");
        PasswordField password = new PasswordField();
        password.setPromptText("密码");
        dialog.getDialogPane().setContent(new VBox(10, username, password));
        ButtonType loginBtnType = new ButtonType("登录", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginBtnType, ButtonType.CANCEL);
        dialog.setResultConverter(dialogBtn -> {
            if (dialogBtn == loginBtnType) {
                return new Pair<>(username.getText().trim(), password.getText().trim());
            }
            return null;
        });
        dialog.initOwner(primaryStage);
        Optional<Pair<String, String>> loginResult = dialog.showAndWait();
        loginResult.ifPresent(input -> {
            String realUserName = input.getKey();
            String realPassword = input.getValue();
            //调整为basic登录
            // 创建基本认证的Authorization头
            String auth = realUserName + ":" + realPassword;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            String authHeader = "Basic " + encodedAuth;

            ResponseResult<String> loginResponse = RemoteCallApi.loginWithUsernameAndPassword(baseUrl, realUserName,
                    realPassword);
            if (loginResponse.getHttpStatus() == 200) {
                //登录成功，数据回写，确保下次能直接使用
                LinkClusterInfo linkClusterInfo = new LinkClusterInfo();
                linkClusterInfo.setBaseUrl(baseUrl);
                linkClusterInfo.setClusterName("");
                linkClusterInfo.setUsername(realUserName);
                linkClusterInfo.setPassword(realPassword);
                linkClusterInfo.setAuthHead(authHeader);
                LinkClusterCache.addToCache(linkClusterInfo);
                //发请求去拿到所有的索引
                HttpRequest indexRequest = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/_alias"))
                        .header("Authorization", authHeader)
                        .GET()
                        .build();
                ResponseResult<String> indexResponse = RemoteCallApi.sendRequest(indexRequest);
                if (indexResponse.getHttpStatus() == 200) {
                    initTableView(indexResponse, tableView, indexComboBox);
                }

            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("错误");
                alert.setHeaderText("用户名和密码错误");
                alert.setContentText("请检查用户名和密码后重新输入");
                alert.showAndWait();
            }
        });
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        //初始化，先把已有集群数据加载上来
        LinkClusterCache.init();
        QueryHistoryCache.init();

        BorderPane root = new BorderPane();

        TextField urlInput = new TextField("请输入连接地址");
        Button connBtn = new Button("连 接");
        ComboBox<String> linkClusterComboBox = new ComboBox<>();
        //根据缓存拿数据填充
        List<LinkClusterInfo> clusterInfoList = LinkClusterCache.getAll();
        if (CollectionUtils.isNotEmpty(clusterInfoList)) {
            List<String> baseUrlList = clusterInfoList.stream().map(LinkClusterInfo::getBaseUrl).toList();
            linkClusterComboBox.setItems(FXCollections.observableArrayList(baseUrlList));
            linkClusterComboBox.getSelectionModel().selectFirst();
            urlInput.setText(linkClusterComboBox.getValue());
        }
        //数据一致显示
        linkClusterComboBox.setOnAction(actionEvent -> {
            String selectedItem = linkClusterComboBox.getSelectionModel().getSelectedItem();
            urlInput.setText(selectedItem);

        });

        StackPane stackPane = new StackPane();
        stackPane.setAlignment(Pos.CENTER);
        stackPane.setPadding(new Insets(10));

        //stackPane的第二层
        VBox vbox = new VBox();
        vbox.setSpacing(10);
        vbox.setPadding(new Insets(10));
        //下拉框填充方法
        //下拉框填充索引
        //下拉框填充动作
        HBox querybox = new HBox();
        querybox.setSpacing(15);
        querybox.setPadding(new Insets(10));
        ComboBox<String> methodComboBox = new ComboBox<>();
        ObservableList<String> methodList = FXCollections.observableArrayList("GET", "POST", "PUT", "HEAD", "DELETE");
        methodComboBox.setItems(methodList);
        methodComboBox.getSelectionModel().selectFirst();

        SearchableComboBox<String> indexComboBox = new SearchableComboBox <>();


        ComboBox<String> actionComboBox = new ComboBox<>();

        ObservableList<String> actionList = FXCollections.observableArrayList("_search", "_count");


        actionComboBox.setItems(actionList);
        actionComboBox.getSelectionModel().selectFirst();

        Button actionBtn = new Button("GO !");

        Button historyBtn = new Button("Save For History");

        ComboBox<QueryHistory> historyDslComboBox = new ComboBox<>();

        ObservableList<QueryHistory> dslList = FXCollections.observableArrayList(QueryHistoryCache.getAll());
        historyDslComboBox.setItems(dslList);
        Callback<ListView<QueryHistory>, ListCell<QueryHistory>> factory= new Callback<>() {
            @Override
            public ListCell<QueryHistory> call(ListView<QueryHistory> queryHistoryListView) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(QueryHistory queryHistory, boolean empty) {
                        super.updateItem(queryHistory, empty);
                        setText(empty ? "" : queryHistory.getTemplateName());

                    }
                };
            }
        };
        historyDslComboBox.setCellFactory(factory);
        historyDslComboBox.setButtonCell(factory.call(null));



        querybox.getChildren().addAll(methodComboBox, indexComboBox, actionComboBox, actionBtn, historyBtn,
                historyDslComboBox);


        TextArea queryDsl = new TextArea();
        TextArea resultDsl = new TextArea();
        HBox dslBox = new HBox(queryDsl, resultDsl);
        HBox.setHgrow(queryDsl, Priority.ALWAYS);
        HBox.setHgrow(resultDsl, Priority.ALWAYS);
        dslBox.setPadding(new Insets(10));
        dslBox.setSpacing(10);
        dslBox.setPrefHeight(480);
        vbox.getChildren().addAll(querybox, dslBox);


        //历史查询语句传递
        historyDslComboBox.setOnAction(actionEvent -> {
            QueryHistory queryHistory = historyDslComboBox.getSelectionModel().getSelectedItem();
            queryDsl.setText(queryHistory.getRequestJson());

        });

        //stackPane的第一层
        TableView<IndexInfo> tableView = new TableView<>();
        tableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        TableColumn<IndexInfo, String> indexNameColumn = new TableColumn<>("Index");
//        indexNameColumn.setMinWidth(200);
        indexNameColumn.setPrefWidth(200);
        indexNameColumn.setResizable(true);
        indexNameColumn.setCellValueFactory(new PropertyValueFactory<>("indexName"));
        TableColumn<IndexInfo, String> aliasNameColumn = new TableColumn<>("Alias");
//        aliasNameColumn.setMinWidth(280);
        aliasNameColumn.setPrefWidth(280);
        aliasNameColumn.setResizable(true);
        aliasNameColumn.setCellValueFactory(new PropertyValueFactory<>("aliasName"));

        TableColumn<IndexInfo, Void> actionColumn = buildActionColumn(stackPane, vbox, indexComboBox);
        tableView.getColumns().clear();
        tableView.getColumns().addAll(indexNameColumn, aliasNameColumn, actionColumn);
        System.out.println("After adding: " + tableView.getColumns().size());
        connBtn.setOnMouseClicked(mouseEvent -> {
            boolean loginFlag = false;
            //判断是否已存在
            LinkClusterInfo currentLinkClusterInfo = LinkClusterCache.getByBaseUrl(urlInput.getText().trim());
            //
            if (currentLinkClusterInfo != null) {
                //拿已有参数直接连接
                ResponseResult<String> responseResult =
                        RemoteCallApi.loginWithAuthHead(currentLinkClusterInfo.getBaseUrl(),
                                currentLinkClusterInfo.getAuthHead());
                if (responseResult.getHttpStatus() == 200) {
                    //登录成功
                    System.out.println("login success " + responseResult.getData());
                    loginFlag = true;
                }
            }
            //如果没有登录成功，则拼接参数去请求
            if (!loginFlag) {
                ResponseResult<String> responseResult = RemoteCallApi.loginWithBaseUrl(urlInput.getText().trim());
                if (responseResult.getHttpStatus() == 200) {
                    //登录成功
                    System.out.println("login without username and password success " + responseResult.getData());
                    LinkClusterInfo linkClusterInfo = new LinkClusterInfo();
                    linkClusterInfo.setBaseUrl(urlInput.getText().trim());
                    linkClusterInfo.setClusterName("");
                    linkClusterInfo.setUsername("");
                    linkClusterInfo.setPassword("");
                    linkClusterInfo.setAuthHead("");
                    LinkClusterCache.addToCache(linkClusterInfo);
                    //发请求去拿到所有的索引
                    HttpRequest indexRequest = HttpRequest.newBuilder()
                            .uri(URI.create(urlInput.getText().trim() + "/_alias"))
                            .GET()
                            .build();
                    ResponseResult<String> indexResponse = RemoteCallApi.sendRequest(indexRequest);
                    if (indexResponse.getHttpStatus() == 200) {
                        initTableView(indexResponse, tableView, indexComboBox);
                    }
                } else if (responseResult.getHttpStatus() == 401) {
                    buildLoginDialog(primaryStage, urlInput.getText().trim(), tableView, indexComboBox);

                } else {
                    System.out.println("other scene ..." + responseResult.getData());
                }
            } else {
                //发请求去拿到所有的索引
                HttpRequest indexRequest =
                        RemoteCallApi.buildHttpRequestWithAuth(currentLinkClusterInfo.getBaseUrl() + "/_alias",
                        currentLinkClusterInfo.getAuthHead());
                ResponseResult<String> indexResponse = RemoteCallApi.sendRequest(indexRequest);
                if (indexResponse.getHttpStatus() == 200) {
                    initTableView(indexResponse, tableView, indexComboBox);
                }
            }


        });
        actionBtn.setOnAction(actionEvent -> {
            //构建完整查询动作
            String baseUrl = urlInput.getText().trim();
            String index = indexComboBox.getSelectionModel().getSelectedItem();
            String method = methodComboBox.getSelectionModel().getSelectedItem();
            String action = actionComboBox.getSelectionModel().getSelectedItem();
            String requestJson = queryDsl.getText().trim();
            if (StringUtils.isEmpty(requestJson)) {
                requestJson = "{}";
            }
            LinkClusterInfo linkClusterInfo = LinkClusterCache.getByBaseUrl(baseUrl);
            String authHead = linkClusterInfo.getAuthHead();
            ResponseResult<String> dslResponse = RemoteCallApi.queryDsl(baseUrl, authHead, method, index, action,
                    requestJson);
            String unformattedJson = dslResponse.getData();

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Object json = gson.fromJson(unformattedJson, Object.class);
            String prettyJson = gson.toJson(json);
            resultDsl.clear();
            resultDsl.appendText(prettyJson);

        });
        historyBtn.setOnAction(actionEvent -> {

            String requestJson = queryDsl.getText().trim();
            if (StringUtils.isEmpty(requestJson)) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("错误");
                alert.setHeaderText("DSL文本为空");
                alert.setContentText("重新输入");
                alert.showAndWait();
                return;
            }

            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("保存为历史记录");
            dialog.setHeaderText("保存当前查询");
            TextField queryId = new TextField();
            queryId.setPromptText("请输入一个有效的查询名字");
            dialog.getDialogPane().setContent(new VBox(10, queryId));
            ButtonType okBtnType = new ButtonType("确认", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(okBtnType, ButtonType.CANCEL);
            dialog.setResultConverter(dialogBtn -> {
                if (dialogBtn == okBtnType) {
                    return queryId.getText().trim();
                }
                return null;
            });
            dialog.initOwner(primaryStage);
            Optional<String> historyResult = dialog.showAndWait();
            historyResult.ifPresent(input -> {
                String saveId = input.trim();
                if (StringUtils.isNotEmpty(saveId)) {
                    QueryHistory queryHistory = new QueryHistory();
                    queryHistory.setTemplateName(saveId);
                    queryHistory.setRequestJson(requestJson);
                    QueryHistoryCache.save(queryHistory);

                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setHeaderText("重新输入");
                    alert.setContentText("重新输入");
                    alert.showAndWait();
                }
            });
        });

        HBox top = new HBox(urlInput, linkClusterComboBox, connBtn);
        top.setSpacing(15);
        top.setPadding(new Insets(10));
        root.setTop(top);
        //默认第一个展示
        stackPane.getChildren().add(tableView);
        root.setCenter(stackPane);
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("ELASTICSEARCH查询客户端");
        primaryStage.setWidth(1200);
        primaryStage.setHeight(600);
        primaryStage.show();
    }
}
