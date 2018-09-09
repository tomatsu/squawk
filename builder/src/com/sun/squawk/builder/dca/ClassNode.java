package com.sun.squawk.builder.dca;

import java.util.*;
import org.objectweb.asm.Opcodes;

public class ClassNode implements Opcodes {
    Map<String,MethodNode> methodNodes = new HashMap<String,MethodNode>();
    Map<String,FieldNode> fieldNodes = new HashMap<String,FieldNode>();
    Set<ClassNode> classRefs = new HashSet<>();
    ClassNode enclosingClass;
    MethodNode enclosingMethod;
    
    String name;
    ClassNode[] interfaces;
    ClassNode superClass;
    Set<ClassNode> implClasses; // for interface
    String signature;
    int access;
    int version;
    boolean defined = false;
    boolean required;

    ClassNode(String name) {
	this.name = name;
    }

    ClassNode(int version, int access, String name, String signature, ClassNode superClass, ClassNode[] interfaces) {
	this.version = version;
	this.access = access;
	this.name = name;
	this.signature = signature;
	this.superClass = superClass;
	this.interfaces = interfaces;
	this.defined = true;
    }

    void addImplementation(ClassNode impl) {
	Set<ClassNode> s = this.implClasses;
	if (s == null) {
	    s = new HashSet<>();
	    this.implClasses = s;
	}
	s.add(impl);
    }

    public String getName() {
	return name;
    }

    public boolean isInterface() {
	return ((access & ACC_INTERFACE) != 0);
    }

    public ClassNode getSuperclass() {
	return superClass;
    }

    public boolean isSerializable() {
	if (isInterface()) {
	    return false;
	}
	for (ClassNode t : getSuperTypes()) {
	    if (t.isInterface() && t.getName().equals("java/io/Serializable")) {
		return true;
	    }
	}
	return false;
    }

    public boolean isRunnable() {
	if (getName().equals("java/lang/Runnable")) {
	    return true;
	}
	for (ClassNode s : getSuperTypes()) {
	    if (s.isRunnable()) {
		return true;
	    }
	}
	return false;
    }

    void addClassRef(ClassNode cn) {
	classRefs.add(cn);
    }

    Set<ClassNode> getClassRefs() {
	return classRefs;
    }
    
    public static ClassNode create(Env env, int version, int access, String name, String signature, String superName, String[] interfaces) {
	ClassNode[] itf = null;
	if (interfaces != null) {
	    itf = new ClassNode[interfaces.length];
	    for (int i = 0; i < interfaces.length; i++) {
		itf[i] = env.registerClass(interfaces[i]);
	    }
	}
	ClassNode sp = null;
	if (superName == null && !"java/lang/Object".equals(name)) {
	    throw new RuntimeException();
	}
	if (superName != null) {
	    sp = env.registerClass(superName);
	}
	ClassNode cn = env.addClassNode(version, access, name, signature, sp, itf);
	if (sp != null) {
	    cn.addClassRef(sp);
	}
	for (int i = 0; i < itf.length; i++) {
	    cn.addClassRef(itf[i]);
	}
	return cn;
    }

    MethodNode getMethod(String name, String desc) {
	String id = name + desc;
	return methodNodes.get(id);
    }

    MethodNode registerMethod(String name, String desc) {
	String id = name + desc;
	MethodNode mn = methodNodes.get(id);
	if (mn == null) {
	    methodNodes.put(id, mn = new MethodNode(this, name, desc));
	}
	return mn;	
    }
    
    MethodNode addMethod(int access, String name, String desc, String signature, ClassNode[] exceptions) {
	String id = name + desc;
	MethodNode mn = methodNodes.get(id);
	if (mn == null) {
	    methodNodes.put(id, mn = new MethodNode(this, access, name, desc, signature, exceptions));
	} else {
	    mn.access = access;
	    mn.signature = signature;
	    mn.exceptions = exceptions;
	    mn.defined = true;
	}
	return mn;	
    }

    FieldNode getField(String name) {
	return fieldNodes.get(name);
    }

    FieldNode registerField(String name, String desc, boolean isStatic) {
	FieldNode fn = fieldNodes.get(name);
	if (fn == null) {
	    fieldNodes.put(name, fn = new FieldNode(this, name, desc, isStatic));
	}
	return fn;
    }

    FieldNode addField(int access, String name, String desc, String signature, Object value) {
	FieldNode fn = fieldNodes.get(name);
	if (fn == null) {
	    fieldNodes.put(name, fn = new FieldNode(this, access, name, desc, signature, value));
	} else {
	    if (!desc.equals(fn.desc)) {
		throw new RuntimeException("inconsistent field desc");
	    }
	    if (((access & ACC_STATIC) != 0) ^ fn.isStatic) {
		throw new RuntimeException("inconsistent field use: " + (this.name + "." + name));
	    }
	    fn.access = access;
	    fn.signature = signature;
	    fn.value = value;
	    fn.defined = true;
	}
	return fn;
    }

