package com.sun.squawk.builder.glue;

import java.io.*;
import java.util.*;
import org.objectweb.asm.*;

public class NativeGlueGen implements Opcodes {
	private final static int BYTE = Type.BYTE_ID;
	private final static int CHAR = Type.CHAR_ID;
	private final static int SHORT = Type.SHORT_ID;
	private final static int INT = Type.INT_ID;
	private final static int LONG = Type.LONG_ID;
	private final static int BOOLEAN = Type.BOOLEAN_ID;
	private final static int FLOAT = Type.FLOAT_ID;
	private final static int DOUBLE = Type.DOUBLE_ID;
	
	private final static int ADDRESS = 8;
	private final static int WORD = 9;
	private final static int OFFSET = 10;
	private final static int KLASS = 11;
	private final static int OBJECT = 12;
	private final static int VOID = 13;

	static String outputDir = ".";
	
	private static String[] builtinClasses  =	{
		"com/sun/squawk/Address",
		"com/sun/squawk/UWord",
		"com/sun/squawk/Offset",
		"com/sun/squawk/NativeUnsafe",
		"com/sun/squawk/VM",
		"com/sun/squawk/CheneyCollector",
		"com/sun/squawk/ServiceOperation",
		"com/sun/squawk/GarbageCollector",
		"com/sun/squawk/Lisp2Bitmap",
		"java/lang/System"
	};
	private static Set<String> builtinClassSet = new HashSet<String>();
	static 	{
		for (int i = 0; i < builtinClasses.length; i++) {
			builtinClassSet.add(builtinClasses[i]);
		}
	}
	
	static String constantName(String className, String methodName) {
		return className.replace('/', '_') + "_" + mangle(methodName);
	}

	static String funcName(String className, String methodName) {
		return "Java_" + constantName(className, methodName);
	}

	static class Sig {
		List<Type> parameterTypes;
		Type returnType;
	}


	static int getCType(Type t) {
		if (t == Type.INT ||
			t == Type.BYTE ||
			t == Type.CHAR ||
			t == Type.SHORT ||
			t == Type.BOOLEAN ||
			t == Type.FLOAT)
			{
				return WORD;
			}
		else if (t == Type.LONG ||
				 t == Type.DOUBLE)
			{
				return LONG;
			}
		else if (t == Type.VOID)
			{
				return VOID;
			}
		else if (t instanceof ReferenceType)
			{
				return ADDRESS;
			}
		else if (t instanceof ArrayType)
			{
				return ADDRESS;
			}
		else
			{
				throw new RuntimeException(String.valueOf(t));
			}
	}

	static String getTypeName(Type type) {
		if (type instanceof ReferenceType) {
			String name = type.getName();
			if (name.equals("com/sun/squawk/Address")) {
				return "REF";
			}
			if (name.equals("com/sun/squawk/Offset")) {
				return "WORD";
			}
			if (name.equals("com/sun/squawk/Word") || name.equals("com/sun/squawk/UWord")) {
				return "UWORD";
			}
			if (name.equals("com/sun/squawk/Klass")) {
				return "KLASS";
			}
			return "OOP";
		}
		if (type instanceof ArrayType) {
			return "REF";
		}
		return type.getName();
	}

	static void parseDesc(Sig sig, String desc) throws IOException {
		int i = 0;
		int j = 0;
		char c = desc.charAt(i);
		if (c != '(') {
			throw new RuntimeException();
		}
		List<Type> types = new ArrayList<Type>();
		Type returnType;
		c = desc.charAt(++i);
		if (c != ')') {
			int idx = desc.indexOf(')', i);
			if (idx < 0) {
				throw new RuntimeException();
			}
			CharArrayReader cr = new CharArrayReader(desc.substring(i, idx).toCharArray());
			Type t;
			while ((t = Type.parseType(cr)) != null) {
				types.add(t);
			}
			i = idx;
		}
		i++;
		CharArrayReader cr = new CharArrayReader(desc.substring(i).toCharArray());
		returnType = Type.parseType(cr);
		sig.parameterTypes = types;
		sig.returnType = returnType;
	}
	
