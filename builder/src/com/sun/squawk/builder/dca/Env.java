package com.sun.squawk.builder.dca;

import java.util.*;
import java.io.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.*;

public class Env {
    Map<String,ClassNode> classNodes = new HashMap<String,ClassNode>();
    Set<ClassNode> mandatoryClassEntries = new HashSet<>();
    DeadCodeAnalyzer dca;
    boolean debug;

    public Env(DeadCodeAnalyzer dca) {
	this.dca = dca;
	this.debug = dca.debug;
    }

    ClassNode registerClass(String name) {
	ClassNode cn = classNodes.get(name);
	if (cn == null) {
	    classNodes.put(name, cn = new ClassNode(name));
	}
	return cn;
    }
    
    public ClassNode getClassNode(String name) {
	return classNodes.get(name);
    }

    public MethodNode getMethodNode(String id) {
	int idx = id.indexOf('.');
	if (idx < 0) {
	    throw new RuntimeException("illegal method id: " + id);
	}
	String clazz = id.substring(0, idx);
	ClassNode cn = getClassNode(clazz);
	if (cn == null) {
	    throw new RuntimeException("no class definition: " + clazz);
	}
	int idx2 = id.indexOf('(', idx + 1);
	if (idx2 < 0) {
	    throw new RuntimeException("illegal method id: " + id);
	}
	return cn.getMethod(id.substring(idx + 1, idx2), id.substring(idx2));
    }

    public FieldNode getFieldNode(String id) {
	int idx = id.indexOf('.');
	if (idx < 0) {
	    throw new RuntimeException("illegal field id: " + id);
	}
	String clazz = id.substring(0, idx);
	ClassNode cn = getClassNode(clazz);
	if (cn == null) {
	    //	    throw new RuntimeException("no class definition: " + clazz);
	    return null;
	}
	String fieldName = id.substring(idx + 1);
	FieldNode fn = cn.getField(fieldName);
	if (fn == null) {
	    for (ClassNode superType : cn.getSuperTypes()) {
		fn = superType.getField(fieldName);
		if (fn != null) {
		    return fn;
		}
	    }
	}
	return fn;
    }

    public ClassNode addClassNode(int version, int access, String name, String signature, ClassNode superClass, ClassNode[] interfaces) {
	ClassNode c = getClassNode(name);
	if (c == null) {
	    c = new ClassNode(version, access, name, signature, superClass, interfaces);
	    classNodes.put(name, c);
	} else {
	    c.version = version;
	    c.access = access;
	    c.signature = signature;
	    c.superClass = superClass;
	    c.interfaces = interfaces;
	    c.defined = true;
	}

	if (interfaces != null) {
	    for (int i = 0; i < interfaces.length; i++) {
		interfaces[i].addImplementation(c);
	    }
	}
	return c;
    }

    public void analyze() throws IOException {
	String line;
	BufferedReader reader;

	String[] files = dca.config.split(",");
	for (int i = 0; i < files.length; i++) {
	    String configFile = files[i];
	    reader = new BufferedReader(new FileReader(configFile));
	    while ((line = reader.readLine()) != null) {
		processConfigLine(line);
	    }
	}

	markRequiredEntires();
	if (dca.used != null) {
	    reportUsedEntries();
	}
	if (dca.outputFile !=null) {
	    reportUnusedEntries();
	}
	if (dca.xref != null) {
	    reportCrossReferences();
	}
	if (dca.itf != null) {
	    reportUnneededInterfaces();
	}
    }

    boolean updated;
    
    void markRequiredEntires() {
	Collection<ClassNode> classes = classNodes.values();
	while (true) {
	    this.updated = false;
	    for (ClassNode cn : classes) {
		cn.checkRequired(this);
	    }
	    if (!updated) {
		break;
	    }
	}
    }

