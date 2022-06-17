package com.diff.core.Utils;

import com.alibaba.fastjson.JSONObject;
import com.diff.core.Recorders.AbstractRecord;
import com.diff.core.Recorders.InterfaceRecord;
import com.diff.core.Visitors.AsmClassVisitor;
import com.diff.core.Visitors.AsmSearchFilterClass;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import static com.diff.core.Common.Code.*;

public class ChainUtils {
    /**
     * {
     *     "botApi|(Lcom/alibaba/fastjson/JSONObject;)Ljava/lang/String;":[
     *         "com/bot/server/qqBot/envGet:getByEnv|(Ljava/lang/String;)Ljava/lang/String;",
     *         "com/bot/server/qqBot/mapper/postMethod:run|(Lcom/alibaba/fastjson/JSONObject;Lcom/alibaba/fastjson/JSONObject;Ljava/lang/Integer;)Ljava/lang/String;"
     *     ],
     *     "saveToMysql|()Z":[
     *         "com/bot/server/qqBot/server/msgManage:saveToMysqlSignal|()V"
     *     ],
     *     "test|(Ljava/lang/String;)Ljava/lang/String;":[
     *
     *     ],
     *     "|()V":[
     *
     *     ],
     *     "showList|()Ljava/lang/String;":[
     *         "com/bot/server/qqBot/server/msgManage:getMsgList|()Ljava/util/Map;"
     *     ],
     *     "showEnvs|()Ljava/util/Map;":[
     *         "com/bot/server/qqBot/envGet:getByEnv|(Ljava/lang/String;)Ljava/lang/String;"
     *     ],
     *     "checkThread|(Ljava/lang/String;)Ljava/util/Map;":[
     *         "com/bot/server/qqBot/server/msgManage:getAllThreadStatus|()Ljava/util/Map;",
     *         "com/bot/server/qqBot/server/msgManage:getGroupThreadStatus|(Ljava/lang/String;)Ljava/util/Map;"
     *     ]
     * }
     * @param classfileBuffer
     * @return
     */
    public static Map<String, List<String>> getRelationShipFromClassBuffer(byte[] classfileBuffer){
        ClassReader cr = new ClassReader(classfileBuffer);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        AsmClassVisitor cv = new AsmClassVisitor(cw);
        cr.accept(cv, ClassReader.SKIP_FRAMES);

        return cv.getMethodRelations();
    }


    /**
     * 先visit一遍，把项目的所有className存到FilterUtils中
     * @param classfileBuffer
     */
    public static void scanForClassName(byte[] classfileBuffer){
        ClassReader cr = new ClassReader(classfileBuffer);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new AsmSearchFilterClass(cw);
        cr.accept(cv, ClassReader.SKIP_FRAMES);
    }

    /**
     * {
     *     "com/bot/server/qqBot/envGet:getByEnv|(Ljava/lang/String;)Ljava/lang/String;":{
     *         "com/bot/server/qqBot/mapper/postMethod:run|(Lcom/alibaba/fastjson/JSONObject;Lcom/alibaba/fastjson/JSONObject;Ljava/lang/Integer;)Ljava/lang/String":{
     *             ...
     *         }
     *     },
     *     "com/bot/server/qqBot/envGet:test|(Ljava/lang/String;)Ljava/lang/String;":{
     *         ...
     *     }
     * }
     * @param relationShips
     * @param startFullMethodName
     * @return
     */
    public static JSONObject getJSONChainFromRelationShip(Map<String, Map<String, List<String>>> relationShips, String startFullMethodName){
        InterfaceRecord interfaceRecord = InterfaceRecord.getInstance();
        AbstractRecord abstractRecord = AbstractRecord.getInstance();

        String className = startFullMethodName.substring(0, startFullMethodName.indexOf(METHOD_SPLIT));
        String methodName = startFullMethodName.substring(startFullMethodName.indexOf(METHOD_SPLIT)+1);
        Stack<StackJSONNode> stack = new Stack<>();
        JSONObject relation = new JSONObject();
        List<String> startChain = new ArrayList<>();
        startChain.add(startFullMethodName);
        StackJSONNode initNode = new StackJSONNode(className, methodName, startChain, relation);
        stack.push(initNode);
        while(!stack.empty()){
            StackJSONNode currentNode = stack.pop();
            String currentClassName = currentNode.getClassName();
//            if(currentClassName.equals("com/hiido/controllers/center/UserCenterController"))
//                System.out.println("haha");
            String currentMethodName = currentNode.getMethodName();
            List<String> currentChain = currentNode.getChain();
            List<String> methodRelationShip = relationShips.get(currentClassName).get(currentMethodName);
            JSONObject currentRelation = currentNode.getChainNode();
            // 处理接口或抽象类
            if(interfaceRecord.containInterface(currentClassName) || abstractRecord.containAbstract(currentClassName)){
                List<String> entries = interfaceRecord.getEntries(currentClassName);
                Map<String, Boolean> abstractMethod = interfaceRecord.getMethod(currentClassName);
                if(entries==null){
                    entries = abstractRecord.getEntries(currentClassName);
                    abstractMethod = abstractRecord.getMethod(currentClassName);
                }
                for(String entryClassName:entries){
                    // 存在default方法，需要判断是否被实体类复写
                    if(!abstractMethod.get(currentMethodName)){
                        // default或者抽象类中有实体的方法
                        if(relationShips.get(entryClassName).containsKey(currentMethodName)){
                            // 实体类有复写这个方法，但是静态分析下无法判断实际跑的是哪一个方法，所以都要加进去
                            methodRelationShip.add(entryClassName + METHOD_SPLIT + currentMethodName);
                        }
                    }else{
                        methodRelationShip.add(entryClassName + METHOD_SPLIT + currentMethodName);
                    }
                }
            }
            if(methodRelationShip.size()>0){
                for(String fullMethodName:methodRelationShip){
                    // 解开methodRelationShip，加到stack中，并与往json里添加
                    JSONObject tmpRelation = new JSONObject();
                    currentRelation.put(fullMethodName, tmpRelation);
                    if(!currentChain.contains(fullMethodName)){
                        String tmpClassName = fullMethodName.substring(0, fullMethodName.indexOf(METHOD_SPLIT));
                        String tmpMethodName = fullMethodName.substring(fullMethodName.indexOf(METHOD_SPLIT)+1);
                        List<String> tmpChain = new ArrayList<String>(currentChain);
                        tmpChain.add(fullMethodName);
                        stack.add(new StackJSONNode(tmpClassName, tmpMethodName, tmpChain, tmpRelation));
                    }
                }
            }
        }
        return relation;
    }

    private static class StackJSONNode{
        private String className;
        private String methodName;
        private List<String> chain;
        private JSONObject chainNode;
        public StackJSONNode(String className, String methodName, List<String> chain, JSONObject chainNode){
            this.className = className;
            this.methodName = methodName;
            this.chain = chain;
            this.chainNode = chainNode;
        }

        public List<String> getChain() {
            return chain;
        }

        public String getClassName() {
            return className;
        }

        public String getMethodName() {
            return methodName;
        }

        public JSONObject getChainNode() {
            return chainNode;
        }
    }
}
