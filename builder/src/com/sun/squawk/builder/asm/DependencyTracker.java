package com.sun.squawk.builder.asm;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.objectweb.asm.ClassReader;

public class DependencyTracker {
	DependencyVisitor v;
	
	DependencyTracker(DependencyVisitor v) {
		this.v = v;
	}

	void visitDirectory(File f) throws IOException {
		File[] files = f.listFiles();
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (file.isDirectory()) {
				visitDirectory(file);
			} else {
				String name = file.getName();
				if (name.endsWith(".class")) {
//					System.out.println("reading " + file);
					FileInputStream fin = new FileInputStream(file);
					new ClassReader(fin).accept(v, 0);
					fin.close();
				}
			}
		}
	}
	
    public void run(final String[] args) throws IOException {
		for (int i = 0; i < args.length; i++) {
			File file = new File(args[i]);
			if (file.isDirectory()) {
				visitDirectory(file);
			} else {
				ZipFile f = new ZipFile(file);

				Enumeration<? extends ZipEntry> en = f.entries();
				while (en.hasMoreElements()) {
					ZipEntry e = en.nextElement();
					String name = e.getName();
					if (name.endsWith(".class")) {
						new ClassReader(f.getInputStream(e)).accept(v, 0);
					}
				}
			}
		}
    }
}
