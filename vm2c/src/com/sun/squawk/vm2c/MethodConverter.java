/*
 * Copyright 2005-2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * only, as published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included in the LICENSE file that accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 16 Network Circle, Menlo
 * Park, CA 94025 or visit www.sun.com if you need additional
 * information or have any questions.
 */

package com.sun.squawk.vm2c;

import java.util.*;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.List;
import java.util.regex.*;

/**
 * Converts a Java method to C functions.
 *
 */
public class MethodConverter extends Tree.Visitor {

    /**
     * The method being converted.
     */
    private final ProcessedMethod method;

    /**
     * A hashtable mapping the first statement in a block to the set of variable
     * declarations local to the block.
     */
    private final Map<Tree, List<VarSymbol>> blockLocalDecls;

    /**
     * The conversion context.
     */
    private final Converter conv;

    /**
     * The buffer for the generated C functions.
     */
    private CCodeBuffer ccode;

    /**
     * Resolves character positions to source file line numbers.
     */
    private final LineNumberTable lnt;

    /**
     * Creates the converter for a given Java compilation unit (i.e. a Java source file).
     *
     * @param conv  the context in which multiple compilation units are being converted
     */
    public MethodConverter(ProcessedMethod method, Converter conv, LineNumberTable lnt) {
        this.method = method;
        this.conv = conv;
        this.lnt = lnt;
        this.blockLocalDecls = new LocalVarDeclScanner().run(method.tree);
        ccode = new CCodeBuffer();
    }

    /**
     * Converts the method to a C function.
     *
     * @return the C function definition
     * @throws InconvertibleNodeException if there was a construct in the method that could not be converted
     */
    public String convert() {
        visitMethodDef(method.tree);
        return ccode.toString();
    }

    static void inconvertible(Tree inconvertibleNode, String desc) {
        String message = "converter cannot handle " + desc;
        throw new InconvertibleNodeException(inconvertibleNode, message);
    }

    /**************************************************************************
     * Traversal methods
     *************************************************************************/

    /** Visitor argument: the current precedence level.
     */
    int parentsPrec = TreeInfo.notExpression;
    int prec = TreeInfo.notExpression;

    /**
     * Visitor argument: denotes if the current expression is an lvalue or rvalue
     */
    boolean lvalue;

    /** Visitor method: traverse expression tree.
     *  @param prec  The current precedence level.
     */
    public void doExpr(Tree tree, int prec, boolean lvalue) {
        int prevParentsPrec = this.parentsPrec;
        int prevPrec = this.prec;
        boolean prevLvalue = this.lvalue;
        try {
            this.parentsPrec = this.prec;
            this.prec = prec;
            this.lvalue = lvalue;
            if (tree == null)
                ccode.print("/*missing*/");
            else {
                tree.accept(this);
            }
        } catch (Error e) {
            // This is only for debugging the converter
            conv.log.rawError(tree.pos, e.toString());
            e.printStackTrace();
            System.exit(1);
        } catch (RuntimeException e) {
            if (e instanceof InconvertibleNodeException) {
                throw e;
            }
            // This is only for debugging the converter
            conv.log.rawError(tree.pos, e.toString());
            e.printStackTrace();
            System.exit(1);
        } finally {
            this.parentsPrec = prevParentsPrec;
            this.prec = prevPrec;
            this.lvalue = prevLvalue;
        }
    }

    public void doExpr(Tree tree, int prec) {
        doExpr(tree, prec, false);
    }

    public String exprToString(Tree tree, int prec, boolean lvalue) {
        ccode = ccode.enter();
        doExpr(tree, prec, lvalue);
        String value = ccode.toString();
        ccode = ccode.leave();
        return value;
    }

    /** Derived visitor method: print expression tree at minimum precedence level
     *  for expression.
     */
    public void doExpr(Tree tree) {
        doExpr(tree, TreeInfo.notExpression, false);
    }

    /** Derived visitor method: print statement tree.
     */
    public void doStatement(Tree tree) {
        doExpr(tree, TreeInfo.notExpression, false);
    }

    /** Derived visitor method: print list of expression trees, separated by given string.
     *  @param sep the separator string
     */
    public <T extends Tree> void doExprs(List<T> trees, String sep) {
        if (trees.nonEmpty()) {
            doExpr(trees.head);
            for (List<T> l = trees.tail; l.nonEmpty(); l = l.tail) {
                ccode.print(sep);
                doExpr(l.head);
            }
        }
    }

