package com.sun.squawk.builder.glue;

import java.io.*;

class Type {
	final static int BYTE_ID = 0;
	final static int CHAR_ID = 1;
	final static int SHORT_ID = 2;
	final static int INT_ID = 3;
	final static int LONG_ID = 4;
	final static int BOOLEAN_ID = 5;
	final static int FLOAT_ID = 6;
	final static int DOUBLE_ID = 7;
	
	final static String[] typeNames = {
		"BYTE", "CHAR", "SHORT", "INT", "LONG", "BOOLEAN", "FLOAT", "DOUBLE"
	};
	public static Type INT = new PrimitiveType(INT_ID);
	public static Type BYTE = new PrimitiveType(BYTE_ID);
	public static Type SHORT = new PrimitiveType(SHORT_ID);
	public static Type CHAR = new PrimitiveType(CHAR_ID);
	public static Type BOOLEAN = new PrimitiveType(BOOLEAN_ID);
	public static Type LONG = new PrimitiveType(LONG_ID);
	public static Type FLOAT = new PrimitiveType(FLOAT_ID);
	public static Type DOUBLE = new PrimitiveType(DOUBLE_ID);
	public static Type VOID = new Type();

	public String getName() {
		throw new RuntimeException();
	}

	public String toString() {
		return getName();
	}


	static Type getType(int c) {
		switch (c) {
		case 'I':
			return Type.INT;
		case 'C':
			return Type.CHAR;
		case 'S':
			return Type.SHORT;
		case 'B':
			return Type.BYTE;
		case 'Z':
			return Type.BOOLEAN;
		case 'J':
			return Type.LONG;
		case 'F':
			return Type.FLOAT;
		case 'D':
			return Type.DOUBLE;
		case 'V':
			return Type.VOID;
		default:
			throw new RuntimeException(String.valueOf(c));
		}
	}
	
	static Type parseType(CharArrayReader cr) throws IOException {
		int dimension = 0;
		int c = cr.read();
		if (c == -1) {
			return null;
		}
		if (c == '[') {
			do {
				c = cr.read();
				if (c == -1){
					throw new RuntimeException();
				}
				dimension++;
			} while (c == '[');
		}
		if (c == 'L') {
			StringBuilder b = new StringBuilder();
			while (true) {
				c = cr.read();
				if (c == -1) {
					throw new RuntimeException();
				}
				if (c == ';') {
					break;
				}
				b.append((char)c);
			}

			Type ref = new ReferenceType(b.toString());
			if (dimension > 0) {
				return new ArrayType(ref, dimension);
			} else {
				return ref;
			}
		} else {
			if (dimension > 0) {
				return new ArrayType(Type.getType(c), dimension);
			} else {
				return Type.getType(c);
			}
		}
	}
}

class PrimitiveType extends Type {
	int type;
	PrimitiveType(int type) {
		this.type = type;
	}
	public String getName() {
		return typeNames[type];
	}
}

class ReferenceType extends Type {
	String className;
	ReferenceType(String className){
		this.className = className;
	}
	public String getName() {
		return className;
	}
		
}

class ArrayType extends Type {
	int dimension;
	Type componentType;
	ArrayType(Type componentType, int dimension) {
		this.componentType = componentType;
		this.dimension = dimension;
	}
	public String getName() {
		return "<array>";
	}
}
	
