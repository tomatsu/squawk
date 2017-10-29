package com.sun.squawk.builder.dca;

import java.util.zip.*;
import java.util.*;
import java.io.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.*;

public class DeadCodeAnalyzer implements Opcodes {

    String config;
    String outputFile;
    String used;
    String xref;
    String itf;
    boolean onlyRequired;
    boolean debug;
    boolean eliminateWriteOnlyFields;
    
    void processClassFile(Env env, InputStream in) throws IOException {
	ClassReader cr = new ClassReader(in);
	ClassVisitor cv = new MyVisitor(env);

	cr.accept(cv, ClassReader.SKIP_DEBUG);
    }

    void processClassFiles(Env env, String[] input) throws IOException {
	for (int i = 0; i < input.length; i++) {
	    ZipFile zfile = new ZipFile(input[i]);
	    for (Enumeration e = zfile.entries(); e.hasMoreElements(); ){
		ZipEntry entry = (ZipEntry)e.nextElement();
		String entryName = entry.getName();
		String name = entry.getName();
		InputStream in = zfile.getInputStream(entry);
		if (name.endsWith(".class")) {
		    processClassFile(env, in);
		}
	    }
	}
    }
	
    void doit(String[] input) throws IOException {
	Env env = new Env(this);
	processClassFiles(env, input);
	env.analyze();
    }
	
    public static void main(String[] args) throws IOException {
	int len = args.length;
	if (len < 2) {
	    System.err.println("Usage: java DeadCodeAnalyzer -config <configFile1>,... -o <outputFile> jarFile1 ...");
	    System.exit(0);
	}
	String config = null;
	String output = null;
	String xref = null;
	String used = null;
	String itf = null;
	boolean debug = false;
	boolean onlyRequired = true;
	boolean eliminateWriteOnlyFields = false;
	int i = 0;
	while (i < len) {
	    if ("-config".equals(args[i])) {
		if (++i < len) {
		    config = args[i];
		}
	    } else if ("-o".equals(args[i])) {
		if (++i < len) {
		    output = args[i];
		}
	    } else if ("-x".equals(args[i])) {
		if (++i < len) {
		    xref = args[i];
		    onlyRequired = true;
		}
	    } else if ("-X".equals(args[i])) {
		if (++i < len) {
		    xref = args[i];
		    onlyRequired = false;
		}
	    } else if ("-u".equals(args[i])) {
		if (++i < len) {
		    used = args[i];
		}
	    } else if ("-i".equals(args[i])) {
		if (++i < len) {
		    itf = args[i];
		}
	    } else if ("-d".equals(args[i])) {
		debug = true;
	    } else if ("-w".equals(args[i])) {
		eliminateWriteOnlyFields = true;
	    } else {
		break;
	    }
	    i++;
	}
	String[] newArgs = new String[len - i];
	System.arraycopy(args, i, newArgs, 0, newArgs.length);
	
	DeadCodeAnalyzer analyzer = new DeadCodeAnalyzer();
	analyzer.config = config;
	analyzer.used = used;
	analyzer.outputFile = output;
	analyzer.xref = xref;
	analyzer.itf = itf;
	analyzer.debug = debug;
	analyzer.onlyRequired = onlyRequired;
	analyzer.eliminateWriteOnlyFields = eliminateWriteOnlyFields;
	analyzer.doit(newArgs);
    }

    class MyVisitor extends ClassVisitor {
	String name;
	ClassNode classNode;
	Env env;

	MyVisitor(Env env) {
	    super(Opcodes.ASM5, null);
	    this.env = env;
	}
	
	public void visit(int version, int access, String name, String signature,
			  String superName, String[] interfaces) {
	    this.name = name;
	    super.visit(version, access, name, signature, superName, interfaces);
	    classNode = ClassNode.create(env, version, access, name, signature, superName, interfaces);
	}

	public void visitInnerClass(String name, String outerName,
				    String innerName, int access)
	{
	    super.visitInnerClass(name, outerName, innerName, access);
	}
	
	public void visitOuterClass(String owner, String name, String desc) {
	    if (name == null || desc == null) {
		classNode.enclosingClass = env.registerClass(owner);
	    } else {
		ClassNode enc = env.registerClass(owner);
		classNode.enclosingMethod = enc.registerMethod(name, desc);
	    }
	    super.visitOuterClass(owner, name, desc);
	}
	
	public MethodVisitor visitMethod(int access, String name, String desc,
					 String signature, String[] exceptions)
	{
	    final MethodNode methodNode = MethodNode.create(env, classNode, access, name, desc, signature, exceptions);
	    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
	    return new MethodVisitor(Opcodes.ASM5, mv) {
		public void visitMethodInsn(final int opcode, final String owner,
					    final String name, final String desc, final boolean itf) {
		    if (opcode == INVOKESPECIAL ||
			opcode == INVOKEVIRTUAL ||
			opcode == INVOKESTATIC ||
			opcode == INVOKEINTERFACE)
			{
			    methodNode.callMethod(env, owner, name, desc);
			}
		    super.visitMethodInsn(opcode, owner, name, desc, itf);
		}
		public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		    if (opcode == GETSTATIC || opcode == GETFIELD) {
			methodNode.accessField(env, owner, name, desc, opcode);
		    } else if (!eliminateWriteOnlyFields && (opcode == PUTSTATIC || opcode == PUTFIELD)) {
			methodNode.accessField(env, owner, name, desc, opcode);
		    }
		    super.visitFieldInsn(opcode, owner, name, desc);
		}
		public void visitTypeInsn(int opcode, String type) {
		    methodNode.accessType(env, type);
		    super.visitTypeInsn(opcode, type);
		}
		public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		    methodNode.accessType(env, type);
		    super.visitTryCatchBlock(start, end, handler, type);
		}
	    };
	}
	
	public FieldVisitor visitField(int access, String name, String desc,
				       String signature, Object value)
	{
	    FieldNode.create(env, classNode, access, name, desc, signature, value);
	    return super.visitField(access, name, desc, signature, value);
	}
    }
}
