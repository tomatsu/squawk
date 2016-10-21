package com.sun.squawk.builder.asm;

import org.objectweb.asm.Type;
import org.objectweb.asm.Opcodes;
import java.util.*;
import java.io.*;
import java.net.URL;

public class DependencyAnalyzer {
	Map<String,ClassNode> classMap;
	Map<String,MethodNode> methodMap;
	Map<String,FieldNode> fieldMap;

	Set<ClassNode> classes;
	Set<MethodNode> methods;
	Set<FieldNode> fields;
	Set<ClassNode> systemRoot;

	static String builtinClasses[] = {
		"com/sun/squawk/Modifier",
		"com/sun/squawk/vm/HDR",
		"com/sun/squawk/vm/Native",
		"com/sun/squawk/vm/FP",
		"com/sun/squawk/vm/HDR",
		"com/sun/squawk/vm/CID",
		"com/sun/squawk/vm/SC",
		"com/sun/squawk/vm/OPC",
		"com/sun/squawk/vm/OPC$Properties",
		"com/sun/squawk/vm/AddressType",
		"com/sun/squawk/vm/MathOpcodes",
		"com/sun/squawk/vm/FieldOffsets",
		"com/sun/squawk/vm/MethodOffsets",
		"com/sun/squawk/vm/ChannelConstants",
	};
		
//	static String builtinMethods[];
//	static String builtinFields[];

	static String builtinMethods[] = {
		"com/sun/squawk/VM.startup(Lcom/sun/squawk/Suite;)V",
		"com/sun/squawk/VM.undefinedNativeMethod(I)V",
		"com/sun/squawk/VM.callRun()V",
		"com/sun/squawk/VM.getStaticOop(Lcom/sun/squawk/Klass;I)Ljava/lang/Object;",
		"com/sun/squawk/VM.getStaticInt(Lcom/sun/squawk/Klass;I)I",
		"com/sun/squawk/VM.getStaticLong(Lcom/sun/squawk/Klass;I)J",
		"com/sun/squawk/VM.putStaticOop(Ljava/lang/Object;Lcom/sun/squawk/Klass;I)V",
		"com/sun/squawk/VM.putStaticInt(ILcom/sun/squawk/Klass;I)V",
		"com/sun/squawk/VM.putStaticLong(JLcom/sun/squawk/Klass;I)V",
		"com/sun/squawk/VM.yield()V",
		"com/sun/squawk/VM.nullPointerException()V",
		"com/sun/squawk/VM.arrayIndexOutOfBoundsException()V",
		"com/sun/squawk/VM.arithmeticException()V",
		"com/sun/squawk/VM.abstractMethodError()V",
		"com/sun/squawk/VM.arrayStoreException()V",
		"com/sun/squawk/VM.monitorenter(Ljava/lang/Object;)V",
		"com/sun/squawk/VM.monitorexit(Ljava/lang/Object;)V",
		"com/sun/squawk/VM.checkcastException(Ljava/lang/Object;Lcom/sun/squawk/Klass;)V",
		"com/sun/squawk/VM.class_clinit(Lcom/sun/squawk/Klass;)V",
		"com/sun/squawk/VM._new(Lcom/sun/squawk/Klass;)Ljava/lang/Object;",
		"com/sun/squawk/VM.newarray(ILcom/sun/squawk/Klass;)Ljava/lang/Object;",
		"com/sun/squawk/VM.newdimension([Ljava/lang/Object;I)Ljava/lang/Object;",
		"com/sun/squawk/VM._lcmp(JJ)I",
		"com/sun/squawk/StringOfBytes._init_(Lcom/sun/squawk/StringOfBytes;)Lcom/sun/squawk/StringOfBytes;",
		"com/sun/squawk/Klass.setClassFileDefinition(Lcom/sun/squawk/Klass;[Lcom/sun/squawk/Klass;[Lcom/sun/squawk/ClassFileMethod;[Lcom/sun/squawk/ClassFileMethod;[Lcom/sun/squawk/ClassFileField;[Lcom/sun/squawk/ClassFileField;Ljava/lang/String;)V",
		"com/sun/squawk/Klass.changeState(B)V",
		"com/sun/squawk/JavaApplicationManager.main([Ljava/lang/String;)V",
		"java/lang/Object.notify()V",
		"java/lang/Object.notifyAll()V",
		"java/lang/Object.wait(J)V",
		"java/lang/Object.wait(JI)V",
		"java/lang/Object.wait()V",
		"java/lang/Object.getClass()Ljava/lang/Class;",
//		"java/lang/Object.hashCode()I",
//		"java/lang/Object.equals(Ljava/lang/Object;)Z",
//		"java/lang/Object.toString()Ljava/lang/String;",
		"java/lang/Object.abstractMethodError()V",
		"java/lang/Object.missingMethodError()V",
		"java/lang/Class.getComponentType()Ljava/lang/Class;",
	};