	public static void generateGlue(PrintStream w, PrintStream p, String className, String methodName, String desc) throws IOException {
		Sig sig = new Sig();
		parseDesc(sig, desc);
		w.print("CASE_(");
		w.print(constantName(className, methodName));
		w.println(", {\\");
		
		switch (getCType(sig.returnType)) {
		case VOID:
			p.print("extern void "); break;
		case WORD:
			p.print("extern int "); break;
		case LONG:
			p.print("extern jlong "); break;
		case ADDRESS:
			p.print("extern Address "); break;
		default:
			throw new RuntimeException();
		}
		p.print(funcName(className, methodName));
		p.print("(");
		List<Type> parameterTypes = sig.parameterTypes;
		if (parameterTypes.size() > 0) {
			int t = getCType(parameterTypes.get(0));
			switch (t) {
			case WORD:
				p.print("int"); break;
			case LONG:
				p.print("jlong"); break;
			case ADDRESS:
				p.print("Address"); break;
			}
		}
		for (int i = 1; i < parameterTypes.size(); i++) {
			int type = getCType(parameterTypes.get(i));
			switch (type) {
			case WORD:
				p.print(", int"); break;
			case LONG:
				p.print(", jlong"); break;
			case ADDRESS:
				p.print(", Address");
			}
		}
		p.println(");");
		int count = parameterTypes.size() - 1;
		for (int i = 0; i < parameterTypes.size(); i++) {
			int t = getCType(parameterTypes.get(parameterTypes.size() - 1 - i));
			switch (t) {
			case WORD:
				w.println("\tint v" + count-- + " = popInt();\\"); break;
			case LONG:
				w.println("\tjlong v" + count-- + " = popLong();\\"); break;
			case ADDRESS:
				w.println("\tAddress v" + count-- + " = popAddress();\\");
			}
		}
		int returnType = getCType(sig.returnType);
		if (returnType == VOID) {
			w.print("\t");				
			w.print(funcName(className, methodName));
			w.print("(");
			if (sig.parameterTypes.size() > 0) {
				w.print("v0");
			}
			for (int i = 1; i < parameterTypes.size(); i++) {
				w.print(", v" + i);
			}
			w.println("); break;\\");
		} else if (returnType == WORD) {
			w.print("\tpushInt(");
			w.print(funcName(className, methodName));
			w.print("(");
			if (sig.parameterTypes.size() > 0) {
				w.print("v0");
			}
			for (int i = 1; i < sig.parameterTypes.size(); i++) {
				w.print(", v" + i);
			}
			w.println(")); break;\\");
		} else if (returnType == LONG) {
			w.print("\tpushLong(");
			w.print(funcName(className, methodName));
			w.print("(");
			if (sig.parameterTypes.size() > 0) {
				w.print("v0");
			}
			for (int i = 1; i < sig.parameterTypes.size(); i++) {
				w.print(", v" + i);
			}
			w.println(")); break;\\");
		} else if (returnType == ADDRESS) {
			w.print("\tpushAddress(");
			w.print(funcName(className, methodName));
			w.print("(");
			if (sig.parameterTypes.size() > 0) {
				w.print("v0");
			}
			for (int i = 1; i < sig.parameterTypes.size(); i++) {
				w.print(", v" + i);
			}
			w.println(")); break;\\");
		} else {
			throw new RuntimeException();
		}
		w.println("})\\");
	}

	static class MethodInfo implements Comparable<MethodInfo> {
		String name;
		String desc;
		boolean isStatic;
		public int compareTo(MethodInfo o) {
			return name.compareTo(o.name);
		}
	}
	static Map<String,Set<MethodInfo>> nativeMethodMap = new HashMap<String,Set<MethodInfo>>();

	static void loadClasses(String[] args) throws IOException {
		NativeMethodFinder v = new NativeMethodFinder(new NativeMethodFinder.Handler() {
				public void callback(String className, String methodName, String desc, boolean isStatic) {
					Set<MethodInfo> s = nativeMethodMap.get(className);
					if (s == null) {
						nativeMethodMap.put(className, s = new TreeSet<MethodInfo>());
					}
					MethodInfo mi = new MethodInfo();
					mi.name = methodName;
					mi.desc = desc;
					mi.isStatic = isStatic;
					s.add(mi);
				}
			});
		v.run(args);
	}

	static File getClassFilePath(String filename) {
		File dir;
		if (filename.startsWith("com/sun/squawk/translator")) {
			dir = new File(new File(outputDir), "translator/classes");
		} else {
			dir = new File(new File(outputDir), "cldc/classes");
		}
		File f = new File(dir, filename);
		f.getParentFile().mkdirs();
		return f;
	}
	
