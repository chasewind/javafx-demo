package com.example.javafxdemo;

import lombok.Data;

@Data
public class ResponseResult<T> {

    private int httpStatus;
    private T data;
}