    /** Derived visitor method: print list of expression trees, separated by commas.
     */
    public <T extends Tree> void doExprs(List<T> trees) {
        doExprs(trees, ", ");
    }

    /** Derived visitor method: print list of statements, each on a separate line.
     */
    public void doStatements(List<? extends Tree> trees, String sep) {
        for (List<? extends Tree> l = trees; l.nonEmpty(); l = l.tail) {
            ccode.align();
            doStatement(l.head);
            ccode.println();
        }
    }

    /** Print a block.
     */
    public void doBlock(List<? extends Tree> stats) {
        ccode.println("{");
        ccode.indent();

        if (!stats.isEmpty()) {
            List<VarSymbol> symbols = blockLocalDecls.get(stats.head);
            if (symbols != null) {
                for (VarSymbol sym : symbols) {
                    ccode.align();
                    printVarDecl(sym);
                    ccode.print(";");
                    ccode.println();
                }
            }
        }

        doStatements(stats, CCodeBuffer.LINE_SEP);
        ccode.undent();
        ccode.align();
        ccode.print("}");
    }

    public void printVarDecl(VarSymbol var) {
        ccode.print(conv.asString(var.type));
        ccode.print(" " + conv.subVarName(var.name));
    }


    /**************************************************************************
     * Visitor methods
     *************************************************************************/

    public void visitClassDef(Tree.ClassDef tree) {
        inconvertible(tree, "method-local class definition");
    }

    private boolean isVirtual(MethodSymbol method) {
        return !method.isStatic() &&
            (method.flags() & Flags.FINAL) == 0 &&
            (method.enclClass().flags() & Flags.FINAL) == 0;
    }

    private final static int NEVER_INLINE = -1;
    private final static int MAY_INLINE = 0;
    private final static int MUST_INLINE = 1;

    public void visitMethodDef(Tree.MethodDef tree) {

        Map<String, String> annotations = new AnnotationParser().parse(method);
        String cRoot = annotations.get("root");
        boolean isRoot = cRoot != null;
        MethodSymbol method = this.method.sym;
        assert tree.sym == method;
        List<ProcessedMethod> impls = conv.getImplementersOf(method);

		List<Tree> thrown = tree.thrown;
		int shouldInline = MAY_INLINE;

		if (thrown != null) {
		    Iterator<Tree> iter = thrown.iterator();
			while (iter.hasNext()) {
				Tree.Ident t = (Tree.Ident)iter.next();
				String name = t.name.toString();
				if (name.equals("ForceInlinedPragma") ||
                    name.equals("AllowInlinedPragma") ||
					name.equals("NativePragma")) {
					shouldInline = MUST_INLINE;
					// System.out.println("Auto inlining " + method);
					break;
				} else if (name.equals("NotInlinedPragma")) {
					shouldInline = NEVER_INLINE;
					// System.out.println("Auto inlining " + method);
					break;
				}
			}
		}

        String code = annotations.get("code");
        String proxy = annotations.get("proxy");
        String macro = annotations.get("macro");
        if (code == null && proxy == null && macro == null && impls.isEmpty()) {
            if (tree.name == tree.name.table.init) {
                inconvertible(tree, "constructor");
            }

            if (tree.body == null) {
                inconvertible(tree, "method with no body");
            }

            if (isVirtual(method)) {
                inconvertible(tree, "virtual method");
            }
        }

        ccode.align();
        ccode.print(AnnotationParser.getDocComment(this.method.unit, tree, ccode.lmargin));

        if (macro != null) {
            ccode.print("#define ");
            ccode.print(" " + conv.asString(method));
        } else {
            if (shouldInline == NEVER_INLINE) {
				ccode.print("NOINLINE ");
            } else if (proxy != null || shouldInline == MUST_INLINE) {
				ccode.print("INLINE ");
			} else {
				ccode.print("static ");
			}
            ccode.print(conv.asString(tree.sym.type.restype()));
            ccode.print(" " + (isRoot ? cRoot : conv.asString(method)));
        }
        if (conv.asString(method).equals("GC_isTracing_I")) {
        	int a = 1;
        }
        ccode.print("(");

        ccode.print(conv.getReceiverDecl(tree.sym, false));
        if (macro != null) {
            int params = method.params().size();
            for (VarSymbol param : method.params()) {
                ccode.print(" " + conv.subVarName(param.name));
                if (--params != 0) {
                    ccode.print(", ");
                }
            }
        } else {
            doExprs(tree.params);
        }
        ccode.print(") ");

        if (!impls.isEmpty()) {
            this.dispatchAbstractMethod(tree, impls);
        } else if (proxy != null) {
            ccode.println("{");
            ccode.indent();
            StringBuilder buf = new StringBuilder();
            if (tree.sym.type.restype().tag != TypeTags.VOID) {
                buf.append("return ");
            }
            buf.append(proxy.length() == 0 ? method.name.toString() : proxy);
            buf.append(makePassThroughInvocation(tree)).append(';');
            ccode.aprintln(buf);
            ccode.undent();
            ccode.aprintln("}");
        } else if (code != null) {
            // A sanity check as some C compilers quietly accept non-void functions without a return statement!
            boolean hasReturn = false;
            String[] idents = code.split("\\W+");
            for (String ident: idents) {
                if (ident.equals("return")) {
                    hasReturn = true;
                    break;
                }
            }
            if (method.type.restype().tag != TypeTags.VOID && !hasReturn) {
                throw new InconvertibleNodeException(tree, "code annotation for non-void function does not include a return statement");
            }
            ccode.printFunctionBody(code);
        } else if (macro != null) {
            ccode.printMacroBody(macro);
        } else {
            doStatement(tree.body);
        }
    }

