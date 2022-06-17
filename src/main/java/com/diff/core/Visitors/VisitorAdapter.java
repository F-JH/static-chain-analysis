package com.diff.core.Visitors;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class VisitorAdapter extends VoidVisitorAdapter<JSONArray> {
    @Override
    public void visit(MethodDeclaration n, JSONArray arg){
        JSONObject descript = new JSONObject();
        descript.put("name", n.getName().toString());
        descript.put("type", "method");
        descript.put("signature", n.getSignature().toString());
        arg.add(descript);
        super.visit(n, arg);
    }

    /**
     * [
     *      {
     *          "name": "app",
     *          "type": "class",
     *          "member": [
     *              {
     *                  "name": "main",
     *                  "type": "method",
     *                  "signate": [Stirng[] args, int cnt]
     *               },
     *               {
     *                   "name": "main",
     *                   "type": "method",
     *                   "signate": "[int a, int b]"
     *               },
     *               {
     *                   "name": "tt",
     *                   "type": "class",
     *                   "member": [...]
     *               }
     *           ]
     *       },
     *       {...}
     * ]
     * @param n
     * @param arg
     */
    @Override
    public void visit(ClassOrInterfaceDeclaration n, JSONArray arg){
        JSONObject descript = new JSONObject();
        descript.put("name", n.getName().toString());
        descript.put("type", "class");
        JSONArray Members = new JSONArray();
        descript.put("member", Members);
        arg.add(descript);
        super.visit(n, Members);
    }
}
