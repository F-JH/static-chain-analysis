package com.diff.core.Utils;

import com.alibaba.fastjson.JSONObject;
import com.diff.core.Common.Config;
import com.diff.core.Recorders.ControllerRecord;
import com.diff.core.Recorders.DubboRecord;
import org.apache.commons.io.FileUtils;
import org.dom4j.DocumentException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.NotDirectoryException;
import java.util.*;

import static com.diff.core.Common.Code.*;

public class ProjectChainUtils {
    public static Map<String, JSONObject> getProjectChainFromPath() throws IOException {
        Config config = Config.getInstance();
        String rootDir = config.getString("newProjectPath") + URL_SPLIT + config.getString("target");
        List<String> filePaths = FileTreeUtil.scanForDirectory(rootDir);
        Map<String, Map<String, List<String>>> relationShips = new HashMap<>();

        for(String filePath:filePaths){
            // 扫描项目全部class
            ChainUtils.scanForClassName(FileUtils.readFileToByteArray(new File(filePath)));
        }

        for(String filePath:filePaths){
            // 扫描方法之间的调用关系
            String className = filePath.substring(rootDir.length()+1,filePath.lastIndexOf('.'));
            if(!URL_SPLIT.equals(PACKAGE_SPLIT))
                className = className.replace(URL_SPLIT, PACKAGE_SPLIT);
            relationShips.put(className, ChainUtils.getRelationShipFromClassBuffer(FileUtils.readFileToByteArray(new File(filePath))));
        }
        Map<String, JSONObject> result = new HashMap<>();
        ControllerRecord controllerRecord = ControllerRecord.getInstance();
        for(String controllerName:controllerRecord.getControllers()){
            for(String methodName:controllerRecord.getApiFromControlClassName(controllerName)){
                String fullMethodName = controllerName + METHOD_SPLIT + methodName;
                result.put(fullMethodName, ChainUtils.getJSONChainFromRelationShip(relationShips, fullMethodName));
            }
        }
        return result;
    }

    public static List<String> getProjectUpdateMethod(String oldProject, String newProject) throws NotDirectoryException, FileNotFoundException {
        List<String> result = new ArrayList<>();
        Config config = Config.getInstance();
        Map<String, List<String>> classDiff = FileTreeUtil.compireToPath(
                oldProject + URL_SPLIT + config.getString("source"),
                newProject + URL_SPLIT + config.getString("source")
        );
        List<String> scanFiles = new ArrayList<>();

        List<String> newDirectorys = classDiff.get("newDirectorys");
        List<String> newFiles = classDiff.get("newFiles");
        List<String> modifyFiles = classDiff.get("modifyFiles");

        for(String directory:newDirectorys){
            List<String> files = FileTreeUtil.scanForDirectory(newProject + URL_SPLIT + config.getString("source") + directory);
            if(files != null)
                scanFiles.addAll(files);
        }
        // 对比 modifyFiles 获取有修改的method
        for(String file:modifyFiles){
            Map<String, Boolean> modifyMethods = ParseUtil.compireToMethod(
                    new File(oldProject + URL_SPLIT + config.getString("source") + file),
                    new File(newProject + URL_SPLIT + config.getString("source") + file)
            );
            for(String method:modifyMethods.keySet()){
                if(!modifyMethods.get(method)){
                    result.add(method);
                }
            }
        }
        // 新目录下的所有method
        for(String file:scanFiles){
            List<String> scanMethods = ParseUtil.scanMethods(file);
            result.addAll(scanMethods);
        }
        // 新java文件的所有method
        for(String file:newFiles){
            List<String> newMethods = ParseUtil.scanMethods(
                    newProject + URL_SPLIT + config.getString("source") + file
            );
            result.addAll(newMethods);
        }
        return result;
    }

