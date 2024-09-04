package com.example.javafxdemo.event;

public enum EventType {
    /**连接ES集群成功*/
    CONNECT_SUCCESS_CLUSTER,
    /**连接ES集群失败*/
    CONNECT_FAIL_CLUSTER,
    /**根据指定的索引查数据*/
    QUERY_WITH_SPECIAL_INDEX,
    /**初始化集群对应的索引*/
    INIT_CLUSTER_INDEX,
    /**回到概览页面*/
    BACK_TO_OVERVIEW;
    ;

}
