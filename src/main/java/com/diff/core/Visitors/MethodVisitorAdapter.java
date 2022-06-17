package com.diff.core.Visitors;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.HashMap;
import java.util.Map;

import static com.diff.core.Common.Code.*;

public class MethodVisitorAdapter extends VoidVisitorAdapter<String> {
    private final Map<String, MethodDeclaration> mds = new HashMap<>();

    @Override
    public void visit(MethodDeclaration n, String arg){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(");
        NodeList<Parameter> parameters = n.getParameters();
        for(Parameter parameter:parameters){
            String parameterType = parameter.getType().toString();
            if(parameterType.contains("<"))
                parameterType = parameterType.replace(parameterType.substring(parameterType.indexOf('<'), parameterType.lastIndexOf('>')+1), "");
            if(!stringBuilder.toString().equals("("))
                stringBuilder.append(", ");
            stringBuilder.append(parameterType);
        }
        stringBuilder.append(")");
        String returnType = n.getType().toString();
        if(returnType.contains("<"))
            returnType = returnType.replace(returnType.substring(returnType.indexOf('<'), returnType.lastIndexOf('>')+1), "");
        stringBuilder.append(returnType);
        mds.put(arg + METHOD_SPLIT + n.getName().toString() + stringBuilder, n);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, String arg){
        if(arg.substring(arg.length()-1).equals(PACKAGE_SPLIT))
            super.visit(n, arg + n.getName().toString());
        else
            super.visit(n, arg + SUB_CLASS_SPLIT + n.getName().toString());
    }

    public Map<String, MethodDeclaration> getMds() {
        return mds;
    }
}
