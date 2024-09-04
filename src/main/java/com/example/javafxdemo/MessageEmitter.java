package com.example.javafxdemo;

import com.example.javafxdemo.util.GlobalApplicationLifecycleUtil;
import javafx.application.Platform;
import javafx.scene.control.Label;

import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

public class MessageEmitter {

    private final Label messageLabel;

    private final Timer timer = new Timer();

    public MessageEmitter(Label messageLabel) {
        this.messageLabel = messageLabel;
        GlobalApplicationLifecycleUtil.addOnCloseListener(timer::cancel);
    }

    public void emitInfo(String msg){
        messageLabel.setText(msg);
        this.clearMsg();
    }

    public void emitWarn(String msg){
        messageLabel.setText(msg);
        Toolkit.getDefaultToolkit().beep();
        this.clearMsg();
    }

    public void emitError(String msg){
        messageLabel.setText(msg);
        Toolkit.getDefaultToolkit().beep();
        this.clearMsg();
    }

    private void clearMsg(){
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    messageLabel.setText("");
                });
            }
        }, 4000L);
    }

    public void clear(){
        messageLabel.setText("");
    }

}
