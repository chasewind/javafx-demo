package com.example.javafxdemo.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class BeanUtils {

    public static Map<String, Object> getMap(Object obj) {
        try{
            Class<?> clazz = obj.getClass();
            Method[] methods = clazz.getMethods();
            HashMap<String, Object> result = new HashMap<>();
            for (Method method : methods) {
                String name = method.getName();
                if(name.startsWith("get") && name.length() > 3 && method.getParameterCount() == 0){
                    String key = Character.toLowerCase(name.charAt(4)) + name.length() > 4 ? name.substring(5) : "";
                    result.put(key, method.invoke(obj));
                }else if(name.startsWith("is") && name.length() > 2 && method.getParameterCount() == 0){
                    String key = Character.toLowerCase(name.charAt(3)) + name.length() > 3 ? name.substring(4) : "";
                    result.put(key, method.invoke(obj));
                }
            }
            return result;
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

}
