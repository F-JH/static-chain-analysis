package com.diff.core.Utils;

import com.alibaba.fastjson.JSONObject;
import com.diff.core.Common.Config;
import org.apache.commons.io.FileUtils;
import org.dom4j.DocumentException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static com.diff.core.Common.Code.*;

/**
 * 文件结构树工具
 */
public class FileTreeUtil {
    public static void main(String[] args) throws Exception {
        System.out.println(JSONObject.toJSONString(compireToPomTree("/Users/xiaoandi/github/qqbot/qqbot","/Users/xiaoandi/github/qqbot/diff_test/qqbot")));
    }

    /**
     *
     * @param oldProject
     * @param newProject
     * @return
     * {
     *     "newModules": [
     *          // 新的子模块
     *          "/Users/xiaoandi/github/qqbot/diff_test/qqbot",
     *          ...
     *     ],
     *     "normalModules": [
     *          // 不是新的子模块
     *          "/Users/xiaoandi/github/qqbot/diff_test/qqbot",
     *          ...
     *     ]
     * }
     * @throws Exception
     */
    public static Map<String, List<String>> compireToPomTree(String oldProject, String newProject) throws Exception{
        File oldRoot = new File(oldProject);
        File newRoot = new File(newProject);
        if(!oldRoot.exists() || !newRoot.exists() || !oldRoot.isDirectory() || !newRoot.isDirectory())
            throw new NotDirectoryException("需要传入完整路径");
        Config config = Config.getInstance();
        File newRootPom = new File(newProject + URL_SPLIT + POM);
        String oldRootPomPath = oldProject + URL_SPLIT + POM;
        String newRootPomPath = newProject + URL_SPLIT + POM;

        List<String> newModules = new ArrayList<>();            // 新增的模块，不需要走parser对比，直接检索并加入 new Method 列表
        List<String> normalModules = new ArrayList<>();         // 非新增模块，需要走正常流程
        File newRootSource = new File(newProject + URL_SPLIT + config.getString("source"));
        File oldRootSource = new File(oldProject + URL_SPLIT + config.getString("source"));

        Stack<FileNode> start = new Stack<>();
        if(newRootSource.exists() && newRootSource.listFiles().length > 0){
            // 根目录有源码
            if(oldRootSource.exists() && oldRootSource.listFiles().length > 0){
                // 旧项目根目录也TM有源码
                normalModules.add(newProject);
            }else{
                newModules.add(newProject);
            }
        }
        for(String module:listModules(newRootPom)){
            // 初始化start
            start.push(new FileNode(
                    new StringBuilder(""),
                    new File(newProject + URL_SPLIT + module + URL_SPLIT + POM)
            ));
        }
        while(!start.empty()){
            FileNode topItem = start.pop();
            String currentModuleName = topItem.fileNode.getAbsolutePath();
            currentModuleName = currentModuleName.substring(0, currentModuleName.lastIndexOf(URL_SPLIT));
            currentModuleName = currentModuleName.substring(currentModuleName.lastIndexOf(URL_SPLIT)+1);
            File currentModuleSource = new File(newProject + topItem.relativePath.toString() + URL_SPLIT + currentModuleName + URL_SPLIT + config.getString("source"));
            File oldModuleSource = new File(oldProject + topItem.relativePath.toString() + URL_SPLIT + currentModuleName  + URL_SPLIT + config.getString("source"));
            // 判断当前module是否有源码
            if(currentModuleSource.exists() && currentModuleSource.listFiles().length > 0){
                if(oldModuleSource.exists() && oldModuleSource.listFiles().length > 0){
                    normalModules.add(newProject + topItem.relativePath.toString() + URL_SPLIT + currentModuleName);
                }else{
                    newModules.add(newProject + topItem.relativePath.toString() + URL_SPLIT + currentModuleName);
                }
            }
            List<String> list = listModules(topItem.fileNode);

            for(String subModule:list){
                if(!URL_SPLIT.equals(PACKAGE_SPLIT)){
                    subModule = subModule.replace(PACKAGE_SPLIT, URL_SPLIT);
                }
                String currentPath = topItem.relativePath.append(URL_SPLIT).append(currentModuleName).toString();
                start.push(new FileNode(
                        new StringBuilder(currentPath),
                        new File(newProject + currentPath + URL_SPLIT + subModule + URL_SPLIT + POM)
                ));
            }
        }
        Map<String, List<String>> result = new HashMap<>();
        result.put("newModules", newModules);
        result.put("normalModules", normalModules);
        return result;
    }

    /**
     * [完整目录]
     * @param dirPath
     * @return
     * @throws NotDirectoryException
     */
    public static List<String> scanForDirectory(String dirPath) throws NotDirectoryException {
        List<String> result = new ArrayList<>();
        File dirFile = new File(dirPath);
        if (!dirFile.exists() || !dirFile.isDirectory())
            throw new NotDirectoryException("「" + dirPath + "」不是文件夹");
        File[] files = dirFile.listFiles();
        if(files==null)
            return null;
        Stack<FileNode> stack = new Stack<>();
        for(File file:files)
            stack.push(new FileNode(new StringBuilder(""), file));
        while(!stack.empty()){
            FileNode topItem = stack.pop();
            if(topItem.fileNode.isFile()){
                String fileName = topItem.fileNode.getName();
                if(fileName.substring(fileName.lastIndexOf('.')).equals(".java") || fileName.substring(fileName.lastIndexOf('.')).equals(".class")){
                    String topItemPath = topItem.relativePath.append(URL_SPLIT).append(fileName).toString();
                    result.add(dirPath + topItemPath);
                }
            }else{
                String dirName = topItem.fileNode.getName();
                String topItemPath = topItem.relativePath.append(URL_SPLIT).append(dirName).toString();
                File[] fs = topItem.fileNode.listFiles();
                for(File f:fs)
                    stack.push(new FileNode(new StringBuilder(topItemPath), f));
            }
        }
        return result;
    }

