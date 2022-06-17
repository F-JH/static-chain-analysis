package com.diff.core.Recorders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 保存所有dubbo的类
 */
public class DubboRecord {
    private static final List<String> dubboMethods = new ArrayList<>();


    public static void putDubboMethod(String fullMethodName){
        dubboMethods.add(fullMethodName);
    }

    public static boolean contains(String fullMethodName){
        return dubboMethods.contains(fullMethodName);
    }

    public static List<String> getList(){
        return dubboMethods;
    }
}
