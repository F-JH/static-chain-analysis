package com.diff.core.Visitors;

import com.diff.core.Recorders.AbstractRecord;
import com.diff.core.Recorders.DubboRecord;
import com.diff.core.Recorders.InterfaceRecord;
import com.diff.core.Utils.FilterUtils;
import com.diff.core.Utils.FullMethodNameUtil;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static com.diff.core.Common.Code.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * 第一次遍历所有class，记录下项目的所有className到FilterUtil中；
 * 记录下所有interface以及abstract类，以及它们与实体类的对应关系
 */
public class AsmSearchFilterClass extends ClassVisitor{
    private final InterfaceRecord interfaceRecord = InterfaceRecord.getInstance();
    private final AbstractRecord abstractRecord = AbstractRecord.getInstance();
    private String className;
    private boolean isInterface = false;
    private boolean isAbstract = false;
    private boolean isDubbo = false;

    public AsmSearchFilterClass(ClassVisitor cv){
        super(ASM7, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces){
        // 记录className
        FilterUtils.addProjectPackage(name);
        className = name;
        // 如果是interface
        if((access & ACC_INTERFACE)==ACC_INTERFACE){
            interfaceRecord.putInterfaceClass(name);
            isInterface = true;
        }
        // 如果是abstract类
        if((access & ACC_ABSTRACT)==ACC_ABSTRACT){
            abstractRecord.putAbstractClass(name);
            isAbstract = true;
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions){
        // 保存方法名
        String methodName = FullMethodNameUtil.getMethodSignatureName(name, descriptor);
        FilterUtils.addProjectMethod(className + METHOD_SPLIT + methodName);
        // 接口或抽象类的abstract方法需要记录
        if(isInterface){
            if((access & ACC_ABSTRACT)==ACC_ABSTRACT){
                interfaceRecord.putMethod(className, methodName, true);
            }else{
                interfaceRecord.putMethod(className, methodName, false);
            }
        }
        if(isAbstract){
            if((access & ACC_ABSTRACT)==ACC_ABSTRACT){
                abstractRecord.putMethod(className, methodName, true);
            }else{
                abstractRecord.putMethod(className, methodName, false);
            }
        }
        if(isDubbo){
            DubboRecord.putDubboMethod(className + METHOD_SPLIT + methodName);
        }
        return new AsmSearchFilterMethod(
                super.visitMethod(access, name, descriptor, signature, exceptions),
                access, name, descriptor
        );
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visiable){
        if(FilterUtils.isDubboAnnotation(descriptor)){
            isDubbo = true;
//            DubboRecord.putDubboClass(descriptor);
        }
        return super.visitAnnotation(descriptor, visiable);
    }
}
