package com.diff.core.Recorders;

import java.util.*;

/**
 * 保存所有Controller类方法与Api路径的对应关系
 */
public class ApiRecord {
    private Map<String, Set<String>> record = new HashMap<>();
    private final static ApiRecord instance = new ApiRecord();

    private ApiRecord(){}

    public static ApiRecord getInstance() {
        return instance;
    }

    public void putApi(String fullMethodName, Set<String> api){
//        record.computeIfAbsent(fullMethodName, k -> new HashSet<>());
        record.put(fullMethodName, api);
    }

    public Set<String> getApis(String fullMethodName){
        return record.get(fullMethodName);
    }
}
