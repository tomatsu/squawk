package com.sun.squawk.builder.asm;

import java.util.*;
	
class ClassNode {
	Set<MethodNode> methods = new HashSet<MethodNode>();
	Set<FieldNode> fields = new HashSet<FieldNode>();
	Set<ClassNode> classRef = new HashSet<ClassNode>();
	Set<ClassNode> superTypes = new HashSet<ClassNode>();
	String name;
	int access;

	ClassNode(String name) {
		this.name = name;
	}

	void setSuperTypes(Set<ClassNode> superTypes) {
		this.superTypes = superTypes;
	}
	
	void addField(FieldNode f) {
		fields.add(f);
	}
	
	void addMethodRef(MethodNode m) {
		methods.add(m);
	}
	
	void addClassRef(ClassNode cls) {
		classRef.add(cls);
	}

	String getName() {
		return name;
	}

	boolean isAssignableFrom(ClassNode n) {
		for (ClassNode c : superTypes) {
			if (c == n) {
				return true;
			}
			if (c.isAssignableFrom(n)) {
				return true;
			}
		}
		return false;
	}
	
	public String toString() {
		return name;
	}
}
