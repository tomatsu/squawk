package com.sun.squawk.builder.asm;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.*;

public class Shrink {

	/*
	 * Usage:
	 *  <input_dir> <output_dir> <main_class> classpaths..
	 *  <input_jar> <output_dir> <main_class> classpaths..
	 *  <input_jar> <output_jar> <main_class> classpaths..
	 */
	public static void main(String[] args) throws IOException {
		String[] classpaths = new String[args.length - 3];
		System.arraycopy(args, 3, classpaths, 0, args.length - 3);
		DependencyAnalyzer analyzer = DependencyAnalyzer.analyze(args[2], classpaths);
		Shrink f = new Shrink(analyzer);
		File in = new File(args[0]);
		File out = new File(args[1]);
		if (in.isDirectory()) {
			f.processDir(in, out);
		} else {
			f.processJar(in, out);
		}
	}

	private DependencyAnalyzer analyzer;

	Shrink(DependencyAnalyzer analyzer) {
		this.analyzer = analyzer;
	}
	
	void processDir(File input, File output) throws IOException {
		processDir(input, output, "");
	}
	
	void processDir(File input, File output, String prefix) throws IOException {
		String infiles[] = input.list();
		for (int i = 0; i < infiles.length; i++) {
			String name = infiles[i];
			File f = new File(input, name);
			if (f.isDirectory()) {
				File dest = new File(output, name);
				processDir(f, dest, ("".equals(prefix) ? "" : prefix + "/") + name);
			} else {
				File dest = new File(output, name);
				File src = new File(input, name);
				if (name.endsWith(".class")) {
					String id = ("".equals(prefix) ? "" : prefix + "/") + name.substring(0, name.length() - 6);
					if (isUsedClass(id)) {
						File dir = dest.getParentFile();
						if (!dir.exists()) {
							dir.mkdirs();
						}
						FileInputStream fin = new FileInputStream(src);
						FileOutputStream fout = new FileOutputStream(dest);
						process(id, fin, fout);
						fin.close();
						fout.close();
					} else {
//						System.out.println("# deleted " + id);
					}
				} else if (!name.endsWith("/")) {
					File dir = dest.getParentFile();
					if (!dir.exists()) {
						dir.mkdirs();
					}
					FileInputStream fin = new FileInputStream(src);
					FileOutputStream fout = new FileOutputStream(dest);
					copy(fin, fout);
					fin.close();
					fout.close();
				}
			}
		}
				
	}
	
	void processJar(File input, File output) throws IOException {
		ZipInputStream zin = new ZipInputStream(new FileInputStream(input));
		if (output.isDirectory()) {
			processJar(zin, output);
		} else {
			ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(output));
			processJar(zin, zout);
			zout.close();
		}
		zin.close();
	}
		
	void processJar(ZipInputStream zin, File output) throws IOException {
		while (true) {
			ZipEntry entry = zin.getNextEntry();
			if (entry != null){
				String name = entry.getName();
				File dest = new File(output, name);
				if (name.endsWith(".class")){
					String id = name.substring(0, name.length()-6);
					if (isUsedClass(id)) {
						File dir = dest.getParentFile();
						if (!dir.exists()) {
							dir.mkdirs();
						}
						FileOutputStream out = new FileOutputStream(dest);
						process(id, zin, out);
						out.close();
					}
				} else if (!name.endsWith("/")) {
					File dir = dest.getParentFile();
					if (!dir.exists()) {
						dir.mkdirs();
					}
					FileOutputStream out = new FileOutputStream(dest);
					copy(zin, out);
					out.close();
				}
			} else {
				break;
			}
		}
	}

	void processJar(ZipInputStream zin, ZipOutputStream zout) throws IOException {
		byte[] buf = new byte[512];
		int n;
		while (true) {
			ZipEntry entry = zin.getNextEntry();
			if (entry != null){
				String name = entry.getName();
				if (name.endsWith(".class")){
					String id = name.substring(0, name.length()-6);
					if (isUsedClass(id)) {
						zout.putNextEntry(entry);
						while ((n = zin.read(buf)) != -1) {
							zout.write(buf, 0, n);
						}
					}
				} else if (!name.endsWith("/")) {
					zout.putNextEntry(entry);
					while ((n = zin.read(buf)) != -1) {
						zout.write(buf, 0, n);
					}
				}
			} else {
				break;
			}
		}
	}
				
	boolean isUsedClass(String name) {
		return analyzer.isUsedClass(name);
	}
	
	boolean isUsedMethod(String name) {
		return analyzer.isUsedMethod(name);
	}

	boolean isUsedField(String name) {
		return analyzer.isUsedField(name);
	}
		
	static void copy(InputStream in, OutputStream out) throws IOException {
		byte[] buf = new byte[512];
		int n;
		while ((n = in.read(buf)) != -1){
			out.write(buf, 0, n);
		}
	}
		
	void process(final String className, InputStream in, OutputStream out) throws IOException {
		ClassReader cr = new ClassReader(in);
		ClassWriter cw = new ClassWriter(0);
		ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
			
		public MethodVisitor visitMethod(int access, String name, String desc,
										 String signature, String[] exceptions) {
					String id = className + "." + name + desc;
					if (!isUsedMethod(id)) {
						return null;
					}
					return super.visitMethod(access, name, desc, signature, exceptions);
				}
								
				public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
					String id = className + "." + name;
					if (!isUsedField(id)) {
						return null;
					}
					return super.visitField(access, name, desc, signature, value);
				}
			};
		cr.accept(cv, ClassReader.SKIP_DEBUG);

		byte[] code = cw.toByteArray();
		out.write(code);
	}

}
