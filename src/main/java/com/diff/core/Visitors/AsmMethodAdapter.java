package com.diff.core.Visitors;

import com.diff.core.Recorders.ApiRecord;
import com.diff.core.Recorders.ControllerRecord;
import com.diff.core.Utils.FilterUtils;
import com.diff.core.Utils.FullMethodNameUtil;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.*;

import static com.diff.core.Common.Code.*;

public class AsmMethodAdapter extends AdviceAdapter {
    private String className;
    private String methodName;
    private String desc;
    private List<String> relationShip;
    private Set<String> parentPath;
    private Set<String> requestMappingValue;
    private Map<String, Set<String>> recordMapping;

    public AsmMethodAdapter(int access, String methodName, String desc, MethodVisitor mv, List<String> relationShip, String className){
        super(ASM7, mv, access, methodName, desc);
        this.methodName = methodName;
        this.relationShip = relationShip;
        this.desc = desc;
        this.className = className;
    }

    public AsmMethodAdapter(int access, String methodName, String desc, MethodVisitor mv, List<String> relationShip, String className, Set<String> parentPath, Map<String, Set<String>> recordMapping){
        super(ASM7, mv, access, methodName, desc);
        this.methodName = methodName;
        this.relationShip = relationShip;
        this.desc = desc;
        this.className = className;
        this.parentPath = parentPath;
        this.recordMapping = recordMapping;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descript, boolean isInterface){
        String fullMethodName = owner + METHOD_SPLIT + FullMethodNameUtil.getMethodSignatureName(name, descript);
        if(FilterUtils.isNeedInject(owner) && FilterUtils.isNeedInjectMethod(fullMethodName) && !relationShip.contains(fullMethodName))
            relationShip.add(fullMethodName);
        super.visitMethodInsn(opcode, owner, name, descript, isInterface);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visiable){
        if(FilterUtils.isRequestAnnotation(descriptor)){
            // request的方法
            ControllerRecord.getInstance().putControlMethod(className, FullMethodNameUtil.getMethodSignatureName(methodName, desc));
            requestMappingValue = new HashSet<>();
            // 这里去获取方法的 requestMappingValue
            // 故而：requestMappingValue记录自己的，parentPath上一级(比如类)传过来的 requestMappingValue
            return new AsmAnnotationVisitor(super.visitAnnotation(descriptor, visiable), requestMappingValue, parentPath);
        }
        return super.visitAnnotation(descriptor, visiable);
    }

    @Override
    public void visitTypeInsn(int opcode, String type){
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitMaxs(int a, int b){
        try{
            super.visitMaxs(a, b);
        }catch (TypeNotPresentException e){
            return;
        }
    }

    @Override
    public void visitEnd(){
        // 处理一下Mapping
        if(requestMappingValue!=null){
            String fullMethodName = className + METHOD_SPLIT + FullMethodNameUtil.getMethodSignatureName(methodName, desc);
            recordMapping.put(fullMethodName, requestMappingValue);
            ApiRecord.getInstance().putApi(fullMethodName, requestMappingValue);
        }

        super.visitEnd();
    }
}
