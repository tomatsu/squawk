package com.sun.squawk.builder.filter;

import java.util.zip.*;
import java.util.*;
import java.io.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.*;

public abstract class FieldFilter implements Opcodes {
    boolean verbose = false;
    
    static final byte[] stackGrowth = {
		0, 	// nop
		1,	// aconst_null
		1,	// iconst_m1
		1,	// iconst_0
		1,	// iconst_1
		1,	// iconst_2
		1,	// iconst_3
		1,	// iconst_4
		1,	// iconst_5
		2,	// lconst_0
		2,	// lconst_1
		1,	// fconst_0
		1,	// fconst_1
		1,	// fconst_2
		2,	// dconst_0
		2,	// dconst_1
		1,	// bipush
		1,	// sipush
		1,	// ldc
		1,	// ldc_W
		2,	// ldc2_W
		1,	// iload
		2,	// lload
		1,	// fload
		2,	// dload
		1,	// aload
		1,	// iload_0
		1,	// iload_1
		1,	// iload_2
		1,	// iload_3
		2,	// lload_0
		2,	// lload_1
		2,	// lload_2
		2,	// lload_3
		1,	// fload_0
		1,	// fload_1
		1,	// fload_2
		1,	// fload_3
		2,	// dload_0
		2,	// dload_1
		2,	// dload_2
		2,	// dload_3
		1,	// aload_0
		1,	// aload_1
		1,	// aload_2
		1,	// aload_3
		-1,	// iaload
		0,	// laload
		-1,	// faload
		0,	// daload
		-1,	// aaload
		-1,	// baload
		-1,	// caload
		-1,	// saload
		-1,	// istore
		-2,	// lstore
		-1,	// fstore
		-2,	// dstore
		-1,	// astore
		-1,	// istore_0
		-1,	// istore_1
		-1,	// istore_2
		-1,	// istore_3
		-2,	// lstore_0
		-2,	// lstore_1
		-2,	// lstore_2
		-2,	// lstore_3
		-1,	// fstore_0
		-1,	// fstore_1
		-1,	// fstore_2
		-1,	// fstore_3
		-2,	// dstore_0
		-2,	// dstore_1
		-2,	// dstore_2
		-2,	// dstore_3
		-1,	// astore_0
		-1,	// astore_1
		-1,	// astore_2
		-1,	// astore_3
		-3,	// iastore
		-4,	// lastore
		-3,	// fastore
		-4,	// dastore
		-3,	// aastore
		-3,	// bastore
		-3,	// castore
		-3,	// sastore
		-1,	// pop
		-2,	// pop2
		1,	// dup
		1,	// dup_X1
		1,	// dup_X2
		2,	// dup2
		2,	// dup2_X1
		2,	// dup2_X2
		0,	// swap
		-1,	// iadd
		-2,	// ladd
		-1,	// fadd
		-2,	// dadd
		-1,	// isub
		-2,	// lsub
		-1,	// fsub
		-2,	// dsub
		-1,	// imul
		-2,	// lmul
		-1,	// fmul
		-2,	// dmul
		-1,	// idiv
		-2,	// ldiv
		-1,	// fdiv
		-2,	// ddiv
		-1,	// irem
		-2,	// lrem
		-1,	// frem
		-2,	// drem
		0,	// ineg
		0,	// lneg
		0,	// fneg
		0,	// dneg
		-1,	// ishl
		-1,	// lshl
		-1,	// ishr
		-1,	// lshr
		-1,	// iushr
		-1,	// lushr
		-1,	// iand
		-2,	// land
		-1,	// ior
		-2,	// lor
		-1,	// ixor
		-2,	// lxor
		0,	// iinc
		1,	// i2l
		0,	// i2f
		1,	// i2d
		-1,	// l2i
		-1,	// l2f
		0,	// l2d
		0,	// f2i
		1,	// f2l
		1,	// f2d
		-1,	// d2i
		0,	// d2l
		-1,	// d2f
		0,	// i2b
		0,	// i2c
		0,	// i2s
		-3,	// lcmp
		-1,	// fcmpl
		-1,	// fcmpg
		-3,	// dcmpl
		-3,	// dcmpg
		-1,	// ifeq
		-1,	// ifne
		-1,	// iflt
		-1,	// ifge
		-1,	// ifgt
		-1,	// ifle
		-2,	// if_icmpeq
		-2,	// if_icmpne
		-2,	// if_icmplt
		-2,	// if_icmpge
		-2,	// if_icmpgt
		-2,	// if_icmple
		-2,	// if_acmpeq
		-2,	// if_acmpne
		0,	// goto
		0,	// jsr
		0,	// ret
		-1,	// tableswitch
		-1,	// lookupswitch
		-1,	// ireturn
		-2,	// lreturn
		-1,	// freturn
		-2,	// dreturn
		-1,	// areturn
		0,	// return
		0,	// getstatic
		0,	// putstatic
		-1,	// getfield
		-1,	// putfield
		-1,	// invokevirtual
		-1,	// invokespecial
		0,	// invokestatic
		-1,	// invokeinterface
		0,	// UNUSED
		1,	// new
		0,	// newarray
		0,	// anewarray
		0,	// arraylength
		-1,	// athrow
		0,	// checkcast
		0,	// instanceof
		-1,	// monitorenter
		-1,	// monitorexit
		0,	// wide
		1,	// multianewarray
		-1,	// ifnull
		-1,	// ifnonnull
		0,	// goto_w
		1,	// jsr_w
		0,	// breakpoint
		1,	// ldc_quick
		1,	// ldc_w_quick
		2,	// ldc2_w_quick
		0,	// getfield_quick
		0,	// putfield_quick
		0,	// getfield2_quick
		0,	// putfield2_quick
		0,	// getstatic_quick
		0,	// putstatic_quick
		0,	// getstatic2_quick
		0,	// putstatic2_quick
		0,	// invokevirtual_quick
		0,	// invokenonvirtual_quick
		0,	// invokesuper_quick
		0,	// invokestatic_quick
		0,	// invokeinterface_quick
		0,	// invokevirtualobject_quick
		0,	// UNUSED
		1,	// new_quick
		1,	// anewarray_quick
		1,	// multianewarray_quick
		-1,	// checkcast_quick
		0,	// instanceof_quick
		0,	// invokevirtual_quick_w
		0,	// getfield_quick_w
		0	// putfield_quick_w
    };
    