    /**
     * 比较两个文件夹的目录结构
     * @param oldPath   旧路径，需要传入完整路径
     * @param newPath   新路径，需要传入完整路径
     * @return  只会记录新建的java文件、修改的java文件以及新建的文件夹，不会返回删除的内容
     * @throws NotDirectoryException
     */
    public static Map<String, List<String>> compireToPath(String oldPath, String newPath) throws NotDirectoryException {
        File oldRoot = new File(oldPath);
        File newRoot = new File(newPath);
        if(!oldRoot.exists() || !newRoot.exists() || !oldRoot.isDirectory() || !newRoot.isDirectory())
            throw new NotDirectoryException("需要传入完整路径");

        Map<String, List<String>> result = new HashMap<>();
        List<String> newFiles = new ArrayList<>();
        List<String> modifyFiles = new ArrayList<>();
        List<String> newDirectorys = new ArrayList<>();

        Stack<FileNode> newStack = new Stack<>();
        // 初始化Stack
        File[] newRootFiles = newRoot.listFiles();
        if(newRootFiles==null)
            return null;
        for(File f:newRootFiles)
            newStack.push(new FileNode(new StringBuilder(""), f));
        while(!newStack.empty()){
            FileNode topItem = newStack.pop();
            if(topItem.fileNode.isFile()){
                // 文件类型只处理.java文件，其他的不管
                // 能来到这说明至少在oldPath中有同个文件夹
                String fileName = topItem.fileNode.getName();
                if(fileName.substring(fileName.lastIndexOf('.')).equals(".java")){
                    // 检查 oldPath 中是否有此java文件，有的话再对比byte是否有修改
                    String topItemPath = topItem.relativePath.append(URL_SPLIT).append(fileName).toString();
                    File oldFile = new File(oldPath + topItemPath);
                    if(oldFile.exists()){
                        if(!Arrays.equals(getFileBytes(oldFile), getFileBytes(topItem.fileNode)))
                            modifyFiles.add(topItemPath);
                    }else{
                        newFiles.add(topItemPath);
                    }
                }
            }else{
                // 如果是文件夹，先判断oldPath里是否存在，不存在就是新增的文件夹，直接添加到newDirectory中，不用遍历文件
                // 如果不是新增的，则把文件夹内的元素解开，添加到Stack中
                String dirName = topItem.fileNode.getName();
                String topItemPath = topItem.relativePath.append(URL_SPLIT).append(dirName).toString();
                File oldDir = new File(oldPath + topItemPath);
                if(oldDir.exists()){
                    File[] files = topItem.fileNode.listFiles();
                    for(File file:files)
                        newStack.push(new FileNode(new StringBuilder(topItemPath), file));
                }else{
                    newDirectorys.add(topItemPath);
                }
            }
        }

        result.put("newFiles", newFiles);
        result.put("modifyFiles", modifyFiles);
        result.put("newDirectorys", newDirectorys);
        return result;
    }

    public static Map<String, String> scanMybatisXml(String path) throws NotDirectoryException, DocumentException {
        Map<String, String> result = new HashMap<>();
        File dirFile = new File(path);
        if (!dirFile.exists() || !dirFile.isDirectory())
            throw new NotDirectoryException("「" + path + "」不是文件夹");
        File[] files = dirFile.listFiles();
        if(files==null)
            return null;
        Stack<FileNode> stack = new Stack<>();
        for(File file:files)
            stack.push(new FileNode(new StringBuilder(""), file));
        while(!stack.empty()){
            FileNode topItem = stack.pop();
            if(topItem.fileNode.isFile()){
                String fileName = topItem.fileNode.getName();
                if(fileName.substring(fileName.lastIndexOf('.')).equals(".xml")){
                    XmlDiffUtils xmlDiffUtils = new XmlDiffUtils(topItem.fileNode);
                    if(xmlDiffUtils.isMapper()){
                        String topItemPath = topItem.relativePath.append(URL_SPLIT).append(fileName).toString();
                        result.put(xmlDiffUtils.getMapperNameSpace(), path + topItemPath);
                    }
                }
            }else{
                String dirName = topItem.fileNode.getName();
                String topItemPath = topItem.relativePath.append(URL_SPLIT).append(dirName).toString();
                File[] fs = topItem.fileNode.listFiles();
                for(File f:fs)
                    stack.push(new FileNode(new StringBuilder(topItemPath), f));
            }
        }
        return result;
    }

    private static List<String> listModules(File pom) throws ParserConfigurationException, IOException, SAXException {
        List<String> result = new ArrayList<>();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document document;
        DocumentBuilder db = dbf.newDocumentBuilder();
        document = db.parse(pom);
        NodeList modules = document.getElementsByTagName("module");
        if(modules.getLength() == 0){
            return result;
        }else{
            for(int i=0; i<modules.getLength(); i++){
                result.add(modules.item(i).getFirstChild().getNodeValue());
            }
        }

        return result;
    }

    private static byte[] getFileBytes(File file){
        try{
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] b = md.digest(FileUtils.readFileToByteArray(file));
            return b;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class FileNode{
        StringBuilder relativePath;
        File fileNode;
        public FileNode(StringBuilder relativePath, File fileNode){
            this.relativePath = relativePath;
            this.fileNode = fileNode;
        }
    }
}