    void reportUsedEntries() throws IOException {
	PrintWriter pw = null;
	try {
	    pw = new PrintWriter(new FileWriter(dca.used));
	    for (Map.Entry<String,ClassNode> e : classNodes.entrySet()) {
		ClassNode cn = e.getValue();
		if (cn.required) {
		    pw.println(cn.getName());
		}
		for (Map.Entry<String,MethodNode> entry : cn.methodNodes.entrySet()) {
		    MethodNode mn = entry.getValue();
		    if (mn.required) {
			pw.println(mn.getId());
		    }
		}
		
		for (Map.Entry<String,FieldNode> entry : cn.fieldNodes.entrySet()) {
		    FieldNode fn = entry.getValue();
		    if (fn.required) {
			pw.println(fn.getId());
		    }
		}
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	} finally {
	    if (pw != null) {
		pw.close();
	    }
	}
    }
    void reportUnusedEntries() throws IOException {
	PrintWriter pw = null;
	try {
	    pw = new PrintWriter(new FileWriter(dca.outputFile));
	    for (Map.Entry<String,ClassNode> e : classNodes.entrySet()) {
		ClassNode cn = e.getValue();
		if (!cn.required) {
		    pw.println(cn.getName());
		}
		for (Map.Entry<String,MethodNode> entry : cn.methodNodes.entrySet()) {
		    MethodNode mn = entry.getValue();
		    if (!mn.required) {
			pw.println(mn.getId());
		    }
		}
		
		for (Map.Entry<String,FieldNode> entry : cn.fieldNodes.entrySet()) {
		    FieldNode fn = entry.getValue();
		    if (!fn.required) {
			pw.println(fn.getId());
		    }
		}
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	} finally {
	    if (pw != null) {
		pw.close();
	    }
	}
    }

    void reportCrossReferences() throws IOException {
	PrintWriter pw = null;
	boolean onlyRequired = dca.onlyRequired;
	try {
	    pw = new PrintWriter(new FileWriter(dca.xref));

	    for (Map.Entry<String,ClassNode> e : classNodes.entrySet()) {
		ClassNode cn = e.getValue();
		for (Map.Entry<String,FieldNode> entry : cn.fieldNodes.entrySet()) {
		    FieldNode f = entry.getValue();
		    if (!onlyRequired || f.required) {
			String id = f.getId();
			for (MethodNode a : f.accessors) {
			    if (!onlyRequired || a.required) {
				pw.println(f.getId() + " <- " + a.getId());
			    }
			}
		    }
		}
		for (Map.Entry<String,MethodNode> entry : cn.methodNodes.entrySet()) {
		    MethodNode m = entry.getValue();
		    if (!onlyRequired || m.required) {
			for (MethodNode c : m.callers) {
			    if (!onlyRequired || c.required) {
				pw.println(m.getId() + " <- " + c.getId());
			    }
			}
		    }
		}
	    }
	    
	} catch (IOException e) {
	    e.printStackTrace();
	} finally {
	    if (pw != null) {
		pw.close();
	    }
	}
    }

    private void collectUsedInterfaces(Set<ClassNode> usedInterfaces) {
	for (Map.Entry<String,ClassNode> e : classNodes.entrySet()) {
	    ClassNode cn = e.getValue();
	    if (!cn.required) {
		continue;
	    }
	    for (Map.Entry<String,MethodNode> entry : cn.methodNodes.entrySet()) {
		MethodNode mn = entry.getValue();
		if (!mn.required) {
		    continue;
		}
		String desc = mn.desc;
		Type[] argTypes = Type.getArgumentTypes(desc);
		Type retType = Type.getReturnType(desc);
		for (int i = 0; i < argTypes.length; i++) {
		    Type t = argTypes[i];
		    if (t.getSort() == Type.OBJECT) {
			ClassNode typeNode = getClassNode(t.getInternalName());
			if (typeNode != null && typeNode.isInterface()) {
			    usedInterfaces.add(typeNode);
			}
		    }
		}
		if (retType.getSort() == Type.OBJECT) {
		    ClassNode typeNode = getClassNode(retType.getInternalName());
		    if (typeNode != null && typeNode.isInterface()) {
			usedInterfaces.add(typeNode);
		    }
		}
	    }
		
	    for (Map.Entry<String,FieldNode> entry : cn.fieldNodes.entrySet()) {
		FieldNode fn = entry.getValue();
		if (!fn.required) {
		    continue;
		}
		Type t = Type.getType(fn.desc);
		ClassNode typeNode = null;
		if (t.getSort() == Type.OBJECT) {
		    typeNode = getClassNode(t.getInternalName());
		    if (typeNode != null && typeNode.isInterface()) {
			usedInterfaces.add(typeNode);
		    }
		}
	    }
	}
    }
    
    void reportUnneededInterfaces() throws IOException {
	PrintWriter pw = null;
	
	Set<ClassNode> usedInterfaces = new HashSet<>();
	collectUsedInterfaces(usedInterfaces);
	
	try {
	    pw = new PrintWriter(new FileWriter(dca.itf));
	    
	    for (Map.Entry<String,ClassNode> e : classNodes.entrySet()) {
		ClassNode cn = e.getValue();
		if (!cn.required) {
		    continue;
		}
		if (!cn.isInterface()) {
		    continue;
		}
		if (!usedInterfaces.contains(cn) && !mandatoryClassEntries.contains(cn)) {
		    pw.println(cn.getName());
		}
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	} finally {
	    if (pw != null) {
		pw.close();
	    }
	}
    }

    /**/
    final static int METHOD = 2;
    final static int FIELD = 1;
    final static int CLASS = 0;
    final static int CLASS_ALL_MEMBERS = 3;

    static int getType(String s) {
	if (s.indexOf('.') < 0) {
	    return CLASS;
	}
	if (s.indexOf('(') < 0) {
	    int i = s.indexOf('.');
	    if (s.substring(i + 1).equals("*")) {
		return CLASS_ALL_MEMBERS;
	    } else {
		return FIELD;
	    }
	}
	return METHOD;
    }
    
    void processConfigLine(String line) {
	if (line.startsWith("#")) return;
	if (line.length() < 1) return;
	String[] entry = line.split(" ");
	if (entry.length == 1) {
	    processMandatoryEntry(line);	    
	} else if (entry.length == 2) {
	    String caller = entry[0];
	    String callee = entry[1];
	    processCall(caller, callee);
	} else {
	    throw new RuntimeException("format error");
	}
    }

    void processMandatoryEntry(String id) {
	int type = getType(id);
	if (type == METHOD) {
	    MethodNode mn = getMethodNode(id);
	    if (mn == null) {
		notFound(id);
	    } else {
		mn.required = true;
	    mandatoryClassEntries.add(mn.getClassNode());
	    }
	} else if (type == FIELD) {
	    FieldNode fn = getFieldNode(id);
	    if (fn == null) {
		notFound(id);
	    } else {
		fn.required = true;
	    mandatoryClassEntries.add(fn.getClassNode());
	    }
	} else if (type == CLASS) {
	    ClassNode cn = getClassNode(id);
	    if (cn == null) {
		notFound(id);
	    } else {
		cn.required = true;
		/*
		cn.requireAllMembers(this);
		*/
		mandatoryClassEntries.add(cn);		
	    }
	} else if (type == CLASS_ALL_MEMBERS) {
	    int idx = id.indexOf('.');
	    String n = id.substring(0, idx);
	    ClassNode cn = getClassNode(n);
	    if (cn == null) {
		notFound(id);
	    } else {
		cn.setRequired(this);
		cn.requireAllMembers(this);
	    mandatoryClassEntries.add(cn);			    
	    }
	}
    }
    
    void processCall(String caller, String callee) {
	/*
	 * method -> method
	 * method -> class
	 * method -> field
 	 * class -> class
	 */
	int callerType = getType(caller);
	int calleeType = getType(callee);

	if (callerType == METHOD) {
	    MethodNode mn = getMethodNode(caller);
	    if (mn == null) {
		notFound(caller);
	    } else {
		switch (calleeType) {
		case CLASS:
		    ClassNode c = getClassNode(callee);
		    if (c == null) {
			notFound(callee);
		    } else {
			MethodNode initializer = c.registerMethod("<clinit>", "()V");
			mn.callees.add(initializer);
			initializer.callers.add(mn);
		    }
		    break;
		case FIELD:
		    FieldNode fn = getFieldNode(callee);
		    if (fn == null) {
			notFound(callee);
		    } else {
			fn.accessors.add(mn);
		    }
		    break;
		case METHOD:
		    MethodNode mn2 = getMethodNode(callee);
		    if (mn2 == null) {
			notFound(callee);
		    } else {
			mn.callees.add(mn2);
			mn2.callers.add(mn);
		    }
		    break;
		default:
		    throw new RuntimeException("format error");
		}
	    }
	} else if (callerType == CLASS) {
	    ClassNode c1 = getClassNode(caller);
	    if (c1 == null) {
		notFound(caller);
	    } else {
		if (calleeType == CLASS) {
		    ClassNode c2 = getClassNode(callee);
		    if (c2 == null) {
			notFound(callee);
		    } else {
			c1.addClassRef(c2);
		    }
		} else {
		    throw new RuntimeException("format error");
		}
	    }
	    mandatoryClassEntries.add(c1);
	} else {
	    throw new RuntimeException("format error");
	}
    }

    void notFound(String entry){
	System.out.println(entry + " not found");
    }

}
