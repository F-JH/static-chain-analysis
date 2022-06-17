package com.diff.core.Recorders;

import java.util.*;

/**
 * 保存所有Controller方法，在调用链入口可使用
 */
public class ControllerRecord {
    private Map<String, List<String>> controllers = new HashMap<>();
    private static final ControllerRecord instance = new ControllerRecord();

    private ControllerRecord(){}
    public static ControllerRecord getInstance(){
        return instance;
    }

    public void putControlClass(String ControlClassName){
        controllers.computeIfAbsent(ControlClassName, k -> new ArrayList<>());
    }

    public void putControlMethod(String ControlClassName, String ControlMethodName){
        controllers.computeIfAbsent(ControlClassName, k -> new ArrayList<>());
        controllers.get(ControlClassName).add(ControlMethodName);
    }

    public List<String> getApiFromControlClassName(String ControlClassName){
        return controllers.get(ControlClassName);
    }

    public Set<String> getControllers(){
        return controllers.keySet();
    }
}
