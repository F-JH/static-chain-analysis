package com.diff.core.Visitors;

import com.diff.core.Recorders.AbstractRecord;
import com.diff.core.Recorders.ControllerRecord;
import com.diff.core.Recorders.InterfaceRecord;
import com.diff.core.Utils.FilterUtils;
import com.diff.core.Utils.FullMethodNameUtil;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public class AsmClassVisitor extends ClassVisitor {
    private String className;

    private Map<String, List<String>> methodRelations = new HashMap<>();
    private InterfaceRecord interfaceRecord = InterfaceRecord.getInstance();
    private AbstractRecord abstractRecord = AbstractRecord.getInstance();
    private boolean isController = false;
    private boolean hasRequestMapping = false;
    private boolean isInterface;
    // 记录自己的 requestMapping::Value
    private Set<String> requestMappingValue;
    // 记录全部子方法的api入口
    private Map<String, Set<String>> recordMapping = new HashMap<>();

    public AsmClassVisitor(ClassVisitor cv){
        super(ASM7, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces){
//        if(name.contains("testInterface") || name.contains("testAbstract"))
//            System.out.println("haha");
        className = name;
        // 如果是接口的实体类，则加入interfaceRecord
        if(interfaces.length > 0){
            for(String interfaceClassName:interfaces) {
                if (FilterUtils.isNeedInject(interfaceClassName)) {
                    interfaceRecord.putInterfaceEntry(interfaceClassName, name);
                }
            }
        }
        // 如果是abstract类的实体类...
        if(abstractRecord.containAbstract(superName)){
            abstractRecord.putAbstractEntry(superName, name);
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions){
        List<String> methodRelation = new ArrayList<>();
        // 处理一下方法名，适配javaparser
        String signa = FullMethodNameUtil.getMethodSignatureName(name, descriptor);
        methodRelations.put(signa, methodRelation);
        if(hasRequestMapping){
            return new AsmMethodAdapter(access, name, descriptor, super.visitMethod(access,name,descriptor,signature,exceptions), methodRelation, className, requestMappingValue, recordMapping);
        }else{
            // 类没有 RequestMapping 则parentPath为空的Set
            return new AsmMethodAdapter(access, name, descriptor, super.visitMethod(access,name,descriptor,signature,exceptions), methodRelation, className, new HashSet<>(), recordMapping);
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visiable){
        if(FilterUtils.isControllerAnnotation(descriptor)){
            // controller类
            ControllerRecord.getInstance().putControlClass(className);
            isController = true;
        }
        if(FilterUtils.isRequestAnnotation(descriptor)){
            hasRequestMapping = true;
            requestMappingValue = new HashSet<>();
            Set<String> parentPath = new HashSet<>();
            // 这里去获取类的 requestMappingValue
            return new AsmAnnotationVisitor(super.visitAnnotation(descriptor, visiable), requestMappingValue, parentPath);
        }
        return super.visitAnnotation(descriptor, visiable);
    }

    @Override
    public void visitEnd(){
        super.visitEnd();
    }

    public Map<String, List<String>> getMethodRelations() {
        return methodRelations;
    }

    public Map<String, Set<String>> getRecordMapping() {
        return recordMapping;
    }
}
