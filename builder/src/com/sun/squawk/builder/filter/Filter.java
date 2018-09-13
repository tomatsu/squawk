package com.sun.squawk.builder.filter;

import java.util.zip.*;
import java.util.*;
import java.io.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.*;
import org.objectweb.asm.util.*;
import org.objectweb.asm.tree.*;

public class Filter implements Opcodes {

    class MyVisitor extends ClassVisitor {
	String name;

	MyVisitor(ClassVisitor visitor) {
	    super(Opcodes.ASM5, visitor);
	}
	
	public void visit(int version, int access, String name, String signature,
			  String superName, String[] interfaces) {
	    if (excludedMembers.contains(name)) {
		if (verbose) System.out.println("deleting " + name);
		return;
	    } else if (packagePrivateMembers.contains(name)) {
		access &= ~(ACC_PUBLIC|ACC_PRIVATE);
	    } else if (privateMembers.contains(name)) {
		access &= ~ACC_PUBLIC;
		access |= ACC_PRIVATE;
	    }
	    this.name = name;

	    if (excludedMembers.contains(superName)) {
		superName = "java/lang/Object";
	    }
	    if (interfaces != null && interfaces.length > 0) {
		List<String> itf = new ArrayList<>();
		for (int i = 0; i < interfaces.length; i++) {
		    if (!excludedMembers.contains(interfaces[i])) {
			itf.add(interfaces[i]);
		    }
		}
		interfaces = itf.toArray(new String[itf.size()]);
	    }
	    if (strip) {
		signature = null;
		access &= ~ACC_ENUM;
	    }
	    super.visit(version, access, name, signature, superName, interfaces);
	}

	public void visitInnerClass(String name, String outerName,
				    String innerName, int access)
	{
	    if (excludedMembers.contains(name)) {
		if (verbose) System.out.println("deleting " + name);
		return;
	    } else if (packagePrivateMembers.contains(name)) {
		access &= ~(ACC_PUBLIC|ACC_PRIVATE);
	    } else if (privateMembers.contains(name)) {
		access &= ~ACC_PUBLIC;
		access |= ACC_PRIVATE;
	    }
	    if (strip) {
		access &= ~ACC_ENUM;
	    }
	    super.visitInnerClass(name, outerName, innerName, access);
	}
	    
