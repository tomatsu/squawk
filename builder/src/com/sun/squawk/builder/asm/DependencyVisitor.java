package com.sun.squawk.builder.asm;

import java.util.*;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

/**
 * DependencyVisitor
 * 
 * class -* method -* method
 *                 -* field
 *       -* field
 */

public class DependencyVisitor extends ClassVisitor {

	Map<String,ClassNode> classMap = new HashMap<String,ClassNode>();
	Map<String,MethodNode> methodMap = new HashMap<String,MethodNode>();
	Map<String,FieldNode> fieldMap = new HashMap<String,FieldNode>();
	ClassNode currentClass;
	MethodNode currentMethod;
	
    public DependencyVisitor() {
        super(Opcodes.ASM5);
    }

	ClassNode getClassNode(String name) {
		ClassNode c = classMap.get(name);
		if (c == null) {
			c = new ClassNode(name);
			classMap.put(name, c);
		}
		return c;
	}
	
	MethodNode getMethodNode(String owner, String name, String desc) {
		String id = owner + "." + name + desc;
		MethodNode n = methodMap.get(id);
		if (n == null) {
			n = new MethodNode(getClassNode(owner), name, desc);
			methodMap.put(id, n);
		}
		return n;
	}
	
	FieldNode getFieldNode(String owner, String name, String desc) {
		String id = owner + "." + name;
		FieldNode n = fieldMap.get(id);
		if (n == null) {
			n = new FieldNode(getClassNode(owner), name, desc);
			fieldMap.put(id, n);
		}
		return n;
	}
	
    // ClassVisitor

    @Override
    public void visit(final int version, final int access, final String name,
            final String signature, final String superName,
            final String[] interfaces) {

		currentClass = getClassNode(name);
		Set<ClassNode> superTypes = new HashSet<ClassNode>();
		if (superName != null) {
			ClassNode sp = getClassNode(superName);
			superTypes.add(sp);
			sp.subTypes.add(currentClass);
		}
		for (int i = 0; i < interfaces.length; i++) {
			ClassNode itf = getClassNode(interfaces[i]);
			superTypes.add(itf);
			itf.subTypes.add(currentClass);
		}
		currentClass.setSuperTypes(superTypes);
		currentClass.access = access;

		/*
        if (signature == null) {
            if (superName != null) {
                addInternalName(superName);
            }
            addInternalNames(interfaces);
        } else {
            addSignature(signature);
        }
		*/
    }

//    @Override
    public AnnotationVisitor visitAnnotation(final String desc,
            final boolean visible) {
//        addDesc(desc);
        return new AnnotationDependencyVisitor();
    }

//    @Override
    public AnnotationVisitor visitTypeAnnotation(final int typeRef,
            final TypePath typePath, final String desc, final boolean visible) {
//        addDesc(desc);
        return new AnnotationDependencyVisitor();
    }

//    @Override
    public FieldVisitor visitField(final int access, final String name,
            final String desc, final String signature, final Object value) {

		FieldNode f = getFieldNode(currentClass.getName(), name, desc);
		currentClass.addField(f);
		/*		
        if (signature == null) {
            addDesc(desc);
        } else {
            addTypeSignature(signature);
        }
        if (value instanceof Type) {
            addType((Type) value);
        }
		*/
        return new FieldDependencyVisitor();
    }

//    @Override
    public MethodVisitor visitMethod(final int access, final String name,
            final String desc, final String signature, final String[] exceptions) {

		MethodNode m = getMethodNode(currentClass.getName(), name, desc);
		m.access = access;
		m.exceptions = exceptions;
		currentClass.addMethodRef(m);
		methodMap.put(currentClass.getName() + "." + name + desc, m);
		currentMethod = m;

		/*
        if (signature == null) {
            addMethodDesc(desc);
        } else {
            addSignature(signature);
        }
        addInternalNames(exceptions);
		*/
        return new MethodDependencyVisitor();
    }

    class AnnotationDependencyVisitor extends AnnotationVisitor {

        public AnnotationDependencyVisitor() {
            super(Opcodes.ASM5);
        }

//        @Override
        public void visit(final String name, final Object value) {
			/*
            if (value instanceof Type) {
                addType((Type) value);
            }
			*/
        }

//        @Override
        public void visitEnum(final String name, final String desc,
                final String value) {
//            addDesc(desc);
        }

//        @Override
        public AnnotationVisitor visitAnnotation(final String name,
                final String desc) {
//            addDesc(desc);
            return this;
        }

//        @Override
        public AnnotationVisitor visitArray(final String name) {
            return this;
        }
    }

    class FieldDependencyVisitor extends FieldVisitor {