    private String makePassThroughInvocation(Tree.MethodDef tree) {
        StringBuilder buf = new StringBuilder();
        buf.append('(');

        int params = tree.params.size();
        if (!tree.sym.isStatic()) {
            buf.append("this");
            if (params != 0) {
                buf.append(", ");
            }
        }
        for (Tree.VarDef var: tree.params) {
            buf.append(var.name.toString());
            if (--params != 0) {
                buf.append(", ");
            }
        }
        return buf.append(")").toString();
    }

    private void dispatchAbstractMethod(Tree.MethodDef tree, List<ProcessedMethod> impls) {

        String invocation = makePassThroughInvocation(tree);
        String ret = tree.sym.type.tag != TypeTags.VOID ? "return " : "";

        ccode.println("{");
        ccode.indent();
        ccode.aprintln("Address klass = getClass(this);");
        ccode.aprintln("int id = com_sun_squawk_Klass_id(klass);");
        ccode.aprintln("int suiteID = id >= 0 ? id : -(id + 1);");
        ccode.aprintln("switch (suiteID) {");
        ccode.indent();
        for (ProcessedMethod impl: impls) {
            ccode.aprintln("case " + conv.asString(impl.sym.enclClass()) + ": " + ret + conv.asString(impl.sym) + invocation + ";");
        }
        ccode.aprintln("default: fatalVMError(\"bad abstract method dispatch\"); ");
        ccode.undent();
        ccode.aprintln("}");
        ccode.undent();
        ccode.aprintln("}");
    }

    public void visitVarDef(Tree.VarDef tree) {
        if (tree.sym.isLocal()) {
            // A parameter
            if ((tree.sym.flags() & Flags.PARAMETER) != 0) {
                printVarDecl(tree.sym);
            } else {
                // A local variable
                if (tree.init != null) {
                    ccode.print(conv.subVarName(tree.name));
                    ccode.print(" = ");
                    doExpr(tree.init);
                    if (prec == TreeInfo.notExpression && !inForInitOrStep) {
                        ccode.print(";");
                    }
                }
            }
        }
    }

    public void visitSkip(Tree.Skip tree) {
        ccode.print(";");
    }

    public void visitBlock(Tree.Block tree) {
        if ((tree.flags & Flags.STATIC) != 0) {
            // This is a static initialization block
            return;
        }
        doBlock(tree.stats);
    }

    public void visitDoLoop(Tree.DoLoop tree) {
        ccode.print("do ");
        doStatement(tree.body);
        ccode.align();
        ccode.print(" while ");
        if (tree.cond.tag == Tree.PARENS) {
            doExpr(tree.cond);
        } else {
            ccode.print("(");
            doExpr(tree.cond);
            ccode.print(")");
        }
        ccode.print(";");
    }