	public MethodVisitor visitMethod(int access, String name, String desc,
					 String signature, String[] exceptions)
	{

	    String key = this.name + "." + name + desc;
	    if (excludedMembers.contains(key)) {
		if (verbose) System.out.println("deleting " + key);
		return null;
	    }
	    access = convertAccess(access, key);
	    
	    if (strip) {
		signature = null; // discard signature
		final MethodVisitor mv = super.visitMethod(access, name, desc, null, exceptions);
		return new MethodVisitor(Opcodes.ASM5, mv) {
		    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			if (verbose) System.out.println("discarding annotation " + desc + ", " + visible);
			return null;
		    }
		    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			if (verbose) System.out.println("discarding type annotation " + typeRef + ", " + typePath + ", " + desc + ", " + visible);
			return null;
		    }
		    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
			if (verbose) System.out.println("discarding parameter annotation " + parameter + ", " + desc + ", " + visible);
			return null;
		    }
		    public void visitAttribute(Attribute attr) {
			if (verbose) System.out.println("discarding non standard attribute " + attr.type);
		    }
		    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
			mv.visitLocalVariable(name, desc, null, start, end, index);
		    }
		};
	    } else {
		return super.visitMethod(access, name, desc, signature, exceptions);
	    }
	}
	
	public FieldVisitor visitField(int access, String name, String desc,
				       String signature, Object value)
	{
	    String key = this.name + "." + name;
	    if (excludedMembers.contains(key)) {
		if (verbose) System.out.println("deleting " + key);
		return null;
	    }
	    access = convertAccess(access, key);

	    if (strip) {
		signature = null; // discard signature
		FieldVisitor fv = super.visitField(access, name, desc, signature, value);
		return new FieldVisitor(Opcodes.ASM5, fv) {
		    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			if (verbose) System.out.println("discarding annotation " + desc + ", " + visible);
			return null;
		    }
		    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			if (verbose) System.out.println("discarding type annotation " + typeRef + ", " + typePath + ", " + desc + ", " + visible);
			return null;
		    }
		    public void visitAttribute(Attribute attr) {
			if (verbose) System.out.println("discarding non standard attribute " + attr.type);
		    }
		};
	    } else {
		return super.visitField(access, name, desc, signature, value);
	    }
	}
	
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
	    if (!strip) {
		return super.visitAnnotation(desc, visible);
	    } else {
		return null;
	    }
	}
	
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
	    if (!strip) {
		return super.visitTypeAnnotation(typeRef, typePath, desc, visible);
	    } else {
		return null;
	    }
	}
	
	public void visitAttribute(Attribute attr) {
	    if (!strip) {
		super.visitAttribute(attr);
	    }
	}
    }

    int convertAccess(int access, String key) {
	if (packagePrivateMembers.contains(key)) {
	    return access & ~(ACC_PUBLIC|ACC_PROTECTED|ACC_PRIVATE);
	} else if (privateMembers.contains(key)) {
	    return (access & ~(ACC_PUBLIC|ACC_PROTECTED)) | ACC_PRIVATE;
	} else if (protectedMembers.contains(key)) {
	    return (access & ~(ACC_PUBLIC|ACC_PRIVATE)) | ACC_PROTECTED;
	} else if (publicMembers.contains(key)) {
	    return (access & ~(ACC_PRIVATE|ACC_PROTECTED)) | ACC_PUBLIC;
	} else {
	    return access;
	}
    }

    Set<String> publicMembers = new HashSet<String>();
    Set<String> protectedMembers = new HashSet<String>();
    Set<String> packagePrivateMembers = new HashSet<String>();
    Set<String> privateMembers = new HashSet<String>();
    Set<String> excludedMembers = new HashSet<String>();
    boolean strip;
    boolean verbose;
    
    public Filter(String config) throws IOException {
	parse(config);
    }
    
    void parse(String config) throws IOException {
	parse(new BufferedReader(new FileReader(config)));
    }

    void parse(BufferedReader reader) throws IOException {
	String line;
	while ((line = reader.readLine()) != null) {
	    parseLine(line);
	}
    }

    void parseLine(String line) {
	if (line.startsWith("#")) return;
	if (line.length() < 1) return;
	    
	if (line.startsWith("package ")) { // package-private
	    packagePrivateMembers.add(line.substring(8));
	} else if (line.startsWith("private ")) { // private
	    privateMembers.add(line.substring(8));
	} else if (line.startsWith("protected ")) { // protected
	    protectedMembers.add(line.substring(10));
	} else if (line.startsWith("public ")) { // public
	    publicMembers.add(line.substring(7));
	} else { // delete
	    excludedMembers.add(line);
	}
    }

    void fixExceptionTable(ClassNode classNode) {
	List<MethodNode> m = classNode.methods;
        for (MethodNode methodNode : m) {
	    List<TryCatchBlockNode> list = methodNode.tryCatchBlocks;
	    for (Iterator<TryCatchBlockNode> it = list.iterator(); it.hasNext(); ) {
		TryCatchBlockNode n = it.next();
		int start = n.start.getLabel().getOffset();
		int end = n.end.getLabel().getOffset();
		if (start >= end) {
		    it.remove();
		}
	    }
	}
    }

    void processClassFile(InputStream in, OutputStream out) throws IOException {
	ClassReader cr = new ClassReader(in);
	ClassWriter cw = new ClassWriter(0);
	ClassVisitor cv = new MyVisitor(cw);
	
	cr.accept(cv, ClassReader.SKIP_DEBUG);
	byte[] code = cw.toByteArray();

	/* process class nodes */
	ByteArrayInputStream bin = new ByteArrayInputStream(code);
	cr = new ClassReader(bin);
        ClassNode classNode = new ClassNode();
	cr.accept(classNode, 0);
	FieldFilter ff = new FieldFilter() {
		protected boolean excludes(String name) {
		    return excludedMembers.contains(name);
		}
	    };
	ff.verbose = verbose;
	ff.transformClassNode(classNode);
	cw = new ClassWriter(0);
        classNode.accept(cw);
	//        classNode.accept(new TraceClassVisitor(cw = new ClassWriter(0), new PrintWriter(System.out)));

	fixExceptionTable(classNode);
	cw = new ClassWriter(0);
        classNode.accept(cw);
	
	code = cw.toByteArray();
	
	out.write(code);
    }
	
    void copy(InputStream in, OutputStream out) throws IOException {
	byte[] buf = new byte[8192];
	int n;
	while ((n = in.read(buf)) != -1) {
	    out.write(buf, 0, n);
	}
    }

    void doit(String input, String output) throws IOException {
	ZipFile zfile = new ZipFile(input);
	ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(output));
	for (Enumeration e = zfile.entries(); e.hasMoreElements(); ){
	    ZipEntry entry = (ZipEntry)e.nextElement();
	    String entryName = entry.getName();
	    InputStream in = zfile.getInputStream(entry);
	    if (!entryName.endsWith(".class")) {
		zout.putNextEntry(new ZipEntry(entryName));
		copy(in, zout);
	    } else {
		if (!excludedMembers.contains(entryName.substring(0, entryName.length() - 6))) {
		    zout.putNextEntry(new ZipEntry(entryName));
		    processClassFile(in, zout);
		}
	    }
	}
	zout.finish();
	zout.close();
    }

    static void usage() {
	System.err.println("Usage: java Filter [-strip] config injar outjar");
	System.exit(0);
    }
    
    public static void main(String[] args) throws IOException {
	int len = args.length;
	if (len < 2) {
	    usage();
	}
	boolean strip = false;
	boolean verbose = false;
	int i = 0;
	while (i < len) {
	    if (args[i].equals("-strip")) { // discard non-standard attributes and signatures
		strip = true;
	    } else if (args[i].equals("-v")) {
		verbose = true;
	    } else {
		break;
	    }
	    i++;
	}
	String[] newArgs = new String[len - i];
	System.arraycopy(args, i, newArgs, 0, len - i);
	args = newArgs;
	
	String input = args[1];
	String output = args[2];
	
	Filter filter = new Filter(args[0]);
	filter.strip = strip;
	filter.verbose = verbose;
	filter.doit(input, output);
    }
}
