package com.sun.squawk.builder.dca;

import java.util.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class FieldNode implements Opcodes {
    ClassNode classNode;
    int access;
    String name;
    String desc;
    String signature;
    Object value;
    boolean isStatic;
    Set<MethodNode> accessors = new HashSet<>();
    ClassNode typeNode;

    boolean defined;
    boolean required;
    static int count = 0;

    FieldNode(ClassNode classNode, String name, String desc, boolean isStatic) {
	this.classNode = classNode;
	this.name = name;
	this.desc = desc;
	this.isStatic = isStatic;
	this.defined = false;
    }

    FieldNode(ClassNode classNode, int access, String name, String desc, String signature, Object value) {
	this.classNode = classNode;
	this.access = access;
	this.name = name;
	this.desc = desc;
	this.defined = true;
    }

    public String getName() {
	return name;
    }

    public String getDesc(){
	return desc;
    }

    public int getAccess(){
	return access;
    }

    public ClassNode getClassNode() {
	return classNode;
    }

    public String getId() {
	return classNode.getName() + "." + name;
    }

    public static String getClassNameFromId(String id) {
	    int idx = id.indexOf('.');
	    if (idx < 0) {
		    throw new RuntimeException("illegal format");
	    }
	    return id.substring(0, idx);
    }

    public static FieldNode create(Env env, ClassNode classNode, int access, String name, String desc, String signature, Object value) {
	Type t = Type.getType(desc);
	ClassNode typeNode = null;
	if (t.getSort() == Type.OBJECT) {
	    typeNode = env.registerClass(t.getInternalName());
	}
	FieldNode fn = classNode.addField(access, name, desc, signature, value);
	fn.typeNode = typeNode;
	return fn;
    }

    void setRequired(Env env) {
	if (!required) {
	    env.updated = true;
	}
	required = true;
    }
    
    boolean checkRequired(Env env) {
	if (classNode.required && name.equals("serialVersionUID")) {
	    if (env.debug) {
		System.out.println(getId() + " is required, because " + classNode.getName() + " is required");
	    }
	    setRequired(env);
	} else if (classNode.required && classNode.isSerializable() && ((access & ACC_TRANSIENT) == 0)) {
	    if (env.debug) {
		System.out.println(getId() + " is required, because " + classNode.getName() + " is required");
	    }
	    setRequired(env);
	} else {
	    for (MethodNode mn : accessors) {
		if (mn.required) {
		    if (env.debug) {
			System.out.println(getId() + " is required, because " + mn.getId() + " is required");
		    }
		    setRequired(env);

		    if (!defined) {
			for (ClassNode superType : classNode.getSuperTypes()) {
			    FieldNode fn = superType.getField(name);
			    if (fn != null) {
				if (env.debug) {
				    System.out.println(fn.getId() + " is required, because " + getId() + " is required");
				}
				fn.setRequired(env);
			    }
			}
		    }
		    break;
		}
	    }
	}
	if (required && typeNode != null) {
	    if (env.debug) {
		System.out.println(typeNode.getName() + " is required, because " + getId() + " is required");
	    }
	    typeNode.setRequired(env);
	}
	return required;
    }
    
    public String toString() {
	StringBuilder sb = new StringBuilder(classNode.getName() + "." + name + " " + desc);
	return sb.toString();
    }
}
