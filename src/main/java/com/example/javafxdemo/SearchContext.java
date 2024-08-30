package com.example.javafxdemo;

import lombok.Data;

import java.util.List;

@Data
public class SearchContext {

    private LinkClusterInfo linkClusterInfo;
    private List<IndexInfo> indexInfoList;
}
