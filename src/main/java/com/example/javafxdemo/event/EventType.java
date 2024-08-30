package com.example.javafxdemo.event;

public enum EventType {
    /**连接ES集群成功*/
    CONNECT_SUCCESS_CLUSTER,
    /**连接ES集群失败*/
    CONNECT_FAIL_CLUSTER,
    /**拿到集群所有的索引*/
    GET_CLUSTER_ALL_INDEX,
    /**根据指定的索引查数据*/
    QUERY_WITH_SPECIAL_INDEX,
    /**初始化集群对应的索引*/
    INIT_CLUSTER_INDEX;

}
