package com.sun.squawk.builder.dca;

import java.util.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class MethodNode implements Opcodes {
    ClassNode classNode;
    int access;
    String name;
    String desc;
    String signature;
    ClassNode[] exceptions;
    Set<ClassNode> classRefs = new HashSet<>();
    
    Set<MethodNode> callees = new HashSet<>();
    Set<MethodNode> callers = new HashSet<>();
    boolean defined;
    boolean required;

    MethodNode(ClassNode classNode, String name, String desc) {
	this.classNode = classNode;
	this.name = name;
	this.desc = desc;
	this.defined = false;
    }
    
    MethodNode(ClassNode classNode, int access, String name, String desc, String signature, ClassNode[] exceptions) {
	this.classNode = classNode;
	this.access = access;
	this.name = name;
	this.desc = desc;
	this.signature = signature;
	this.exceptions = exceptions;
	this.defined = true;
    }

    public String getName() {
	return name;
    }

    public String getDesc(){
	return desc;
    }

    public String getSignature(){
	return signature;
    }

    public int getAccess(){
	return access;
    }

    public ClassNode getClassNode() {
	return classNode;
    }

    void addClassRef(ClassNode cn) {
	classRefs.add(cn);
    }

    Set<ClassNode> getClassRefs() {
	return classRefs;
    }
    
    public String getId() {
	return classNode.getName() + "." + name + desc;
    }

    public static String getClassNameFromId(String id) {
	    int idx = id.indexOf('.');
	    if (idx < 0) {
		    throw new RuntimeException("illegal format");
	    }
	    return id.substring(0, idx);
    }

    public static MethodNode create(Env env, ClassNode classNode, int access, String name, String desc, String signature, String[] exceptions) {
	Type[] argTypes = Type.getArgumentTypes(desc);
	Type retType = Type.getReturnType(desc);
	Set<ClassNode> refs = new HashSet<>();
	if (retType.getSort() == Type.OBJECT) {
	    refs.add(env.registerClass(retType.getInternalName()));
	}
	for (int i = 0; i < argTypes.length; i++) {
	    Type t = argTypes[i];
	    if (t.getSort() == Type.OBJECT) {
		refs.add(env.registerClass(t.getInternalName()));
	    }
	}
	
	ClassNode[] e = null;
	if (exceptions != null) {
	    e = new ClassNode[exceptions.length];
	    for (int i = 0; i < exceptions.length; i++) {
		e[i] = env.registerClass(exceptions[i]);
		refs.add(e[i]);
	    }
	}
	MethodNode mn = classNode.addMethod(access, name, desc, signature, e);
	
	for (ClassNode ref : refs) {
	    mn.addClassRef(ref);
	}
	return mn;
    }

    void callMethod(Env env, String owner, String name, String desc) {
	ClassNode cn = env.registerClass(owner);
	MethodNode callee = cn.registerMethod(name, desc);
	if (callee != this) {
	    callees.add(callee);
	}
	callee.callers.add(this);
    }

    void accessField(Env env, String owner, String name, String desc, int opcode) {
	ClassNode cn = env.registerClass(owner);
	FieldNode fn = cn.registerField(name, desc, (opcode == PUTSTATIC) || (opcode == GETSTATIC));
	fn.accessors.add(this);
    }
    
    void accessType(Env env, String type) {
	if (type == null) {
	    return;
	}
	Type t = Type.getObjectType(type);
	int sort = t.getSort();
	if (sort == Type.ARRAY) {
	    t = t.getElementType();
	} else if (sort != Type.OBJECT) {
	    throw new RuntimeException("illegal sort value : " + sort);
	}
	sort = t.getSort();
	if (sort == Type.OBJECT) {
	    classRefs.add(env.registerClass(t.getInternalName()));
	}
    }
		    
    void setRequired(Env env) {
	if (!required) {
	    env.updated = true;
	}
	required = true;
    }
    
    boolean checkRequired(Env env) {
	if (classNode.required) {
	    if (name.equals("<clinit>")/* || (name.equals("<init>") && desc.equals("()V"))*/) {
		if (env.debug) {
		    System.out.println(getId() + " is required, because " + classNode.getName() + " is required");
		}
		setRequired(env);
	    } else if (name.equals("run") && desc.equals("()V") && classNode.isRunnable()) {
		if (env.debug) {
		    System.out.println(getId() + " is required, because " + classNode.getName() + " is required");
		}
		setRequired(env);
	    } else if (name.equals("readObject") && desc.equals("(Ljava/io/ObjectInputStream;)V")) {
		if (env.debug) {
		    System.out.println(getId() + " is required, because " + classNode.getName() + " is required");
		}
		setRequired(env);
	    } else if (name.equals("writeObject") && desc.equals("(Ljava/io/ObjectOutputStream;)V")) {
		if (env.debug) {
		    System.out.println(getId() + " is required, because " + classNode.getName() + " is required");
		}
		setRequired(env);
	    } else if (name.equals("writeReplace") && desc.equals("()Ljava/lang/Object;")) {
		if (env.debug) {
		    System.out.println(getId() + " is required, because " + classNode.getName() + " is required");
		}
		setRequired(env);
	    } else if (name.equals("readResolve") && desc.equals("()Ljava/lang/Object;")) {
		if (env.debug) {
		    System.out.println(getId() + " is required, because " + classNode.getName() + " is required");
		}
		setRequired(env);
	    } else if (name.equals("readObjectNoData") && desc.equals("()V")) {
		if (env.debug) {
		    System.out.println(getId() + " is required, because " + classNode.getName() + " is required");
		}
		setRequired(env);
	    }
	}
	
	MethodNode pm = null;

	/* this would make configuration for dynamic class loading much easier, but may miss a lot of dead code
	if (defined && (classNode.enclosingMethod == null)) {
	    if (!name.equals("<clinit>")) {
		for (ClassNode superType : classNode.getSuperTypes()) {
		    if (!"java/lang/Object".equals(superType.getName())) {
			pm = superType.getMethod(name, desc);
			if (pm != null) {
			    if (pm.required) {
				if (env.debug) {
				    System.out.println(getId() + " is required, because " + pm.getId() + " is required");
				}
				setRequired(env);
			    }
			}
		    }
		}
	    }
	}
	*/
	for (MethodNode caller : callers) {
	    if (caller.required) {
		if (env.debug) {
		    System.out.println(getId() + " is required, because " + caller.getId() + " is required");
		}
		setRequired(env);
	    }
	}
	if (required) {
	    for (ClassNode ref : getClassRefs()) {
		if (env.debug) {
		    System.out.println(ref.getName() + " is required, because " + getId() + " is required");
		}
		ref.setRequired(env);
	    }
	    if (!defined) {
		for (ClassNode superType : classNode.getSuperTypes()) {
		    pm = superType.getMethod(name, desc);
		    if (pm != null) {
			if (env.debug) {
			    System.out.println(pm.getId() + " is required, because " + getId() + " is required");
			}
			pm.setRequired(env);
			
			if (superType.isInterface()) {
			    for (ClassNode impl : superType.implClasses) {
				if (impl.enclosingMethod == null) {
				    pm = impl.getMethod(name, desc);
				    if (pm != null) {
					if (env.debug) {
					    System.out.println(pm.getId() + " is required (*), because " + getId() + " is required");
					}
					pm.setRequired(env);
				    }
				    for (ClassNode s : impl.getSuperTypes()) {
					if (s.enclosingMethod == null) {
					    pm = s.getMethod(name, desc);
					    if (pm != null) {
						if (env.debug) {
						    System.out.println(pm.getId() + " is required (**), because " + getId() + " is required");
						}
						pm.setRequired(env);
					    }
					}
				    }
				}
			    }
			}
		    }			
		}
	    }
	}
	return required;
    }
    
    public String toString() {
	StringBuilder sb = new StringBuilder(classNode.getName() + "." + name + desc);
	return sb.toString();
    }
}
