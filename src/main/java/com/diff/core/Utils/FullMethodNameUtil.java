package com.diff.core.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.diff.core.Common.Code.PACKAGE_SPLIT;
import static com.diff.core.Common.Code.descriptorMap;

public class FullMethodNameUtil {
    public static void main(String[] args){
        String test = "Ljava/lang/String;Ljava/lang/String;";
        String test1 = "Ljava/lang/String;Ljava/lang/String;[Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;";
        String test2 = "Ljava/lang/String;[Ljava/lang/String;[Ljava/util/Map<Ljava/lang/String;Ljava/util/Map<[Ljava/lang/String;Ljava/lang/String;>;>;";
        System.out.println(genericsParameter(test2));
    }

    public static String getMethodSignatureName(String name, String descriptor){
        StringBuilder signa = new StringBuilder();
        signa.append(name);
        signa.append("(");
        String patter = "(L.*?;|\\[{0,2}L.*?;|[ZCBSIFJDV]|\\[{0,2}[ZCBSIFJDV]{1})";
        Matcher parameterMatcher = Pattern.compile(patter).matcher(descriptor.substring(0, descriptor.lastIndexOf(')') + 1));
        while(parameterMatcher.find()){
            String param = parameterMatcher.group(1);
            if(param.length()==1){
                // V
                signa.append(descriptorMap.get(param)).append(", ");
            }else{
                String type = param.substring(0,1);
                if(descriptorMap.get(param.substring(1)) != null){
                    // [V
                    signa.append(descriptorMap.get(param.substring(1))).append(", ");
                }else{
                    // Ljava/lang/Object;     [Ljava/lang/Object;
                    String typeName = param.substring(param.lastIndexOf(PACKAGE_SPLIT) + 1, param.length()-1);
                    signa.append(typeName).append(descriptorMap.get(type)).append(", ");
                }
            }
        }
        if(!descriptor.startsWith("()"))
            signa.delete(signa.length()-2, signa.length());
        signa.append(")");
        String returnType = descriptor.substring(descriptor.lastIndexOf(')') + 1);
        if(returnType.length()==1){
            signa.append(descriptorMap.get(returnType));
        }else{
            String type = returnType.substring(0,1);
            if(descriptorMap.get(returnType.substring(1)) != null){
                signa.append(descriptorMap.get(returnType.substring(1))).append("[]");
            }else{
                String typeName = returnType.substring(returnType.lastIndexOf(PACKAGE_SPLIT) + 1, returnType.length()-1);
                signa.append(typeName).append(descriptorMap.get(type));
            }
        }

        return signa.toString();
    }

    public static String getMethodSignatureName(String name, String descriptor, String signature){
        StringBuilder fullName = new StringBuilder("");
        fullName.append(name).append("(");
        String pattern = "(L[^;]+<.*?(>;)+|\\[{0,2}L[^;]+<.*?(>;)+|L.*?;|\\[{0,2}L.*?;|[ZCBSIFJDV]|\\[{0,2}[ZCBSIFJDV]{1})";
        Matcher matcher;
        if(signature != null){
            matcher = Pattern.compile(pattern).matcher(signature);
        }else{
            matcher = Pattern.compile(pattern).matcher(descriptor);
        }
        while(matcher.find()){
            String parameter = matcher.group(1);
            if(parameter.length()==1){
                // V
                fullName.append(descriptorMap.get(parameter)).append(", ");
            }else{
                String type = parameter.substring(0,1);
                if(descriptorMap.get(parameter.substring(1)) != null){
                    // [V
                    fullName.append(descriptorMap.get(parameter.substring(1))).append(", ");
                }else{
                    // Ljava/lang/Object;     [Ljava/lang/Object;    [Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;
                    if(parameter.contains("<")){
//                        [Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;
                        String tmp = parameter.substring(0, parameter.indexOf("<"));
                        String typeName = tmp.substring(tmp.lastIndexOf(PACKAGE_SPLIT) + 1)
                                + FullMethodNameUtil.genericsParameter(parameter.substring(
                                        parameter.indexOf("<")+1, parameter.length()-2
                        ));

                    }
                }
            }
        }

        return fullName.toString();
    }

    /**
     * 栈+循环处理泛型的入参
     * @param signature "Ljava/lang/String;Ljava/lang/String;"
     * @return  "<String, String>" "<String, Map<String, String>>"
     */
    public static String genericsParameter(String signature){
        // <*> <?> <>
        if(signature.length() < 2)
            return signature;
        StringBuilder result = new StringBuilder("<");
        List<String> parameterList = new ArrayList<>();
        Stack<String> mainStack = new Stack<>();
        Stack<Node> subStack = new Stack<>();
        Pattern pattern = Pattern.compile("(L[^;]+<.*?(>;)+|\\[{0,2}L[^;]+<.*?(>;)+|L.*?;|\\[{0,2}L.*?;|[ZCBSIFJDV]|\\[{0,2}[ZCBSIFJDV]{1})");
        Matcher matcher = pattern.matcher(signature);
        // 初始化stack
        while(matcher.find())
            mainStack.push(matcher.group(1));
        while(!mainStack.empty()){
            // 遍历stack处理泛型的入参
            String parameter = mainStack.pop();
            if(parameter.contains("<")){
                // 又是带泛型的参数，解包并加入mainStack，subStack相应地加入本节点的className Node
                Matcher subMatcher = pattern.matcher(parameter.substring(parameter.indexOf('<')+1, parameter.length()-2));
                String className = parameter.substring(0, parameter.indexOf("<"));
                int count = 0;
                while(subMatcher.find()){
                    count++;
                    String find = subMatcher.group(1);
                    mainStack.push(find);
                }
                subStack.push(new Node(className, count));
            }else{
                // 普通参数
                String name = parameter.substring(parameter.lastIndexOf(PACKAGE_SPLIT)+1, parameter.length()-1);
                if(descriptorMap.get(parameter.substring(0,1)).equals("[]"))
                    name += "[]";
                if(!subStack.empty()){
                    Node top = subStack.peek();
                    top.push(name);
                    if(top.isEnd()){
                        subStack.pop();
                        if(!subStack.empty())
                            subStack.peek().push(top.getFullName());
                        else
                            parameterList.add(top.getFullName());
                    }
                }else{
                    parameterList.add(name);
                }
            }
        }

        for(int i=parameterList.size()-1;i>=0;i--){
            if(result.toString().equals("<"))
                result.append(parameterList.get(i));
            else
                result.append(", ").append(parameterList.get(i));
        }
        result.append(">");
        return result.toString();
    }

    private static class Node{
        private final String className;
        private int count = 0 ;
        private List<String> subParameter = new ArrayList<>();
        public Node(String className, int count){
            this.className = className;
            this.count = count;
        }

        public String getClassName() {
            return className;
        }

        public int getCount() {
            return count;
        }

        public void push(String parameter){
            subParameter.add(parameter);
            count--;
        }

        public String getFullName() {
            StringBuilder stringBuilder = new StringBuilder("<");
            for(int i=subParameter.size()-1;i>=0;i--){
                if(stringBuilder.toString().equals("<"))
                    stringBuilder.append(subParameter.get(i));
                else
                    stringBuilder.append(", ").append(subParameter.get(i));
            }
            stringBuilder.append(">");

            if(descriptorMap.get(className.substring(0,1)).equals("[]")){
                stringBuilder.append("[]");
            }
            return className.substring(className.lastIndexOf(PACKAGE_SPLIT)+1) + stringBuilder;
        }

        public boolean isEnd(){
            return count == 0;
        }
    }
}
