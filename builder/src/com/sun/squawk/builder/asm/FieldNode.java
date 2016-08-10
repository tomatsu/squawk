package com.sun.squawk.builder.asm;

class FieldNode {
	ClassNode definingClass;
	String name;
	String desc;

	FieldNode(ClassNode definingClass, String name, String desc) {
		this.definingClass = definingClass;
		this.name = name;
		this.desc = desc;
	}

	public String toString() {
		return definingClass.getName() + "." + name;
	}
}