    static int stackGrowth(AbstractInsnNode insn) {
		int opc = insn.getOpcode();
		int growth = stackGrowth[opc];
		switch (opc) {
		case INVOKESTATIC:
		case INVOKEVIRTUAL:
		case INVOKESPECIAL:
		case INVOKEINTERFACE:
			MethodInsnNode m = (MethodInsnNode)insn;
			growth += parseMethodDesc(m.desc);
			break;
		case GETFIELD:
		case PUTFIELD:
		case GETSTATIC:
		case PUTSTATIC:
			FieldInsnNode f = (FieldInsnNode)insn;
			growth += parseFieldDesc(f.desc);
			break;
		}
		return growth;
    }

    static int parseFieldDesc(String desc) {
		int count = 0;
		boolean clazz = false;
		for (int i = 0; i < desc.length(); i++) {
			char c = desc.charAt(i);
			switch (c) {
			case 'L':
				clazz = true;
				break;
			case ';':
				clazz = false;
				count++;
				break;
			case '[':
			case 'I':
			case 'B':
			case 'S':
			case 'C':
			case 'F':
			case 'Z':
				if (!clazz) count++;
				break;
			case 'J':
			case 'D':
				if (!clazz) count += 2;
				break;
			case 'V':
				break;
			default:
				if (!clazz) {
					throw new RuntimeException("format error " + c);
				}
			}
		}
		return count;
    }
    
    static int parseMethodDesc(String desc) {
		int count = 0;
		boolean ret = false;
		boolean clazz = false;
		if (desc.charAt(0) != '(') {
			throw new RuntimeException("format error");
		}
		for (int i = 1; i < desc.length(); i++) {
			char c = desc.charAt(i);
			int size = 0;
			switch (c) {
			case ')':
				ret = true;
				break;
			case 'L':
				clazz = true;
				break;
			case ';':
				clazz = false;
				size = 1;
				break;
			case '[':
			case 'I':
			case 'B':
			case 'S':
			case 'C':
			case 'F':
			case 'Z':
				if (!clazz) size = 1;
				break;
			case 'J':
			case 'D':
				if (!clazz) size = 2;
				break;
			case 'V':
				if (!clazz) size = 0;
				break;
			default:
				if (!clazz) {
					throw new RuntimeException("format error " + c);
				}
			}
			if (!ret) {
				count -= size;
			} else {
				count += size;
			}
		}
		return count;
    }

    protected abstract boolean excludes(String name);
    
    /* eliminate unnecessary field write accesses */
    public void transformClassNode(ClassNode classNode) {
		List<MethodNode> m = classNode.methods;
        for (MethodNode methodNode : m) {
			InsnList instructions = methodNode.instructions;
			ListIterator<AbstractInsnNode> iter = instructions.iterator();
			while (iter.hasNext()) {
				AbstractInsnNode insn = iter.next();
				if (insn instanceof FieldInsnNode) {
					FieldInsnNode node = (FieldInsnNode)insn;
					if (excludes(node.owner + "." + node.name)) {
						if (verbose) {
							System.out.println("remove "+node.owner + "." + node.name + " from " + classNode.name + "." + methodNode.name + methodNode.desc);
						}
						boolean isStatic;
						int opcode = node.getOpcode();
						if (opcode == PUTSTATIC) {
							isStatic = true;
						} else if (opcode == PUTFIELD) {
							isStatic = false;
						} else if (opcode == GETSTATIC || opcode == GETFIELD) {
							throw new RuntimeException();
						} else {
							continue;
						}
						int idx = instructions.indexOf(node);
						int size = isStatic ? 1 : 2;
						int i = idx - 1;
						for (; i >= 0; i--) {
							AbstractInsnNode n = instructions.get(i);
							if (n instanceof MethodInsnNode) {
								MethodInsnNode mn = (MethodInsnNode)n;
								if (verbose) System.out.println("Assuming " + (mn.owner + "." + mn.name + mn.desc) + " is a pure function");
							}
							int opc = n.getOpcode();
							if (opc >= 0 && opc < stackGrowth.length) {
								size -= stackGrowth(n);
								if (size == 0) {
									break;
								}
							} else {
								// branch/branch target
								i = -1;
								break;
							}
						}
						if (i >= 0) {
							List<AbstractInsnNode> unused = new ArrayList<>();
							while (i <= idx) {
								unused.add(instructions.get(i));
								i++;
							}
							for (AbstractInsnNode n : unused) {
								instructions.remove(n);
							}
						} else {
							if (isStatic) {
								instructions.insert(node, new InsnNode(POP));
							} else {
								instructions.insert(node, new InsnNode(POP2));
							}
							instructions.remove(node);
						}
					}
				}
			}
		}
    }
}
