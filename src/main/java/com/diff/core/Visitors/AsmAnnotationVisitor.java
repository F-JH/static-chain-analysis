package com.diff.core.Visitors;

import org.objectweb.asm.AnnotationVisitor;

import java.util.Set;

import static org.objectweb.asm.Opcodes.ASM7;
import static com.diff.core.Common.Code.*;

public class AsmAnnotationVisitor extends AnnotationVisitor {
    // 记录自己的requestMappingValue
    private Set<String> paths;
    // 上一级的requestMappingValue(一般是类的RequestMapping)
    private Set<String> parentPaths;
    // 判断是否有value
    private boolean hasValue = false;

    public AsmAnnotationVisitor(AnnotationVisitor av, Set<String> paths, Set<String> parentPaths){
        super(ASM7, av);
        this.paths = paths;
        this.parentPaths = parentPaths;
    }

    @Override
    public AnnotationVisitor visitArray(String name){
        // 主要提供给 annotationVisitor0 访问
        if(name.equals("value")){
            hasValue = true;
            return new AsmAnnotationVisitor(super.visitArray(name), paths, parentPaths);
        }
        return super.visitArray(name);
    }

    @Override
    public void visit(String name, Object value){
        // 主要提供给 annotationVisitor1 访问
        if(parentPaths.size() > 0){
            for(String parentPath:parentPaths)
                paths.add(parentPath + (String) value);
        }else{
            paths.add((String) value);
        }
        super.visit(name, value);
    }

    @Override
    public void visitEnd(){
        if(!hasValue && paths.size() == 0){
            paths.addAll(parentPaths);
        }
        super.visitEnd();
    }
}