	static File getJavaFilePath(String filename) {
		File dir;
		if (filename.startsWith("com/sun/squawk/translator")) {
			dir = new File(new File(outputDir), "translator/src");
		} else {
			dir = new File(new File(outputDir), "cldc/src");
		}
		File f = new File(dir, filename);
		f.getParentFile().mkdirs();
		return f;
	}
	
	static File getCFilePath(String filename) {
		File dir = new File(new File(outputDir), "vmcore/src/vm");
		dir.mkdirs();
		return new File(dir, filename);
	}
	
	static void generateCFiles() throws IOException {
		PrintStream p1 = new PrintStream(new FileOutputStream(getCFilePath("native.c.inc")));
		PrintStream p2 = new PrintStream(new FileOutputStream(getCFilePath("native.h")));
		p1.println("/* DO NOT EDIT THIS FILE */");
		p2.println("/* DO NOT EDIT THIS FILE */");
		p2.println("#ifdef __cplusplus");
		p2.println("extern \"C\" {");
		p2.println("#endif");

		p1.println("#include \"native.h\"");
		p1.println("#define CUSTOM_NATIVE_CODE \\");
		for (Map.Entry<String,Set<MethodInfo>> entry : nativeMethodMap.entrySet()) {
			String className = entry.getKey();
			Set<MethodInfo> methods = entry.getValue();
			if (!builtinClassSet.contains(className)) {
				for (MethodInfo mi : methods) {
					generateGlue(p1, p2, className, mi.name, mi.desc);
				}
			}
		}
		p1.println();
		p1.close();

		p2.println("#ifdef __cplusplus");
		p2.println("};");
		p2.println("#endif");
		p2.close();
	}

	/*
	 * public final static String <fieldName> = <value>;
	 */
	static void defineStringConstant(ClassWriter cw, String fieldName, String value) {
		cw.visitField(ACC_PUBLIC|ACC_FINAL|ACC_STATIC, fieldName, "Ljava/lang/String;", null, value);
	}
	
	/*
	 * public final static int <fieldName> = <value>;
	 */
	static void defineIntConstant(ClassWriter cw, String fieldName, int value) {
		cw.visitField(ACC_PUBLIC|ACC_FINAL|ACC_STATIC, fieldName, "I", null, value);
	}

	/*
	 * public static int <fieldName> = <value>;
	 */
	static void defineInt(ClassWriter cw, String fieldName, int value) {
		cw.visitField(ACC_PUBLIC|ACC_STATIC, fieldName, "I", null, value);
	}