    public void visitWhileLoop(Tree.WhileLoop tree) {
        ccode.print("while ");
        if (tree.cond.tag == Tree.PARENS) {
            doExpr(tree.cond);
        } else {
            ccode.print("(");
            doExpr(tree.cond);
            ccode.print(")");
        }
        ccode.print(" ");
        doStatement(tree.body);
    }

    boolean inForInitOrStep;

    public void visitForLoop(Tree.ForLoop tree) {
        ccode.print("for (");
        if (tree.init.nonEmpty()) {
            assert inForInitOrStep == false;
            inForInitOrStep = true;
            try {
                if (tree.init.head.tag == Tree.VARDEF) {
                    doExpr(tree.init.head);
                    for (List<Tree> l = tree.init.tail; l.nonEmpty(); l = l.tail) {
                        Tree.VarDef vdef = (Tree.VarDef) l.head;
                        ccode.print(", " + vdef.name + " = ");
                        doExpr(vdef.init);
                    }
                } else {
                    doExprs(tree.init);
                }
            } finally {
                inForInitOrStep = false;
            }
        }
        ccode.print("; ");
        if (tree.cond != null) {
            doExpr(tree.cond);
        }
        ccode.print("; ");
        assert inForInitOrStep == false;
        inForInitOrStep = true;
        try {
            doExprs(tree.step);
        } finally {
            inForInitOrStep = false;
        }
        ccode.print(") ");
        doStatement(tree.body);
    }

    public void visitForeachLoop(Tree.ForeachLoop tree) {
        inconvertible(tree, "enhanced for loop");
    }

    public void visitLabelled(Tree.Labelled tree) {
        inconvertible(tree, "label");
    }

    public void visitSwitch(Tree.Switch tree) {
        ccode.print("switch ");
        if (tree.selector.tag == Tree.PARENS) {
            doExpr(tree.selector);
        } else {
            ccode.print("(");
            doExpr(tree.selector);
            ccode.print(")");
        }
        ccode.println(" {");
        doStatements(tree.cases, CCodeBuffer.LINE_SEP);
        ccode.align();
        ccode.print("}");
    }

    public void visitCase(Tree.Case tree) {
        if (tree.pat == null) {
            ccode.print("default");
        } else {
            ccode.print("case ");
            doExpr(tree.pat);
        }
        ccode.println(": ");
        ccode.indent();
        doStatements(tree.stats, CCodeBuffer.LINE_SEP);
        ccode.undent();
        ccode.align();
    }

    public void visitSynchronized(Tree.Synchronized tree) {
        inconvertible(tree, "synchronized block");
    }

    public void visitTry(Tree.Try tree) {
        inconvertible(tree, "try block");
    }

    public void visitCatch(Tree.Catch tree) {
        inconvertible(tree, "catch statement");
    }

    public void visitConditional(Tree.Conditional tree) {
        ccode.open(prec, TreeInfo.condPrec);
        doExpr(tree.cond, TreeInfo.condPrec);
        ccode.print(" ? ");
        doExpr(tree.truepart, TreeInfo.condPrec);
        ccode.print(" : ");
        doExpr(tree.falsepart, TreeInfo.condPrec);
        ccode.close(prec, TreeInfo.condPrec);
    }

    public void visitIf(Tree.If tree) {
        boolean doThen = !tree.cond.type.isFalse();
        boolean doElse = !tree.cond.type.isTrue();

        if (doThen) {
            ccode.print("if ");
            if (tree.cond.tag == Tree.PARENS) {
                doExpr(tree.cond);
            }
            else {
                ccode.print("(");
                doExpr(tree.cond);
                ccode.print(")");
            }
            ccode.print(" ");
            doStatement(tree.thenpart);
        }
        if (doElse && tree.elsepart != null) {
            if (doThen) {
                ccode.print(" else ");
            }
            doStatement(tree.elsepart);
        }
    }

    public void visitExec(Tree.Exec tree) {
        doExpr(tree.expr);
        if (prec == TreeInfo.notExpression && !inForInitOrStep) {
            ccode.print(";");
        }
    }

    public void visitBreak(Tree.Break tree) {
        ccode.print("break");
        if (tree.label != null) {
            inconvertible(tree, "labelled break");
        }
        ccode.print(";");
    }

    public void visitContinue(Tree.Continue tree) {
        ccode.print("continue");
        if (tree.label != null) {
            inconvertible(tree, "labelled continue");
        }
        ccode.print(";");
    }

