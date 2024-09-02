package com.example.javafxdemo.components;


import com.example.javafxdemo.*;
import com.example.javafxdemo.event.DefaultEventBus;
import com.example.javafxdemo.event.EventType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.SearchableComboBox;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 根据已有索引查询数据
 */
public class DetailSearchBox extends VBox implements SelfDefineComponent{

    private StackPane parentContainer;

    private SearchContext searchContext;
    private Button actionBtn;

    private Stage stage;
    private  SearchableComboBox<String> indexComboBox;
    private ComboBox<String> methodComboBox;
    private ComboBox<String> actionComboBox;
    private TextArea queryDsl  ;
    private TextArea resultDsl ;
    private ComboBox<QueryHistory> historyDslComboBox;
    private Button historyBtn;
    public DetailSearchBox(StackPane parentContainer,Stage stage){
        this.setPadding(new Insets(10));
        this.parentContainer = parentContainer;
        this.stage = stage;
        initChildren();
        initEvent();
    }
    @Override
    public void initChildren() {
        //下拉框填充方法
        //下拉框填充索引
        //下拉框填充动作
        HBox querybox = new HBox();
        querybox.setSpacing(15);
        querybox.setPadding(new Insets(10));
        methodComboBox = new ComboBox<>();
        ObservableList<String> methodList = FXCollections.observableArrayList("GET", "POST", "PUT", "HEAD", "DELETE");
        methodComboBox.setItems(methodList);
        methodComboBox.getSelectionModel().selectFirst();

        indexComboBox = new SearchableComboBox <>();

        actionComboBox = new ComboBox<>();

        ObservableList<String> actionList = FXCollections.observableArrayList("_search", "_count","_mappings");


        actionComboBox.setItems(actionList);
        actionComboBox.getSelectionModel().selectFirst();

         actionBtn = new Button("GO !");

         historyBtn = new Button("Save For History");

       historyDslComboBox = new ComboBox<>();

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


          queryDsl = new TextArea();
          resultDsl = new TextArea();
        HBox dslBox = new HBox(queryDsl, resultDsl);
        HBox.setHgrow(queryDsl, Priority.ALWAYS);
        HBox.setHgrow(resultDsl, Priority.ALWAYS);
        dslBox.setPadding(new Insets(10));
        dslBox.setSpacing(10);
        dslBox.setPrefHeight(480);
        VBox.setVgrow(dslBox,Priority.ALWAYS);
        getChildren().addAll(querybox, dslBox);

    }

    @Override
    public void initEvent() {
        DefaultEventBus.getInstance().registerConsumer(EventType.INIT_CLUSTER_INDEX,event -> {
            this.searchContext= (SearchContext) event.getEventData();
            List<IndexInfo> indexInfoList = searchContext.getIndexInfoList();
            List<String> indexNameList = indexInfoList.stream().map(IndexInfo::getIndexName).collect(Collectors.toList());
            indexComboBox.setItems(FXCollections.observableArrayList(indexNameList));
            indexComboBox.getSelectionModel().selectFirst();
        });
        DefaultEventBus.getInstance().registerConsumer(EventType.QUERY_WITH_SPECIAL_INDEX,event -> {
            IndexInfo indexInfo= (IndexInfo) event.getEventData();
            if(searchContext!=null){
                List<IndexInfo> indexInfoList =   searchContext.getIndexInfoList();
                Optional<IndexInfo> first = indexInfoList.stream().filter(e -> e.getIndexName().equals(indexInfo.getIndexName())).findFirst();
                first.ifPresent(info -> indexComboBox.getSelectionModel().select(info.getIndexName()));

            }
        });


        actionBtn.setOnAction(actionEvent -> {
            if(searchContext == null){
                return;
            }
            //构建完整查询动作
            String index = indexComboBox.getSelectionModel().getSelectedItem();
            String method = methodComboBox.getSelectionModel().getSelectedItem();
            String action = actionComboBox.getSelectionModel().getSelectedItem();
            String requestJson = queryDsl.getText().trim();
            if (StringUtils.isEmpty(requestJson)) {
                requestJson = "{}";
            }
            LinkClusterInfo linkClusterInfo =searchContext.getLinkClusterInfo();
            String authHead = linkClusterInfo.getAuthHead();
            ResponseResult<String> dslResponse = RemoteCallApi.queryDsl(linkClusterInfo.getBaseUrl(), authHead, method, index,
                    action,
                    requestJson);
            String unformattedJson = dslResponse.getData();

            Gson gson = new GsonBuilder().setNumberToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                    .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).setPrettyPrinting().create();
            Object json = gson.fromJson(unformattedJson, Object.class);
            String prettyJson = gson.toJson(json);
            resultDsl.clear();
            resultDsl.appendText(prettyJson);

        });
        //历史查询语句传递
        historyDslComboBox.setOnAction(actionEvent -> {
            QueryHistory queryHistory = historyDslComboBox.getSelectionModel().getSelectedItem();
            queryDsl.setText(queryHistory.getRequestJson());
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
            dialog.initOwner(stage);
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
    }
}
