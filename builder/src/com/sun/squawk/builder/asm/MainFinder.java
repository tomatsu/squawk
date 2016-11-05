package com.sun.squawk.builder.asm;

import org.objectweb.asm.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;

public class MainFinder {
	static class Finder extends ClassVisitor implements Opcodes {
		String className;
		List<String> result;
		
		Finder(List<String> result) {
			super(Opcodes.ASM5, null);
			this.result = result;
		}

		public void visit(int version, int access, String name, String signature,
						  String superName, String[] interfaces) {
			className = name;
		}
		
		public MethodVisitor visitMethod(int access, String name, String desc,
										 String signature, String[] exceptions) {
			if (name.equals("main") &&
				desc.equals("([Ljava/lang/String;)V") &&
				(access & ACC_PUBLIC) == ACC_PUBLIC)
			{
				result.add(className.replace('/', '.'));
			}
			return null;
		}
	}
	
	void processClassFile(InputStream in, List<String> result) throws IOException {
		ClassReader cr = new ClassReader(in);
		ClassVisitor cv = new Finder(result);
		cr.accept(cv, ClassReader.SKIP_DEBUG);
	}

	void doit(String input, List<String> result) throws IOException {
		ZipFile zfile = new ZipFile(input);
		for (Enumeration e = zfile.entries(); e.hasMoreElements(); ){
			ZipEntry entry = (ZipEntry)e.nextElement();
			String name = entry.getName();
			InputStream in = zfile.getInputStream(entry);
			if (name.endsWith(".class")) {
				processClassFile(in, result);
			}
		}
	}

	public static void main(String[] args) throws IOException {
		List<String> result = new ArrayList<String>();
		MainFinder finder = new MainFinder();
		for (int i = 0; i < args.length; i++) {
			finder.doit(args[i], result);
		}
		if (result.size() == 0) {
			throw new RuntimeException("no main class");
		}
		if (result.size() > 1) {
			throw new RuntimeException("duplicate main class");
		}
		for (String cls : result) {
			System.out.println(cls);
		}
	}
}
