package com.example.javafxdemo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class QueryHistoryCache {

    private static final Map<String, QueryHistory> queryHistoryMap = new HashMap<>();

    /**单机线性操作并不存在并发问题，这里不做约束*/
    private static int MAX_POS = 0;

    public static void init() {
        String tempDir = System.getProperty("java.io.tmpdir");
        File queryHistoryFile = new File(tempDir, "es_query_history.bak");
        try {
            if (!queryHistoryFile.exists()) {
                boolean flag = queryHistoryFile.createNewFile();
                System.out.println("make file : " + flag);
            }
            //读取文件
            String queryHistoryStr = FileUtils.readFileToString(queryHistoryFile, "utf-8");
            if (StringUtils.isNotEmpty(queryHistoryStr)) {
                List<QueryHistory> queryHistoryList = new Gson().fromJson(queryHistoryStr,
                        new TypeToken<List<QueryHistory>>() {
                        }.getType());
                if (CollectionUtils.isNotEmpty(queryHistoryList)) {
                    addAll(queryHistoryList);
                }
            }
        } catch (Exception e) {
            log.info("ignore {}", e.getMessage());
        }

    }

    private static void addAll(List<QueryHistory> queryHistoryList) {

        for (QueryHistory queryHistory : queryHistoryList) {
            queryHistoryMap.put(queryHistory.getId(), queryHistory);
            String id = queryHistory.getId();
            String[] array = StringUtils.split(id, "_");
            int pos = Integer.parseInt(array[1]);
            MAX_POS = Math.max(pos, MAX_POS);
        }
    }

    public static List<QueryHistory> getAll() {
        List<QueryHistory> queryHistoryList = new ArrayList<>();
        queryHistoryMap.forEach((k, v) -> {
            queryHistoryList.add(v);
        });
        return queryHistoryList;
    }

    public static void save(QueryHistory queryHistory) {
        MAX_POS = MAX_POS+1;
        queryHistory.setId("history_"+MAX_POS);
        //需要设置一个全局id
        queryHistoryMap.put(queryHistory.getId(), queryHistory);
        //写入文件系统
        String tempDir = System.getProperty("java.io.tmpdir");
        File queryHistoryFile = new File(tempDir, "es_query_history.bak");
        try {
            List<QueryHistory> queryHistoryList = getAll();
            FileUtils.writeStringToFile(queryHistoryFile, new Gson().toJson(queryHistoryList), "utf-8", false);
        } catch (Exception e) {
            log.info("ignore {}", e.getMessage());
        }
    }
}
