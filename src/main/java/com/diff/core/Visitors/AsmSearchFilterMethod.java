package com.diff.core.Visitors;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;

public class AsmSearchFilterMethod extends AdviceAdapter {
    public AsmSearchFilterMethod(MethodVisitor mv, int access, String methodName, String desc){
        super(ASM7, mv, access, methodName, desc);
    }

    @Override
    public void visitMaxs(int a, int b){
        // 这里不关心Type的报错，直接忽略
        try{
            super.visitMaxs(a, b);
        }catch (TypeNotPresentException ignored){
        }
    }
}