package com.example.javafxdemo.components;

import com.example.javafxdemo.*;
import com.example.javafxdemo.event.DefaultEventBus;
import com.example.javafxdemo.event.Event;
import com.example.javafxdemo.event.EventType;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

public class SearchHeader extends HBox implements SelfDefineComponent{

    private TextField urlInput;
    private Button connBtn;
    private ComboBox<String> linkClusterComboBox;
    private Stage stage;

    public SearchHeader(Stage stage){
        this.stage =stage;
        this.setSpacing(15);
        this.setPadding(new Insets(15));
        initChildren();
        initEvent();
    }
    @Override
    public void initChildren() {
        urlInput = new TextField("请输入连接地址");
        linkClusterComboBox = new ComboBox<>();
        //根据缓存拿数据填充
        List<LinkClusterInfo> clusterInfoList = LinkClusterCache.getAll();
        if (CollectionUtils.isNotEmpty(clusterInfoList)) {
            List<String> baseUrlList = clusterInfoList.stream().map(LinkClusterInfo::getBaseUrl).toList();
            linkClusterComboBox.setItems(FXCollections.observableArrayList(baseUrlList));
            linkClusterComboBox.getSelectionModel().selectFirst();
            urlInput.setText(linkClusterComboBox.getValue());
        }
        connBtn = new Button("连 接");
        getChildren().addAll(urlInput,linkClusterComboBox,connBtn);
    }
    @Override
    public void initEvent() {
        //输入框和下拉框联动
        linkClusterComboBox.setOnAction(actionEvent -> {
            String selectedItem = linkClusterComboBox.getSelectionModel().getSelectedItem();
            urlInput.setText(selectedItem);
        });

        connBtn.setOnMouseClicked(mouseEvent -> {
            //判断是否已存在
            LinkClusterInfo currentLinkClusterInfo = LinkClusterCache.getByBaseUrl(urlInput.getText().trim());
            //如果已存在，则尝试登录，登录成功发消息，不成功，则继续走逻辑
            if (currentLinkClusterInfo != null) {
                //拿已有参数直接连接
                ResponseResult<String> responseResult =
                        RemoteCallApi.loginWithAuthHead(currentLinkClusterInfo.getBaseUrl(),
                                currentLinkClusterInfo.getAuthHead());
                if (responseResult.getHttpStatus() == 200) {
                    Event<LinkClusterInfo>event = new Event<>(EventType.CONNECT_SUCCESS_CLUSTER,currentLinkClusterInfo);
                    DefaultEventBus.getInstance().sendEvent(event);
                    return;
                }
            }
            //如果没有登录成功，则拼接参数去请求
            ResponseResult<String> responseResult = RemoteCallApi.loginWithBaseUrl(urlInput.getText().trim());
            if (responseResult.getHttpStatus() == 200) {
                //不需要输入用户名和密码，直接成功
                LinkClusterInfo linkClusterInfo = new LinkClusterInfo();
                linkClusterInfo.setBaseUrl(urlInput.getText().trim());
                linkClusterInfo.setClusterName("");
                linkClusterInfo.setUsername("");
                linkClusterInfo.setPassword("");
                linkClusterInfo.setAuthHead("");
                LinkClusterCache.addToCache(linkClusterInfo);
                Event<LinkClusterInfo>event = new Event<>(EventType.CONNECT_SUCCESS_CLUSTER,linkClusterInfo);
                DefaultEventBus.getInstance().sendEvent(event);

            } else if (responseResult.getHttpStatus() == 401) {
                buildLoginDialog( urlInput.getText().trim());
            } else {
                System.out.println("other scene ..." + responseResult.getData());
            }

        });
    }
    private   void buildLoginDialog( String baseUrl) {
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
        dialog.initOwner(this.stage);
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
                Event<LinkClusterInfo>event = new Event<>(EventType.CONNECT_SUCCESS_CLUSTER,linkClusterInfo);
                DefaultEventBus.getInstance().sendEvent(event);
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("错误");
                alert.setHeaderText("用户名和密码错误");
                alert.setContentText("请检查用户名和密码后重新输入");
                alert.showAndWait();
            }
        });
    }

}
