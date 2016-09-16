package com.sun.squawk.builder.glue;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import java.util.*;
import java.util.zip.*;
import java.io.*;

public class NativeMethodFinder extends ClassVisitor {

	private String className;
	private Handler handler;
	
    public NativeMethodFinder(NativeMethodFinder.Handler handler) {
        super(Opcodes.ASM5);
		this.handler = handler;
	}
	
    @Override
    public void visit(final int version, final int access, final String name,
            final String signature, final String superName,
            final String[] interfaces) {
		this.className = name;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name,
            final String desc, final String signature, final String[] exceptions) {
		boolean isNative = false;
		if ((access & Opcodes.ACC_NATIVE) != 0) {
			isNative = true;
		} else {
			if (exceptions != null) {
				for (int i = 0; i < exceptions.length; i++) {
					if (exceptions[i].equals("com/sun/squawk/pragma/NativePragma")) {
						isNative = true;
						break;
					}
				}
			}
		}
		if (isNative) {
			handler.callback(className, name, desc, (access & Opcodes.ACC_STATIC) != 0);
		}
		return null;
    }

	void processDir(File d) throws IOException {
		File[] files = d.listFiles();
		for (int i = 0; i < files.length; i++) {
			File child = files[i];
			if (child.isDirectory()) {
				processDir(child);
			} else if (child.getName().endsWith(".class")) {
				new ClassReader(new FileInputStream(child)).accept(this, 0);
			}
		}
	}

	void processJar(File d) throws IOException {
		ZipFile z = new ZipFile(d);
		for (Enumeration<? extends ZipEntry> e = z.entries(); e.hasMoreElements(); ) {
			ZipEntry entry = e.nextElement();
			String name = entry.getName();
			if (name.endsWith(".class")) {
				InputStream in = z.getInputStream(entry);
				new ClassReader(in).accept(this, 0);
			}
		}
	}
	public static interface Handler {
		void callback(String className, String methodName, String desc, boolean isStatic);
	}
	
	public void run(String[] args) throws IOException {
		for (int i = 0; i < args.length; i++) {
			File f = new File(args[i]);
			if (!f.exists()) {
				System.out.println(f + " not found");
				return;
			}
			if (f.isDirectory()) {
				processDir(f);
			} else {
				processJar(f);
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		NativeMethodFinder v = new NativeMethodFinder(new NativeMethodFinder.Handler() {
				public void callback(String className, String methodName, String desc, boolean isStatic) {
					System.out.println(className + "." + methodName + desc);
				}
			});
		v.run(args);
	}
}