	static String mangle(String name) {
		StringBuilder sbuf = new StringBuilder();
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (c == '_') {
				sbuf.append("_1");
			} else {
				sbuf.append(c);
			}
		}
		return sbuf.toString();
	}
	
	static void generateJavaConstants() throws IOException {
		ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_1, ACC_PUBLIC|ACC_FINAL, "com/sun/squawk/vm/Native", null, "java/lang/Object", null);
        MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mw.visitVarInsn(ALOAD, 0);
        mw.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mw.visitInsn(RETURN);
        mw.visitMaxs(1, 1);
        mw.visitEnd();
					   
		int id = 0;
		for (int i = 0; i < builtinClasses.length; i++) {
			String className = builtinClasses[i];
			Set<MethodInfo> methods = nativeMethodMap.get(className);
			if (methods != null) {
				for (MethodInfo mi : methods) {
					defineIntConstant(cw, className.replace('/', '_') + "$" + mangle(mi.name), id++);
				}
			}
		}
		defineIntConstant(cw, "com_sun_squawk_VM$lcmp", id++);
		
		for (String className : new TreeSet<String>(nativeMethodMap.keySet())) {
			Set<MethodInfo> methods = nativeMethodMap.get(className);
			if (!builtinClassSet.contains(className)) {
				for (MethodInfo mi : methods) {
					defineIntConstant(cw, className.replace('/', '_') + "$" + mangle(mi.name), id++);
				}
			}
		}
		defineInt(cw, "ENTRY_COUNT", id);

		List<String> list = new ArrayList<String>();
		for (int i = 0; i < 2; i++) {
			String className = builtinClasses[i];
			Set<MethodInfo> methods = nativeMethodMap.get(className);
			for (MethodInfo mi : methods) {
				list.add(className.replace('/', '.') + "." + mangle(mi.name));
			}
		}
		defineLinkableNativeMethodTable(cw, list);
		
        byte[] b = cw.toByteArray();
		FileOutputStream fout = new FileOutputStream(getClassFilePath("com/sun/squawk/vm/Native.class"));
		fout.write(b);
		fout.close();
	}

    static void defineLinkableNativeMethodTable(ClassWriter cw, List<String> methods) {
		StringBuilder out = new StringBuilder();
        String previous = "";
        int id = 0;
        for (String method: methods) {
            int substring = 0;
            while (previous.regionMatches(false, 0, method, 0, substring + 1)) {
                substring++;
            }

            // Convert length to character value relative to '0'
            char charValue = (char)(substring + '0');
            String charValueAsString;
            if (charValue <= '~') {
                // Printable ASCII constant
                charValueAsString = "" + charValue;
            } else {
                // Unicode constant
                charValueAsString = Integer.toHexString(charValue);
                while (charValueAsString.length() < 4) {
                    charValueAsString = "0" + charValueAsString;
                }
                charValueAsString = "\\\\u" + charValueAsString;
            }
			
            out.append(charValueAsString + method.substring(substring) + " ");
            previous = method;
            ++id;
        }
		defineStringConstant(cw, "LINKABLE_NATIVE_METHODS", out.toString());
    }

	static void generateJavaVerifierHelper() throws IOException {
		PrintStream p = new PrintStream(new FileOutputStream(getJavaFilePath("com/sun/squawk/translator/ir/verifier/NativeVerifierHelper.java")));
		p.println("//if[SUITE_VERIFIER]");
        p.println("/* **DO NOT EDIT THIS FILE** */");
		p.println("package com.sun.squawk.translator.ir.verifier;");
		p.println("import com.sun.squawk.*;");
		p.println("import com.sun.squawk.vm.Native;");
		p.println("import com.sun.squawk.util.Assert;");
		p.println();
		p.println("class NativeVerifierHelper {");
		p.println("    private static final Klass INT = Klass.INT,");
		p.println("                               SHORT = Klass.SHORT,");
		p.println("                               CHAR = Klass.CHAR,");
		p.println("                               BYTE = Klass.BYTE,");
		p.println("                               BOOLEAN = Klass.BOOLEAN,");
		p.println("/*if[FLOATS]*/");
		p.println("                               FLOAT = Klass.FLOAT,");
		p.println("                               DOUBLE = Klass.DOUBLE,");
		p.println("/*end[FLOATS]*/");
		p.println("                               WORD = Klass.OFFSET,");
		p.println("                               UWORD = Klass.UWORD,");
		p.println("                               REF = Klass.ADDRESS,");
		p.println("                               LONG = Klass.LONG,");
		p.println("                               KLASS = Klass.KLASS,");
		p.println("                               OOP = Klass.OBJECT;");
		p.println();
		p.println("    static void do_invokenative(Frame frame, int index) {");
		p.println("        switch (index) {");

		for (String className : new TreeSet<String>(nativeMethodMap.keySet())) {
			Set<MethodInfo> methods = nativeMethodMap.get(className);
			for (MethodInfo mi : methods) {
				String symbol = className.replace('/', '_') + "$" + mi.name;
				p.println("        case Native." + symbol + ": {");
					
				Sig sig = new Sig();
				parseDesc(sig, mi.desc);
				List<Type> parameterTypes = sig.parameterTypes;
				for (Type t : parameterTypes) {
					p.println("            frame.pop(" + getTypeName(t) + "); ");
				}

				if (!mi.isStatic) {
					p.println("            frame.pop(" + getTypeName(new ReferenceType(className)) + ");  ");
				}
				p.println("            Assert.that(frame.isStackEmpty());");
				if (sig.returnType != Type.VOID) {
					p.println("            frame.push(" + getTypeName(sig.returnType) + "); ");
				}
				p.println("            return;");
				p.println("        }");
				p.println();
			}
		}
		p.println("        }");
		p.println("        Assert.that(false, \"native method with index \" + index + \" was not found\");");
		p.println("    }");
        p.println("}");
		p.close();
	}
	
	static void generate() throws IOException {
		generateCFiles();
		generateJavaConstants();
		// generateJavaVerifierHelper();
	}
	
	public static void main(String[] args) throws Exception {
		List<String> classpaths = new ArrayList<String>();
		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith("-d:")){
				outputDir = args[i].substring(3);
			} else {
				classpaths.add(args[i]);
			}
		}
		loadClasses(classpaths.toArray(new String[0]));
		generate();
	}
}
