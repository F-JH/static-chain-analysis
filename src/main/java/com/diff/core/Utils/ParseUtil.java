package com.diff.core.Utils;

import com.diff.core.Common.Config;
import com.diff.core.Visitors.MethodVisitorAdapter;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.printer.YamlPrinter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.diff.core.Common.Code.*;

public class ParseUtil {

    /**
     * test
     * @param args
     * @throws FileNotFoundException
     */
    public static void main(String[] args) throws FileNotFoundException {
        CompilationUnit cu1 = StaticJavaParser.parse(new File(Config.getInstance().getString("oldProjectPath") + "/src/main/java/com/bot/server/qqBot/server/msgManage.java"));
        YamlPrinter printer = new YamlPrinter(true);
        System.out.println(printer.output(cu1));
    }

    /**
     * 对比两个方法之间是否存在差异，弃用
     * @param oldFile
     * @param newFile
     * @return
     * @throws FileNotFoundException
     */
    public static Map<String, Boolean> compireToMethod(File oldFile, File newFile) throws FileNotFoundException {
        Config config = Config.getInstance();
        CompilationUnit oldCompilationUnit = StaticJavaParser.parse(oldFile);
        CompilationUnit newCompilationUnit = StaticJavaParser.parse(newFile);

        MethodVisitorAdapter oldMethodVisitor = new MethodVisitorAdapter();
        MethodVisitorAdapter newMethodVisitor = new MethodVisitorAdapter();
        // 清除注释
        oldCompilationUnit.getAllContainedComments().forEach(Node::remove);
        newCompilationUnit.getAllContainedComments().forEach(Node::remove);

        String oldArg = oldFile.getAbsolutePath().substring(
                (config.getString("oldProjectPath") + config.getString("source")).length() + 2,
                oldFile.getAbsolutePath().lastIndexOf(URL_SPLIT) + 1
        );
        String newArg = newFile.getAbsolutePath().substring(
                (config.getString("newProjectPath") + config.getString("source")).length() + 2,
                newFile.getAbsolutePath().lastIndexOf(URL_SPLIT) + 1
        );
        if(!URL_SPLIT.equals(PACKAGE_SPLIT)){
            // 兼容windows的路径，arg需要输入包名
            oldArg = oldArg.replace(URL_SPLIT, PACKAGE_SPLIT);
            newArg = newArg.replace(URL_SPLIT, PACKAGE_SPLIT);
        }

        oldMethodVisitor.visit(oldCompilationUnit, oldArg);
        newMethodVisitor.visit(newCompilationUnit, newArg);

        Map<String, Boolean> compireResult = new HashMap<>();
        for(String name:newMethodVisitor.getMds().keySet()){
            MethodDeclaration methodDeclaration = oldMethodVisitor.getMds().get(name);
            if(methodDeclaration != null)
                compireResult.put(name, methodDeclaration.equals(newMethodVisitor.getMds().get(name)));
            else
                compireResult.put(name, Boolean.FALSE);
        }

        return compireResult;
    }

    /**
     * 对比两个方法之间是否存在差异
     * @param oldFile
     * @param newFile
     * @return
     * @throws FileNotFoundException
     */
    public static Map<String, Boolean> compireToMethod(File oldFile, File newFile, String moduleName) throws FileNotFoundException {
        Config config = Config.getInstance();
        CompilationUnit oldCompilationUnit = StaticJavaParser.parse(oldFile);
        CompilationUnit newCompilationUnit = StaticJavaParser.parse(newFile);

        MethodVisitorAdapter oldMethodVisitor = new MethodVisitorAdapter();
        MethodVisitorAdapter newMethodVisitor = new MethodVisitorAdapter();
        // 清除注释
        oldCompilationUnit.getAllContainedComments().forEach(Node::remove);
        newCompilationUnit.getAllContainedComments().forEach(Node::remove);

        int offset = moduleName.equals("") ? 2:3;
        String oldArg = oldFile.getAbsolutePath().substring(
                (config.getString("oldProjectPath") + moduleName + config.getString("source")).length() + offset,
                oldFile.getAbsolutePath().lastIndexOf(URL_SPLIT) + 1
        );
        String newArg = newFile.getAbsolutePath().substring(
                (config.getString("newProjectPath") + moduleName + config.getString("source")).length() + offset,
                newFile.getAbsolutePath().lastIndexOf(URL_SPLIT) + 1
        );
        if(!URL_SPLIT.equals(PACKAGE_SPLIT)){
            // 兼容windows的路径，arg需要输入包名
            oldArg = oldArg.replace(URL_SPLIT, PACKAGE_SPLIT);
            newArg = newArg.replace(URL_SPLIT, PACKAGE_SPLIT);
        }

        oldMethodVisitor.visit(oldCompilationUnit, oldArg);
        newMethodVisitor.visit(newCompilationUnit, newArg);

        Map<String, Boolean> compireResult = new HashMap<>();
        for(String name:newMethodVisitor.getMds().keySet()){
            MethodDeclaration methodDeclaration = oldMethodVisitor.getMds().get(name);
            if(methodDeclaration != null)
                compireResult.put(name, methodDeclaration.equals(newMethodVisitor.getMds().get(name)));
            else
                compireResult.put(name, Boolean.FALSE);
        }

        return compireResult;
    }

    /**
     * 用于检索新项目中的java方法
     * @param classFilePath
     * @return
     * @throws FileNotFoundException
     */
    public static List<String> scanMethods(String classFilePath)throws FileNotFoundException{
        File file = new File(classFilePath);
        Config config = Config.getInstance();
        CompilationUnit compilationUnit = StaticJavaParser.parse(file);
        MethodVisitorAdapter methodVisitor = new MethodVisitorAdapter();
        String arg = classFilePath.substring(
                classFilePath.indexOf(config.getString("source")) + config.getString("source").length() + 1,
                classFilePath.lastIndexOf(URL_SPLIT) + 1
        );
        if(!URL_SPLIT.equals(PACKAGE_SPLIT))
            arg = arg.replace(URL_SPLIT, PACKAGE_SPLIT);
        methodVisitor.visit(compilationUnit, arg);
        return new ArrayList<>(methodVisitor.getMds().keySet());
    }
}