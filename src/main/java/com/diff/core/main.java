package com.diff.core;

import com.alibaba.fastjson.JSONObject;
import com.diff.core.Common.Config;
import com.diff.core.Recorders.ApiRecord;
import com.diff.core.Utils.FileTreeUtil;
import com.diff.core.Utils.ParseUtil;
import com.diff.core.Utils.ProjectChainUtils;

import static com.diff.core.Common.Code.*;

import java.util.*;

public class main {

    public static void main(String[] args) throws Exception {
        Config config = Config.getInstance();
        Map<String, List<String>> modules = FileTreeUtil.compireToPomTree(config.getString("oldProjectPath"), config.getString("newProjectPath"));
        List<String> newModules = modules.get("newModules");
        List<String> normalModules = modules.get("normalModules");

        List<String> allModules = new ArrayList<>();
        allModules.addAll(newModules);
        allModules.addAll(normalModules);
        Map<String, JSONObject> chains = ProjectChainUtils.getProjectChainFromPath(allModules);

        List<String> update = new ArrayList<>();
        for(String module:newModules){
            // 新模块所有文件加入update
            List<String> scan = FileTreeUtil.scanForDirectory(module);
            for(String file:scan){
                List<String> scanMethods = ParseUtil.scanMethods(file);
                update.addAll(scanMethods);
            }
            // 检索mybatis xml配置
        }
        update.addAll(ProjectChainUtils.getProjectUpdateMethod(normalModules));

        System.out.println("涉及到更新的方法: ");
        for(String method:update){
            System.out.println(method);
        }
        System.out.println("建议回归的API: ");
        Set<String> needToTestApi = new HashSet<>();
        for(String updateMethod:update){
            for(String startChain:chains.keySet()){
                if(startChain.equals(updateMethod) || chains.get(startChain).toJSONString().contains(updateMethod)){
                    Set<String> apis = ApiRecord.getInstance().getApis(startChain);
                    if(apis == null){
                        needToTestApi.add(DUBBO + METHOD_SIGNATURE_SPLIT + startChain);
                    }else{
                        for(String api:apis){
                            needToTestApi.add(HTTP + METHOD_SIGNATURE_SPLIT + api);
                        }
                    }
                }
            }
        }
        for(String api:needToTestApi)
            System.out.println(api);
    }
}
