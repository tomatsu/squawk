package com.sun.squawk.builder.asm;

import java.util.*;
import org.objectweb.asm.Opcodes;

class MethodNode {
	ClassNode definingClass;
	String name;
	String desc;
	String[] exceptions;
	int access;
	Set<MethodNode> methodRef = new HashSet<MethodNode>();
	Set<FieldNode> fieldRef = new HashSet<FieldNode>();
	Set<ClassNode> classRef = new HashSet<ClassNode>();

	MethodNode(ClassNode definingClass, String name, String desc) {
		this.definingClass = definingClass;
		this.name = name;
		this.desc = desc;
	}

	void addFieldRef(FieldNode field) {
		fieldRef.add(field);
	}

	void addMethodRef(MethodNode m) {
		methodRef.add(m);
	}

	void addClassRef(ClassNode cls) {
		classRef.add(cls);
	}

	boolean isAbstract() {
		return (access & Opcodes.ACC_ABSTRACT) != 0;
	}
	
	public String toString() {
		return definingClass + "." + name + desc;
	}
}