    public void visitReturn(Tree.Return tree) {
        ccode.print("return");
        if (tree.expr != null) {
            ccode.print(" ");
            doExpr(tree.expr);
        }
        ccode.print(";");
    }

    public void visitThrow(Tree.Throw tree) {
        ccode.print("fatalVMError(\"" + tree.expr.type.tsym.fullName() + "\");");
    }

    public void visitAssert(Tree.Assert tree) {
        inconvertible(tree, "assertion");
    }

    public void visitApply(Tree.Apply tree) {
        MethodSymbol method = (MethodSymbol)Converter.getSymbol(tree.meth);

        if ((method.flags() & Flags.ABSTRACT) != 0) {
            List<ProcessedMethod> impls = conv.getImplementersOf(method);
            if (impls.isEmpty()) {
                inconvertible(tree, "abstract method invocation");
            }
        } else {
            if (isVirtual(method)) {
                inconvertible(tree, "virtual method invocation");
            }
        }
        if (method.isStatic()) {
            ccode.print(conv.asString(method));
            ccode.print("(");
        } else {
            String receiver;
            if (tree.meth.tag == Tree.IDENT) {
                receiver = "this";
            } else {
                receiver = exprToString(tree.meth, TreeInfo.noPrec, false);
                Type receiverType = method.enclClass().type;
                if (conv.isReferenceType(receiverType)) {
                    receiver = "nullPointerCheck(" + receiver + ')';
                }
            }

            ccode.print(conv.asString(method));
            ccode.print("(" + receiver);
            if (!tree.args.isEmpty()) {
                ccode.print(", ");
            }
        }
        doExprs(tree.args);
        ccode.print(")");
    }

    public void visitNewClass(Tree.NewClass tree) {
        inconvertible(tree, "method-local class definition");
    }

    public void visitNewArray(Tree.NewArray tree) {
        inconvertible(tree, "array allocator");
    }

    public void visitParens(Tree.Parens tree) {
        ccode.print("(");
        doExpr(tree.expr);
        ccode.print(")");
    }

    public void visitAssign(Tree.Assign tree) {
        Symbol lhsSym = Converter.getSymbol(tree.lhs);
        boolean lhsLocal = lhsSym.isLocal();
        boolean lhsGlobal = !lhsLocal && conv.isGlobalVariable(lhsSym);

        if (parentsPrec != TreeInfo.notExpression && !lhsLocal && !lhsGlobal) {
            inconvertible(tree, "assignment to non-local, non-global variable as an expression");
        }

        ccode.open(prec, TreeInfo.assignPrec);
        doExpr(tree.lhs, TreeInfo.assignPrec + 1, true);

        if (lhsSym.isStatic() || lhsLocal) {
            ccode.print(" = ");
        }
        doExpr(tree.rhs, TreeInfo.assignPrec);
        if (!lhsSym.isStatic() && !lhsGlobal && !lhsLocal) {
            ccode.print(')');
        }
        ccode.close(prec, TreeInfo.assignPrec);
    }

    public String operatorName(int tag) {
        switch (tag) {
            case Tree.POS:
                return "+";
            case Tree.NEG:
                return "-";
            case Tree.NOT:
                return "!";
            case Tree.COMPL:
                return "~";
            case Tree.PREINC:
                return "++";
            case Tree.PREDEC:
                return "--";
            case Tree.POSTINC:
                return "++";
            case Tree.POSTDEC:
                return "--";
            case Tree.NULLCHK:
                return "<*nullchk*>";
            case Tree.OR:
                return "||";
            case Tree.AND:
                return "&&";
            case Tree.EQ:
                return "==";
            case Tree.NE:
                return "!=";
            case Tree.LT:
                return "<";
            case Tree.GT:
                return ">";
            case Tree.LE:
                return "<=";
            case Tree.GE:
                return ">=";
            case Tree.BITOR:
                return "|";
            case Tree.BITXOR:
                return "^";
            case Tree.BITAND:
                return "&";
            case Tree.SL:
                return "<<";
            case Tree.SR:
                return ">>";
            case Tree.USR:
                return ">>>";
            case Tree.PLUS:
                return "+";
            case Tree.MINUS:
                return "-";
            case Tree.MUL:
                return "*";
            case Tree.DIV:
                return "/";
            case Tree.MOD:
                return "%";
            default:
                throw new Error();
        }
    }