    Set<ClassNode> getSuperTypes() {
	Set<ClassNode> types = new HashSet<>();
	if (superClass != null) {
	    types.add(superClass);
	}
	if (interfaces != null) {
	    for (int i = 0; i < interfaces.length; i++) {
		types.add(interfaces[i]);
	    }
	}
	getSuperTypes(types);
	return types;
    }
    
    private void getSuperTypes(Set<ClassNode> types) {
	Set<ClassNode> newTypes = new HashSet<>(types);
	while (true) {
	    Set<ClassNode> next = new HashSet<>();
	    for (ClassNode cn : newTypes) {
		if (cn.superClass != null) {
		    if (types.add(cn.superClass)) {
			next.add(cn.superClass);
		    }
		}
		if (cn.interfaces != null) {
		    for (int i = 0; i < cn.interfaces.length; i++) {
			if (types.add(cn.interfaces[i])) {
			    next.add(cn.interfaces[i]);
			}
		    }
		}
	    }
	    if (next.isEmpty()) {
		break;
	    }
	    newTypes = next;
	}
    }

    void requireAllMembers(Env env) {
	for (Map.Entry<String,MethodNode> entry : methodNodes.entrySet()) {
	    MethodNode mn = entry.getValue();
	    if (env.debug) {
		    System.out.println(mn.getId() + " is required by configuration");
	    }
	    mn.setRequired(env);
	}
	for (Map.Entry<String,FieldNode> entry : fieldNodes.entrySet()) {
	    FieldNode fn = entry.getValue();		
	    if (env.debug) {
		    System.out.println(fn.getId() + " is required by configuration");
	    }
	    fn.setRequired(env);
	}
    }

    void setRequired(Env env) {
	if (!required) {
	    env.updated = true;
	}
	required = true;
    }
    
    boolean checkRequired(Env env) {
	for (Map.Entry<String,MethodNode> entry : methodNodes.entrySet()) {
	    MethodNode mn = entry.getValue();
	    if (mn.checkRequired(env)) {
		if (env.debug) {
		    System.out.println(getName() + " is required, because " + mn.getId() + " is required.");
		}
		setRequired(env);
	    }
	}
	for (Map.Entry<String,FieldNode> entry : fieldNodes.entrySet()) {
	    FieldNode fn = entry.getValue();
	    if (fn.checkRequired(env)) {
		if (env.debug) {
		    System.out.println(getName() + " is required, because " + fn.getId() + " is required.");
		}
		setRequired(env);
	    }
	}
	if (required) {
	    for (ClassNode ref : classRefs) {
		if (env.debug) {
		    System.out.println(ref.getName() + " is required, because " + getName() + " is required.");
		}
		ref.setRequired(env);
	    }

	    Set<String> requiredMethods = new HashSet<>();
	    for (ClassNode t : getSuperTypes()) {	    
		for (Map.Entry<String,MethodNode> entry : t.methodNodes.entrySet()) {
		    MethodNode mn = entry.getValue();
		    if (mn.required) {
/*if[MINIMAL_ERROR_REPORT]*/
			if (!mn.getId().equals("java/lang/Object.toString()Ljava/lang/String;")) {
			    if (env.debug) {
				System.out.println(getName() + "." + mn.getName() + mn.getDesc() + " overrides " + mn.getId());
			    }
			    requiredMethods.add(mn.getName() + mn.getDesc());
			}
/*else[MINIMAL_ERROR_REPORT]*/
//			requiredMethods.add(mn.getName() + mn.getDesc());
/*end[MINIMAL_ERROR_REPORT]*/
		    }
		}
	    }
	    for (Map.Entry<String,MethodNode> entry : methodNodes.entrySet()) {
		MethodNode mn = entry.getValue();
		if (!mn.name.equals("<init>") && requiredMethods.contains(mn.getName() + mn.getDesc())) {
		    if (env.debug) {
			System.out.println(mn.getId() + " is required.");
		    }
		    mn.setRequired(env);
		}
	    }
	    Set<String> constructorDesc = new HashSet<>();
	    boolean needConstructor = true;
	    for (Map.Entry<String,MethodNode> entry : methodNodes.entrySet()) {
		MethodNode mn = entry.getValue();
		if (mn.name.equals("<init>")) {
		    constructorDesc.add(mn.desc);
		    if (mn.required) {
			needConstructor = false;
			break;
		    }
		}
	    }
	    if (needConstructor) {
		if (constructorDesc.contains("()V")) {
		    MethodNode mn = getMethod("<init>", "()V");
		    if (env.debug) {
			System.out.println(mn.getId() + " is required, because the class must have at least one constructor");
		    }
		    mn.setRequired(env);
		} else {
		    for (String desc : constructorDesc) {
			MethodNode mn = getMethod("<init>", desc);
			if (env.debug) {
			    System.out.println(mn.getId() + " is required, because the class must have at least one constructor");
			}
			mn.setRequired(env);
			break;
		    }
		}
	    }
	}
	return required;
    }
    
    public String toString() {
	StringBuilder sb = new StringBuilder("Class " + name + "\n");
	for (Map.Entry<String,MethodNode> entry : methodNodes.entrySet()) {
	    sb.append(" ");
	    sb.append(entry.getValue().toString());
	    sb.append("\n");
	}
	return sb.toString();
    }
}