	static String builtinFields[] = {
		"com/sun/squawk/util/SquawkHashtable.entryTable",
		"java/lang/Thread.vmThread",
		"java/lang/Thread.target",
		"java/lang/Class.klass",
		"java/lang/Throwable.trace",
		"com/sun/squawk/Suite.classes",
		"com/sun/squawk/Suite.name",
		"com/sun/squawk/Suite.metadatas",
		"com/sun/squawk/Suite.type",
		"com/sun/squawk/KlassMetadata.definedClass",
		"com/sun/squawk/KlassMetadata.symbols",
		"com/sun/squawk/KlassMetadata.classTable",
		"com/sun/squawk/VM.STREAM_STDOUT",
		"com/sun/squawk/VM.STREAM_STDERR",
		"com/sun/squawk/VM.romStart",
		"com/sun/squawk/VM.romEnd",
		"com/sun/squawk/VM.bootstrapSuite",
		"com/sun/squawk/GC.GC_TRACING_SUPPORTED",
		"com/sun/squawk/GC.TRACE_ALLOCATION",
		"com/sun/squawk/GC.traceThreshold",
		"com/sun/squawk/CheneyCollector.copyingObjectGraph",
		"com/sun/squawk/CheneyCollector.forwardingRepairMap",
		"com/sun/squawk/CheneyCollector.forwardingRepairMapTop",
		"com/sun/squawk/CheneyCollector.IsolateKlass",
		"com/sun/squawk/CheneyCollector.theIsolate",
		"com/sun/squawk/CheneyCollector.ByteArrayKlass",
		"com/sun/squawk/CheneyCollector.oopMap",
		"com/sun/squawk/CheneyCollector.HashTableKlass",
		"com/sun/squawk/CheneyCollector.collectionTimings",
		"com/sun/squawk/CheneyCollector$Timings.setup",
		"com/sun/squawk/CheneyCollector$Timings.copyRoots",
		"com/sun/squawk/CheneyCollector$Timings.copyNonRoots",
		"com/sun/squawk/CheneyCollector$Timings.repair",
		"com/sun/squawk/CheneyCollector$Timings.finalize",
		"com/sun/squawk/GarbageCollector.references",
		"com/sun/squawk/GarbageCollector.numBytesLastScanned",
		"com/sun/squawk/Ref.next",
		"com/sun/squawk/Ref.referent",
		"com/sun/squawk/Ref.sink",
		"com/sun/squawk/Ref.self",
		"com/sun/squawk/Sink.head",
		"com/sun/squawk/ServiceOperation.EXTEND",
		"com/sun/squawk/ServiceOperation.THROW",
		"com/sun/squawk/ServiceOperation.NONE",
		"com/sun/squawk/ServiceOperation.CHANNELIO",
		"com/sun/squawk/ServiceOperation.GARBAGE_COLLECT",
		};
		/*
	static {
		try {
			readBuiltinNames();
		} catch (IOException e){
			//ignore
		}
	}
	*/
	static void readBuiltinNames() throws IOException {
		URL url = DependencyAnalyzer.class.getResource("builtin.txt");
		if (url == null) {
			return;
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
		String line = null;
		int section = -1;
		ArrayList<String> methods = new ArrayList<String>();
		ArrayList<String> fields = new ArrayList<String>();
		while ((line = br.readLine()) != null) {
			if (line.startsWith("#methods")) {
				section = 0; // methods
			} else if (line.startsWith("#fields")) {
				section = 1; // fields
			} else if (!line.isEmpty()) {
				if (section == 0) {
					methods.add(line);
				} else if (section == 1) {
					fields.add(line);
				}
			}
		}
		builtinMethods = methods.toArray(new String[methods.size()]);
		builtinFields = fields.toArray(new String[fields.size()]);
		System.out.println("builtinMethods:"+ builtinMethods.length);
		System.out.println("builtinFields:"+builtinFields.length);
	}
	
	DependencyAnalyzer(DependencyVisitor visitor) {
		classMap = visitor.classMap;
		methodMap = visitor.methodMap;
		fieldMap = visitor.fieldMap;

		classes = new HashSet<ClassNode>();
		methods = new HashSet<MethodNode>();
		fields = new HashSet<FieldNode>();
		systemRoot = new HashSet<ClassNode>();
	}

	ClassNode getClassNode(String name) {
		if (classMap.get(name) == null) {
			System.out.println(name + " not found.");
		}
		return classMap.get(name);
	}
	
	MethodNode getMethodNode(String name) {
//		if (methodMap.get(name) == null) {
//			System.out.println(name + " not found.");
//		}
		return methodMap.get(name);
	}
	
	FieldNode getFieldNode(String name) {
		if (fieldMap.get(name) == null) {
			System.out.println(name + " not found.");
		}
		return fieldMap.get(name);
	}


	void run(String mainClass) {
		MethodNode main = getMethodNode(mainClass.replace('.', '/') + ".main([Ljava/lang/String;)V");
		Set<MethodNode> newNodes = new HashSet<MethodNode>();
		if (main == null) {
			throw new RuntimeException("Main class (" + mainClass + ") is not found");
		}
		newNodes.add(main);
//		methods.add(main);

		systemRoot = new HashSet<ClassNode>();
		for (int i = 0; i < builtinClasses.length; i++) {
			systemRoot.add(getClassNode(builtinClasses[i]));
		}
		
		for (int i = 0; i < builtinMethods.length; i++) {
			MethodNode n = getMethodNode(builtinMethods[i]);
			if (n == null) {
				continue;
			}
			newNodes.add(n);
//			methods.add(n);
		}
		
		for (int i = 0; i < builtinFields.length; i++) {
			FieldNode fn = getFieldNode(builtinFields[i]);
			if (fn == null) {
//				System.out.println("### not found " + builtinFields[i]);
				continue;
			}
			addFieldNode(fn, fn.definingClass);
			addClassNode(fn.definingClass, newNodes);
		}

//		System.out.println("newNodes="+newNodes);
		while (true) {
			Set<MethodNode> next = analyze(methods, newNodes);
			if (next.isEmpty()) {
				break;
			}
			newNodes = next;
		}
		/*		
		System.out.println("classes: " + classes.size());
		for (ClassNode n : classes) {
			System.out.println(n);
		}

		System.out.println("methods: " + methods.size());
		for (MethodNode n : methods) {
			System.out.println(n);
		}
		
		System.out.println("fields: " + fields.size());
		for (FieldNode n : fields) {
			System.out.println(n);
		}
		*/
	}
	
	Set<MethodNode> analyze(Set<MethodNode> known, Set<MethodNode> newNodes) {
//		System.out.println("newNodes = "+ newNodes);
		
		Set<MethodNode> nextNodes = new HashSet<MethodNode>();
		for (MethodNode n : newNodes) {
//			System.out.println("analyzing " + n);
			known.add(n);
			addMethodDesc(n.desc, nextNodes);
			String[] exceptions = n.exceptions;
			if (exceptions != null) {
				for (int i = 0; i < n.exceptions.length; i++) {
					addClassNode(getClassNode(exceptions[i]), nextNodes);
				}
			}
			addClassNode(n.definingClass, nextNodes);
			for (MethodNode n2 : n.methodRef) {
				if (!known.contains(n2)) {
					nextNodes.add(n2);
					addDesc(n2.desc, nextNodes);
				}
			}
			for (FieldNode f : n.fieldRef) {
				addFieldNode(f, f.definingClass);
				addClassNode(f.definingClass, nextNodes);
				addDesc(f.desc, nextNodes);
			}

			for (ClassNode f : n.classRef) {
				addClassNode(f, nextNodes);
			}

			for (ClassNode c : classes) {
				if (n.definingClass.isAssignableFrom(c)) {
					for (MethodNode mn : c.methods) {
						if (n.name.equals(mn.name) && n.desc.equals(mn.desc)) {
							if (!methods.contains(mn)){
								nextNodes.add(mn);
							}
						}
					}
				}
			}
		}
		for (ClassNode c : classes) {
			collectMethodsInSuperTypes(c, nextNodes);
		}
		
		for (Map.Entry<String,MethodNode> e: methodMap.entrySet()) {
			MethodNode n = e.getValue();
			if (!classes.contains(n.definingClass)) {
				continue;
			}
			if (known.contains(n)) {
				continue;
			}
			if (n.exceptions != null) {
				for (int i = 0; i < n.exceptions.length; i++) {
					String ex = n.exceptions[i];
					if (ex.equals("com/sun/squawk/pragma/ReplacementConstructorPragma") ||
						ex.equals("com/sun/squawk/pragma/HostedPragma") ||
						ex.equals("com/sun/squawk/pragma/InterpreterInvokedPragma"))
					{
						nextNodes.add(n);
						break;
					}
				}
			}
			if (overrides(n)) {
				nextNodes.add(n);
			}
		}

		return nextNodes;
	}

	private void collectMethodsInSuperTypes(ClassNode n, Set<MethodNode> nextNodes) {
		Map<String,MethodNode> definedMethodNames = new HashMap<String,MethodNode>();
		for (MethodNode m : n.methods) {
			definedMethodNames.put(m.name + m.desc, m);
		}
		for (MethodNode m2 : methods) {
			ClassNode d = m2.definingClass;
			if (/*d.isAssignableFrom(n) ||*/ n.isAssignableFrom(d)) {
				MethodNode mn = definedMethodNames.get(m2.name + m2.desc);
				if (mn != null) {
					if (!methods.contains(mn)) {
						nextNodes.add(mn);
					}
				}
			}
		}
	}

	private boolean overrides(MethodNode m) {
		return overrides(m, m.definingClass);
	}
	
	private boolean overrides(MethodNode m, ClassNode cn) {
		for (MethodNode mn : cn.methods) {
			if (methods.contains(mn)) {
				if (mn.name.equals(m.name) && mn.desc.equals(m.desc)) {
					return true;
				}
			}
		}
		for (ClassNode c : cn.superTypes) {
			if (overrides(m, c)) return true;
		}
		return false;
	}
	
	
	private boolean addFieldNode(FieldNode n, ClassNode cn) {
		for (FieldNode f: cn.fields) {
			if (f.name.equals(n.name)) {
//				System.out.println("add field " + f);
				fields.add(f);
				return true;
			}
		}
		for (ClassNode s : cn.superTypes) {
			if (addFieldNode(n, s)) {
				return true;
			}
		}
		return false;
	}
	
    private void addClassNode(ClassNode n, Set<MethodNode> nextNodes) {
		if (classes.add(n)) {
//			System.out.println("addClassNode " + n);
			
			MethodNode mn = getMethodNode(n.name + ".<clinit>()V");
			if (mn != null) {
				if (!methods.contains(mn)) {
					nextNodes.add(mn);
				}
			}
			mn = getMethodNode(n.name + ".<init>()V");
			if (mn != null) {
				if (!methods.contains(mn)) {
					nextNodes.add(mn);
				}
			} else {
				for (MethodNode m: n.methods) {
					if (m.name.equals("<init>")) {
						nextNodes.add(m);
						break;
					}
				}
			}

			boolean isThread = false;
			for (ClassNode s : n.superTypes) {
				if (s.name == null) {
//					System.out.println("n="+n);
					continue;
				}
				if (s.name.equals("java/lang/Thread")) {
					isThread = true;
				}
			}
			
			for (MethodNode m: n.methods) {
				if (overrides(m)) {
					if (!methods.contains(m)) {
						nextNodes.add(m);
						continue;
					}
				}
				if (isThread) {
					if (m.name == "run" && m.desc.equals("()V")) {
						nextNodes.add(m);
					}
				}
			}

			Set<String> abstractMethods = new HashSet<String>();
			
			for (ClassNode s : n.superTypes) {
				for (MethodNode m : s.methods) {
					if (methods.contains(m)){
						if ((m.access & Opcodes.ACC_ABSTRACT) != 0) {
							abstractMethods.add(m.name + m.desc);
						}
					}
				}
				addClassNode(s, nextNodes);
			}
			
			for (ClassNode s : n.superTypes) {
				for (MethodNode m : s.methods) {
					if ((m.access & Opcodes.ACC_ABSTRACT) == 0) {
						if (abstractMethods.contains(m.name + m.desc)) {
							if (!methods.contains(m)){
								nextNodes.add(m);
							}
						}
					}
				}
			}
		}
		collectMethodsInSuperTypes(n, nextNodes);
	}

    private void addName(final String name, Set<MethodNode> nextNodes) {
		ClassNode n = getClassNode(name);
		addClassNode(n, nextNodes);
    }

    void addInternalName(final String name, Set<MethodNode> nextNodes) {
        addType(Type.getObjectType(name), nextNodes);
    }

    private void addInternalNames(final String[] names, Set<MethodNode> nextNodes) {
        for (int i = 0; names != null && i < names.length; i++) {
            addInternalName(names[i], nextNodes);
        }
    }

    void addDesc(final String desc, Set<MethodNode> nextNodes) {
        addType(Type.getType(desc), nextNodes);
    }

    void addMethodDesc(final String desc, Set<MethodNode> nextNodes) {
//		System.out.println("addMethodDesc  "  + desc);
        addType(Type.getReturnType(desc), nextNodes);
        Type[] types = Type.getArgumentTypes(desc);
        for (int i = 0; i < types.length; i++) {
            addType(types[i], nextNodes);
        }
    }

    void addType(final Type t, Set<MethodNode> nextNodes) {
        switch (t.getSort()) {
        case Type.ARRAY:
//			System.out.println("addType (array)  "  + t);
            addType(t.getElementType(), nextNodes);
            break;
        case Type.OBJECT:
            addName(t.getInternalName(), nextNodes);
            break;
        case Type.METHOD:
            addMethodDesc(t.getDescriptor(), nextNodes);
            break;
        }
    }

	boolean isUsedClass(String name) {
		ClassNode cn = getClassNode(name);
		if (systemRoot.contains(cn)) {
			return true;
		}
		return cn != null && classes.contains(cn);
	}
	
	boolean isUsedMethod(String name) {
		MethodNode mn = getMethodNode(name);
		if (systemRoot.contains(mn.definingClass)) {
			return true;
		}
		return mn != null && methods.contains(mn);
	}
	
	boolean isUsedField(String name) {
		FieldNode fn = getFieldNode(name);
		if (systemRoot.contains(fn.definingClass)) {
			return true;
		}
		return fn != null && fields.contains(fn);
	}
	
	static DependencyAnalyzer analyze(String mainClass, String[] args) throws IOException {
//		System.out.println("analyze "+ mainClass);
		DependencyVisitor v = new DependencyVisitor();
		new DependencyTracker(v).run(args);
//		v.dump();
		DependencyAnalyzer analyzer = new DependencyAnalyzer(v);
		analyzer.run(mainClass);
		return analyzer;
	}
		
	public static void main(String args[]) throws Exception {
		String[] classpaths = new String[args.length - 1];
		System.arraycopy(args, 1, classpaths, 0, args.length - 1);
		DependencyAnalyzer analyzer = analyze(args[0], classpaths);
//		System.out.println("#classes:");
//		for (String name : new TreeSet<String>(analyzer.classMap.keySet())) {
//			System.out.println(name);
//		}
		System.out.println("#methods:");
		for (String name : new TreeSet<String>(analyzer.methodMap.keySet())) {
			System.out.println(name);
		}
		System.out.println("#fields:");
		for (String name : new TreeSet<String>(analyzer.fieldMap.keySet())) {
			System.out.println(name);
		}
	}
}