    public void visitAssignop(Tree.Assignop tree) {
        if (parentsPrec != TreeInfo.notExpression && !Converter.getSymbol(tree.lhs).isLocal()) {
            inconvertible(tree, "compound assignment to non-local variable as an expression");
        }

        ccode.open(prec, TreeInfo.assignopPrec);
        TreeMaker maker = TreeMaker.instance(conv.context);
        maker.pos = tree.pos;
        Tree.Binary binary = maker.Binary(tree.tag - Tree.ASGOffset, tree.lhs, tree.rhs);
        Tree.Assign assign = maker.Assign(tree.lhs, maker.Parens(binary));

        // Visit the replacement node directly without going through doExpr so that
        // the precedence level is preserved
        visitAssign(assign);

        ccode.close(prec, TreeInfo.assignopPrec);
    }

    public void visitUnary(Tree.Unary tree) {
        assert tree.arg.type.tag != TypeTags.FLOAT && tree.arg.type.tag != TypeTags.DOUBLE;
        String opname = operatorName(tree.tag).toString();
        if (tree.tag >= Tree.PREINC && tree.tag <= Tree.POSTDEC && !Converter.getSymbol(tree.arg).isLocal()) {
            inconvertible(tree, "side-effecting unary operator '" + opname + "' applied to non-local variable");
        }
        int ownprec = TreeInfo.opPrec(tree.tag);
        ccode.open(prec, ownprec);
        if (tree.tag <= Tree.PREDEC) {
            ccode.print(opname);
            doExpr(tree.arg, ownprec);
        } else {
            doExpr(tree.arg, ownprec);
            ccode.print(opname);
        }
        ccode.close(prec, ownprec);
    }

    public void visitBinary(Tree.Binary tree) {

        int ownprec = TreeInfo.opPrec(tree.tag);
        String opname = operatorName(tree.tag).toString();

        ccode.open(prec, ownprec);

        Tree lhs = tree.lhs;
        Tree rhs = tree.rhs;

        if (tree.tag == Tree.PLUS && (!lhs.type.isPrimitive() || !rhs.type.isPrimitive())) {
            inconvertible(tree, "string concatenation");
        }

        if (lhs.type.tag == TypeTags.FLOAT || rhs.type.tag == TypeTags.FLOAT ||
            lhs.type.tag == TypeTags.DOUBLE || rhs.type.tag == TypeTags.DOUBLE)
        {
            inconvertible(tree, "float or double operations");
        }

        boolean isLong = (lhs.type.tag == TypeTags.LONG);
        boolean infix = true;
        switch (tree.tag) {
            case Tree.PLUS:
            case Tree.MINUS:
            case Tree.MUL:
            case Tree.OR:
            case Tree.AND:
            case Tree.BITOR:
            case Tree.BITXOR:
            case Tree.BITAND:
            case Tree.EQ:
            case Tree.NE:
            case Tree.LT:
            case Tree.GT:
            case Tree.LE:
            case Tree.GE: {
                break;
            }
            case Tree.SL: {
                ccode.print(isLong ? "slll" : "sll");
                infix = false;
                break;
            }
            case Tree.SR: {
                ccode.print(isLong ? "sral" : "sra");
                infix = false;
                break;
            }
            case Tree.USR: {
                ccode.print(isLong ? "srll" : "srl");
                infix = false;
                break;
            }
            case Tree.DIV: {
                if (isLong) {
                    ccode.print("div_l");
                } else {
                    ccode.print("div_i");
                }
                infix = false;
                break;
            }
            case Tree.MOD: {
                if (isLong) {
                    ccode.print("rem_l");
                } else {
                    ccode.print("rem_i");
                }
                infix = false;
                break;
            }
            default:
                assert false;
        }

        if (!infix) {
            ccode.print("(");
        }

        doExpr(lhs, ownprec);
        if (infix) {
            ccode.print(" " + opname + " ");
        } else {
            ccode.print(", ");
        }
        doExpr(rhs, ownprec);

        if (!infix) {
            ccode.print(")");
        }

        ccode.close(prec, ownprec);
    }

    public void visitTypeCast(Tree.TypeCast tree) {
        ccode.open(prec, TreeInfo.prefixPrec);
        ccode.print("(");
        ccode.print(conv.asString(tree.type));
        ccode.print(")");
        doExpr(tree.expr, TreeInfo.prefixPrec);
        ccode.close(prec, TreeInfo.prefixPrec);
    }

