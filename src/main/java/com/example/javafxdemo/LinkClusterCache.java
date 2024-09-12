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
public class LinkClusterCache {
    private static final Map<String, LinkClusterInfo> clusterInfoMap = new HashMap<>();

    public static void init() {
        String tempDir = System.getProperty("user.home");
        File linkClusterFile = new File(tempDir, "link_cluster.bak");
        try {
            if (!linkClusterFile.exists()) {
                boolean flag = linkClusterFile.createNewFile();
                System.out.println("make file : " + flag);
            }
            //读取文件
            String linkClusterStr = FileUtils.readFileToString(linkClusterFile, "utf-8");
            if (StringUtils.isNotEmpty(linkClusterStr)) {
                List<LinkClusterInfo> clusterInfoList = new Gson().fromJson(linkClusterStr,
                        new TypeToken<List<LinkClusterInfo>>() {
                        }.getType());
                if (CollectionUtils.isNotEmpty(clusterInfoList)) {
                    addAll(clusterInfoList);
                }
            }
        } catch (Exception e) {
            log.info("ignore {}", e.getMessage());
        }

    }

    public static List<LinkClusterInfo> getAll() {
        List<LinkClusterInfo> clusterInfoList = new ArrayList<>();
        clusterInfoMap.forEach((k, v) -> {
            clusterInfoList.add(v);
        });
        return clusterInfoList;
    }

    private static void addAll(List<LinkClusterInfo> clusterInfoList) {
        for (LinkClusterInfo linkClusterInfo : clusterInfoList) {
            clusterInfoMap.put(linkClusterInfo.getBaseUrl(), linkClusterInfo);
        }
    }

    public static LinkClusterInfo getByBaseUrl(String baseUrl) {
        return clusterInfoMap.get(baseUrl);
    }

    public static void addToCache(LinkClusterInfo linkClusterInfo) {
        clusterInfoMap.put(linkClusterInfo.getBaseUrl(), linkClusterInfo);
        //写入文件系统
        String tempDir = System.getProperty("user.home");
        File linkClusterFile = new File(tempDir, "link_cluster.bak");
        try {
            List<LinkClusterInfo> clusterInfoList = getAll();
            FileUtils.writeStringToFile(linkClusterFile, new Gson().toJson(clusterInfoList), "utf-8", false);
        } catch (Exception e) {
            log.info("ignore {}", e.getMessage());
        }
    }
}
