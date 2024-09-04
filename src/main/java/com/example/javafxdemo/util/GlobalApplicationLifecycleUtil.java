package com.example.javafxdemo.util;

import java.util.ArrayList;

/**
 * 全局的应用生命周期管理器
 */
public class GlobalApplicationLifecycleUtil {

    private static final ArrayList<Runnable> onCloseCallback = new ArrayList<>();

    public static void addOnCloseListener(Runnable runnable){
        onCloseCallback.add(runnable);
    }

    public static void stop(){
        for (Runnable runnable : onCloseCallback) {
            runnable.run();
        }
    }

}
