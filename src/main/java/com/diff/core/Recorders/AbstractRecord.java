package com.diff.core.Recorders;

import java.util.*;

/**
 * 保存所有抽象类，暂时用不到？
 */
public class AbstractRecord {
    private Map<String, List<String>> record = new HashMap<>();
    private final static AbstractRecord instance = new AbstractRecord();
    private final Map<String, Map<String, Boolean>> abstractMethodList = new HashMap<>();

    private AbstractRecord(){}
    public static AbstractRecord getInstance(){
        return instance;
    }
    public void putAbstractClass(String className){
        record.computeIfAbsent(className, k -> new ArrayList<>());
        abstractMethodList.computeIfAbsent(className, k -> new HashMap<>());
    }
    public void putAbstractEntry(String abstractClassName, String entryClassName){
        record.computeIfAbsent(abstractClassName, k -> new ArrayList<>());
        abstractMethodList.computeIfAbsent(abstractClassName, k -> new HashMap<>());
        record.get(abstractClassName).add(entryClassName);
    }
    public void putMethod(String abstractClassName, String methodName, boolean isAbstract){
        abstractMethodList.get(abstractClassName).put(methodName, isAbstract);
    }
    public boolean containAbstract(String abstractClassName){
        return record.containsKey(abstractClassName);
    }
    public List<String> getEntries(String className){
        return record.get(className);
    }
    public Map<String, Boolean> getMethod(String abstractClassName){
        return abstractMethodList.get(abstractClassName);
    }
}
