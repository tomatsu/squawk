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

import java.util.Collections;
import com.sun.tools.javac.tree.Tree;
import java.util.Map;
import java.util.HashMap;
import com.sun.tools.javac.tree.Tree.*;
import static com.sun.squawk.vm2c.CCodeBuffer.*;

/**
 * Utility class for extracting vm2c specific annotations from the javadoc
 * of a javac AST node. The format of these annotations is:
 *
 *  "@vm2c" key [ '(' value ')' ]
 *
 */
public class AnnotationParser {

    public static class Annotation {
        final String key;
        final boolean valueIsOptional;
        Annotation(String key, boolean valueIsOptional) {
            this.key = key;
            this.valueIsOptional = valueIsOptional;
        }
    }

    /**
     * The annotations understood by this parser.
     */
    final Map<String, Annotation> annotations;

    public AnnotationParser(Map<String, Annotation> annotations) {
        this.annotations = annotations;
    }

    public AnnotationParser() {
        this(DEFAULT_ANNOTATIONS);
    }

    public static final Map<String, Annotation> DEFAULT_ANNOTATIONS;
    static {
        Map<String, Annotation> m= new HashMap<String,Annotation>(5);
        m.put("root", new Annotation("root", false));
        m.put("code", new Annotation("code", false));
        m.put("macro", new Annotation("macro", false));
        m.put("proxy", new Annotation("proxy", true));
        m.put("implementers", new Annotation("implementers", false));
        DEFAULT_ANNOTATIONS = Collections.unmodifiableMap(m);
    }

    private void error(Tree tree, String msg) {
        throw new InconvertibleNodeException(tree, msg);
    }

    /**
     * Parses the javadoc for a method and extracts the annotations
     * (if any) that this parser was configured with.
     */
    public Map<String, String> parse(ProcessedMethod method) {
        return parse(method.unit, method.tree);
    }

    /**
     * Parses the javadoc for an AST node and extracts the annotations
     * (if any) that this parser was configured with.
     */
    public Map<String, String> parse(Tree.TopLevel unit, Tree node) {
        if (unit.docComments != null) {
            String dc = unit.docComments.get(node);
            if (dc != null) {
                int index = dc.indexOf("@vm2c ");
                Map<String, String> result = null;
                while (index != -1) {
                    if (result == null) {
                        result = new HashMap<String, String>();
                    }

                    index += "@vm2c ".length();
                    try {
                        while (Character.isWhitespace(dc.charAt(index))) {
                            ++index;
                        }

                        if (!Character.isJavaIdentifierStart(dc.charAt(index))) {
                            error(node, "invalid vm2c annotation key");
                        }

                        int keyStart = index;
                        while (Character.isJavaIdentifierPart(dc.charAt(index))) {
                            ++index;
                        }

                        String key = dc.substring(keyStart, index);
                        Annotation annotation = annotations.get(key);
                        if (annotation == null) {
                            error(node, "unknown vm2c annotation: " + key);
                        }

                        String value;
                        while (index != dc.length() && Character.isWhitespace(dc.charAt(index))) {
                            index++;
                        }

                        if (index != dc.length() && dc.charAt(index) == '(') {
                            index++;
                            int valueStart = index;
                            int nesting = 1;
                            while (nesting != 0) {
                                char c = dc.charAt(index++);
                                if (c == '(') {
                                    ++nesting;
                                } else if (c == ')') {
                                    --nesting;
                                }
                            }
                            value = dc.substring(valueStart, index - 1).trim();
                        } else {
                            if (!annotation.valueIsOptional) {
                                error(node, "vm2c annotation '" + key + "' that is missing a non-optional value");
                            }
                            value = "";
                        }
                        result.put(key, value);
                    } catch (StringIndexOutOfBoundsException e) {
                        error(node, "malformed vm2c annotation");
                    }
                    if (index == dc.length()) {
                        index = -1;
                    } else {
                        index = dc.indexOf("@vm2c ", index);
                    }
                }
                if (result != null) {
                    return result;
                }
            }
        }
        Map<String, String> m = Collections.emptyMap();
        return m;
    }

    /**
     * Align code to be indented to left margin.
     */
    private static StringBuilder indent(int margin, StringBuilder buf) {
        for (int i = 0; i < margin; i++) {
            buf.append(' ');
        }
        return buf;
    }

    /**
     * Parses the javadoc for an AST node and extracts the original javadoc (if any).
     */
    public static String getDocComment(Tree.TopLevel unit, Tree node, int margin) {
        if (unit.docComments != null) {
            String dc = unit.docComments.get(node);
            if (dc != null) {
                StringBuilder buf = new StringBuilder(dc.length() * 2);
                buf.append("/**").append(LINE_SEP);
                int pos = 0;
                int endpos = lineEndPos(dc, pos);
                while (pos < dc.length()) {
                    indent(margin, buf).append(" *");
                    if (pos < dc.length() && dc.charAt(pos) > ' ') {
                        buf.append(" ");
                    }
                    buf.append(dc.substring(pos, endpos)).append(LINE_SEP);
                    pos = endpos + 1;
                    endpos = lineEndPos(dc, pos);
                }
                indent(margin, buf).append(" */").append(LINE_SEP);
                return buf.toString();
            }
        }
        return "";
    }

    private static int lineEndPos(String s, int start) {
        int pos = s.indexOf('\n', start);
        if (pos < 0) pos = s.length();
        return pos;
    }
}