    public void visitTypeTest(Tree.TypeTest tree) {
        inconvertible(tree, "instanceof");
    }

    public void visitIndexed(Tree.Indexed tree) {
        char componentType;
        switch (tree.type.tag) {
            case TypeTags.BOOLEAN:
            case TypeTags.BYTE:      componentType = 'b'; break;
            case TypeTags.CHAR:
            case TypeTags.SHORT:     componentType = 's'; break;
            case TypeTags.INT:       componentType = 'i'; break;
            case TypeTags.LONG:      componentType = 'l'; break;
            case TypeTags.FLOAT:     componentType = 'f'; break;
            case TypeTags.DOUBLE:    componentType = 'd'; break;
            default: {
                if (conv.isReferenceType(tree.type)) {
                    componentType = 'o';
                } else {
                    componentType = 'i';
                }
                break;
            }
        }

        String indexed = exprToString(tree.indexed, TreeInfo.noPrec, false);
        String index = exprToString(tree.index, TreeInfo.noPrec, false);

        if (lvalue) {
            if (componentType == 'o') {
                inconvertible(tree, "object array store");
            }
            ccode.print("astore_" + componentType + '(' + indexed + ", " + index + ", ");
        } else {
            ccode.print("aload_" + componentType + '(' + indexed + ", " + index + ')');
        }
    }

    public void visitSelect(Tree.Select tree) {
        Symbol sym = tree.sym;
        if (sym instanceof VarSymbol) {
            VarSymbol var = (VarSymbol)sym;
            String object = null;
            if (!var.isStatic()) {
                object = exprToString(tree.selected, TreeInfo.noPrec, false);
                if (conv.isReferenceType(var.type)) {
                    object = "nullPointerCheck(" + object + ")";
                }
            }
            ccode.print(conv.asString(tree, var, lvalue, object));
        } else {
            doExpr(tree.selected, TreeInfo.postfixPrec);
        }
    }

    public void visitIdent(Tree.Ident tree) {
        assert tree.sym != null;

        if (tree.sym instanceof VarSymbol) {
            VarSymbol var = (VarSymbol)tree.sym;
            String object = null;
            if (!var.isStatic() && !var.isLocal()) {
                object = "this";
            }
            ccode.print(conv.asString(tree, var, lvalue, object));
        } else {
            ccode.print(conv.asString((MethodSymbol) tree.sym));
        }
    }

    public void visitLiteral(Tree.Literal tree) {
        switch (tree.typetag) {
            case TypeTags.INT:
                ccode.print(tree.value.toString());
                break;
            case TypeTags.LONG:
                ccode.print(tree.value + "L");
                break;
            case TypeTags.FLOAT:
                ccode.print(tree.value + "F");
                break;
            case TypeTags.DOUBLE:
                ccode.print(tree.value.toString());
                break;
            case TypeTags.CHAR:
                ccode.print("\'" +
                            Convert.quote(
                                String.valueOf((char) ((Number) tree.value).intValue())) +
                            "\'");
                break;
            default:
                String literal = Convert.quote(tree.value.toString());
                String key = conv.getLiteralKey(method.sym.enclClass(), literal);
                String className = method.sym.enclClass().fullName().toString().replace('.', '_');
                ccode.print("getObjectForCStringLiteral(" + key + ", " + className + ", \"" + literal + "\")");
                break;
        }
    }

    public void visitTypeIdent(Tree.TypeIdent tree) {
        assert false;
    }

    public void visitTypeArray(Tree.TypeArray tree) {
        inconvertible(tree, "generics");
    }

    public void visitTypeApply(Tree.TypeApply tree) {
        inconvertible(tree, "generics");
    }

    public void visitTypeParameter(Tree.TypeParameter tree) {
        inconvertible(tree, "generics");
    }

    public void visitErroneous(Tree.Erroneous tree) {
        ccode.print("(ERROR)");
    }

    public void visitLetExpr(Tree.LetExpr tree) {
        inconvertible(tree, "'let' expression");
    }

    public void visitModifiers(Tree.Modifiers mods) {
    }

    public void visitAnnotation(Tree.Annotation tree) {
        inconvertible(tree, "annotation");
    }

    public void visitTree(Tree tree) {
        inconvertible(tree, "unknown construct");
    }
}