        public FieldDependencyVisitor() {
            super(Opcodes.ASM5);
        }

//        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
//            addDesc(desc);
            return new AnnotationDependencyVisitor();
        }

//        @Override
        public AnnotationVisitor visitTypeAnnotation(final int typeRef,
                final TypePath typePath, final String desc,
                final boolean visible) {
//            addDesc(desc);
            return new AnnotationDependencyVisitor();
        }
    }

    class MethodDependencyVisitor extends MethodVisitor {

        public MethodDependencyVisitor() {
            super(Opcodes.ASM5);
        }

//        @Override
        public AnnotationVisitor visitAnnotationDefault() {
            return new AnnotationDependencyVisitor();
        }

//        @Override
        public AnnotationVisitor visitAnnotation(final String desc,
                final boolean visible) {
//            addDesc(desc);
            return new AnnotationDependencyVisitor();
        }

//        @Override
        public AnnotationVisitor visitTypeAnnotation(final int typeRef,
                final TypePath typePath, final String desc,
                final boolean visible) {
//            addDesc(desc);
            return new AnnotationDependencyVisitor();
        }

//        @Override
        public AnnotationVisitor visitParameterAnnotation(final int parameter,
                final String desc, final boolean visible) {
//            addDesc(desc);
            return new AnnotationDependencyVisitor();
        }

//        @Override
        public void visitTypeInsn(final int opcode, final String type) {
//            addType(Type.getObjectType(type));
        }

//        @Override
        public void visitFieldInsn(final int opcode, final String owner,
                final String name, final String desc) {
			FieldNode f = getFieldNode(owner, name, desc);
			currentMethod.addFieldRef(f);
			/*			
            addInternalName(owner);
            addDesc(desc);
			*/
        }

//        @Override
        public void visitMethodInsn(final int opcode, final String owner,
                final String name, final String desc, final boolean itf) {

			
			MethodNode n = getMethodNode(owner, name, desc);
			currentMethod.addMethodRef(n);
			/*
            addInternalName(owner);
            addMethodDesc(desc);
			*/
        }

//        @Override
        public void visitInvokeDynamicInsn(String name, String desc,
                Handle bsm, Object... bsmArgs) {
			/*
            addMethodDesc(desc);
            addConstant(bsm);
            for (int i = 0; i < bsmArgs.length; i++) {
                addConstant(bsmArgs[i]);
            }
			*/
        }

//        @Override
        public void visitLdcInsn(final Object cst) {
//            addConstant(cst);
			if (cst instanceof Type) {
				Type t = (Type)cst;
				if (t.getSort() == Type.OBJECT) {
					currentMethod.addClassRef(getClassNode(t.getClassName().replace('.', '/')));
				}
			}
        }

//        @Override
        public void visitMultiANewArrayInsn(final String desc, final int dims) {
//            addDesc(desc);
        }

//        @Override
        public AnnotationVisitor visitInsnAnnotation(int typeRef,
                TypePath typePath, String desc, boolean visible) {
//            addDesc(desc);
            return new AnnotationDependencyVisitor();
        }

//        @Override
        public void visitLocalVariable(final String name, final String desc,
                final String signature, final Label start, final Label end,
                final int index) {
//            addTypeSignature(signature);
        }

//        @Override
        public AnnotationVisitor visitLocalVariableAnnotation(int typeRef,
                TypePath typePath, Label[] start, Label[] end, int[] index,
                String desc, boolean visible) {
//            addDesc(desc);
            return new AnnotationDependencyVisitor();
        }

//        @Override
        public void visitTryCatchBlock(final Label start, final Label end,
                final Label handler, final String type) {
			/*
            if (type != null) {
                addInternalName(type);
            }
			*/
        }

//        @Override
        public AnnotationVisitor visitTryCatchAnnotation(int typeRef,
                TypePath typePath, String desc, boolean visible) {
//            addDesc(desc);
            return new AnnotationDependencyVisitor();
        }
    }

    class SignatureDependencyVisitor extends SignatureVisitor {

        String signatureClassName;

        public SignatureDependencyVisitor() {
            super(Opcodes.ASM5);
        }

//        @Override
        public void visitClassType(final String name) {
			/*
            signatureClassName = name;
            addInternalName(name);
			*/
        }

//        @Override
        public void visitInnerClassType(final String name) {
			/*
            signatureClassName = signatureClassName + "$" + name;
            addInternalName(signatureClassName);
			*/
        }
    }

    // ---------------------------------------------
	
	void dump() {
		for (Map.Entry<String,ClassNode> e: classMap.entrySet()) {
			ClassNode n = e.getValue();
			System.out.println(n.getName());
			for (MethodNode m: n.methods) {
				System.out.println(m);
				for (FieldNode field : m.fieldRef) {
					System.out.print("\t");
					System.out.println(field);
				}
				for (MethodNode m2 : m.methodRef) {
					System.out.print("\t");
					System.out.println(m2);
				}
			}
			System.out.println("---------------------");
		}
	}
}
