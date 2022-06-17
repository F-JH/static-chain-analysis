package com.diff.core.Common;

import java.util.HashMap;
import java.util.Map;

public class Code {
    // 各种分隔符
    public final static String METHOD_SPLIT = ".";
    public final static String METHOD_SIGNATURE_SPLIT = "|";
    public final static String URL_SPLIT;
    public final static String SUB_CLASS_SPLIT = "$";
    public final static String PACKAGE_SPLIT = "/";
    public final static String POM = "pom.xml";
    public final static String DUBBO = "[dubbo]";
    public final static String HTTP = "[http]";


    public final static Map<String, String> descriptorMap = new HashMap<>();
    static {
        // java基本类型与标识符对照表
        descriptorMap.put("B", "byte");
        descriptorMap.put("C", "char");
        descriptorMap.put("D", "double");
        descriptorMap.put("F", "float");
        descriptorMap.put("I", "int");
        descriptorMap.put("J", "long");
        descriptorMap.put("S", "short");
        descriptorMap.put("Z", "boolean");
        descriptorMap.put("V", "void");
        descriptorMap.put("L", ""); // 表示单个类型
        descriptorMap.put("[", "[]"); // 表示数组类型

        if(System.getProperty("os.name").toLowerCase().startsWith("win")){
            URL_SPLIT = "\\";
        }else{
            URL_SPLIT = "/";
        }
    }
}