    public static Map<String, JSONObject> getProjectChainFromPath(List<String> modules) throws Exception {
        Config config = Config.getInstance();
        Map<String, JSONObject> result = new HashMap<>();
        Map<String, Map<String, List<String>>> relationShips = new HashMap<>();

        for(String module:modules){
            String rootDir = module + URL_SPLIT + config.getString("target");
            List<String> filePaths = FileTreeUtil.scanForDirectory(rootDir);
            for(String filePath:filePaths){
                // 扫描当前模块全部class
                ChainUtils.scanForClassName(FileUtils.readFileToByteArray(new File(filePath)));
            }
        }

        for(String module:modules){
            String rootDir = module + URL_SPLIT + config.getString("target");
            List<String> filePaths = FileTreeUtil.scanForDirectory(rootDir);
            for(String filePath:filePaths){
                // 扫描方法之间的调用关系
                String className = filePath.substring(rootDir.length()+1,filePath.lastIndexOf('.'));
                if(!URL_SPLIT.equals(PACKAGE_SPLIT))
                    className = className.replace(URL_SPLIT, PACKAGE_SPLIT);
                relationShips.put(className, ChainUtils.getRelationShipFromClassBuffer(FileUtils.readFileToByteArray(new File(filePath))));
            }
        }
        ControllerRecord controllerRecord = ControllerRecord.getInstance();
        for(String controllerName:controllerRecord.getControllers()){
            for(String methodName:controllerRecord.getApiFromControlClassName(controllerName)){
                String fullMethodName = controllerName + METHOD_SPLIT + methodName;
                result.put(fullMethodName, ChainUtils.getJSONChainFromRelationShip(relationShips, fullMethodName));
            }
        }
        // 扫描dubbo调用链
        for(String dubboMethodName: DubboRecord.getList()){
            result.put(dubboMethodName, ChainUtils.getJSONChainFromRelationShip(relationShips, dubboMethodName));
        }
        return result;
    }

    public static List<String> getProjectUpdateMethod(List<String> modules) throws NotDirectoryException, FileNotFoundException, DocumentException {
        Config config = Config.getInstance();
        List<String> result = new ArrayList<>();
        String newProject = config.getString("newProjectPath");
        String oldProject = config.getString("oldProjectPath");
        for(String module:modules){
            String oldModule = oldProject + module.substring(newProject.length());
            Map<String, List<String>> classDiff = FileTreeUtil.compireToPath(
                    oldModule + URL_SPLIT + config.getString("source"),
                    module + URL_SPLIT + config.getString("source")
            );
            List<String> newDirectorys = classDiff.get("newDirectorys");
            List<String> newFiles = classDiff.get("newFiles");
            List<String> modifyFiles = classDiff.get("modifyFiles");
            List<String> scanFiles = new ArrayList<>();

            for(String directory:newDirectorys){
                List<String> files = FileTreeUtil.scanForDirectory(module + URL_SPLIT + config.getString("source") + directory);
                if(files != null)
                    scanFiles.addAll(files);
            }
            // 对比 modifyFiles 获取有修改的method
            for(String file:modifyFiles){
                String moduleName = module.equals(newProject) ? "" : module.substring(newProject.length()+1);
                Map<String, Boolean> modifyMethods = ParseUtil.compireToMethod(
                        new File(oldModule + URL_SPLIT + config.getString("source") + file),
                        new File(module + URL_SPLIT + config.getString("source") + file),
                        moduleName
                );
                for(String method:modifyMethods.keySet()){
                    if(!modifyMethods.get(method)){
                        result.add(method);
                    }
                }
            }
            // 新目录下的所有method
            for(String file:scanFiles){
                List<String> scanMethods = ParseUtil.scanMethods(file);
                result.addAll(scanMethods);
            }
            // 新java文件的所有method
            for(String file:newFiles){
                List<String> newMethods = ParseUtil.scanMethods(
                        module + URL_SPLIT + config.getString("source") + file
                );
                result.addAll(newMethods);
            }
            // 检索所有mybatis xml配置
            String oldResource = oldModule + URL_SPLIT + config.getString("resources");
            String newResource = module + URL_SPLIT + config.getString("resources");

            if(new File(oldResource).exists() && new File(newResource).exists()){
                Map<String, String> oldMybatisXml = FileTreeUtil.scanMybatisXml(oldResource);
                Map<String, String> newMybatisXml = FileTreeUtil.scanMybatisXml(newResource);

                for(String xml:newMybatisXml.keySet()){
                    if(oldMybatisXml.containsKey(xml)){
                        List<String> xmlDiff =  XmlDiffUtils.compireXml(oldMybatisXml.get(xml), newMybatisXml.get(xml));
                        for(String methodName:xmlDiff){
                            List<String> methods = ParseUtil.scanMethods(
                                    module + URL_SPLIT + config.getString("source") + URL_SPLIT + methodName.substring(0, methodName.lastIndexOf(METHOD_SPLIT)) + ".java"
                            );
                            for(String fullMethodName:methods){
                                if(fullMethodName.startsWith(methodName)){
                                    result.add(fullMethodName);
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
}
