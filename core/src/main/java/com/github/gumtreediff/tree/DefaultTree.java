/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public class DefaultTree extends AbstractTree implements Tree {
    private Type type;

    private String label;
    private String isaLabel;

    private int pos;
    private int length;

    private AssociationMap metadata;

    /**
     * Constructs a new node with an empty label.
     * @param type the type of the node
     * @see TreeContext#createTree(Type, String)
     */
    public DefaultTree(Type type) {
        this(type, NO_LABEL);
    }

    /**
     * Constructs a new node with a given label.
     * @param type the type of the node
     * @param label the label. If null, it will be replaced by an empty string.
     *              Note that the label will be interned.
     * @see TreeContext#createTree(Type, String)
     * @see String#intern()
     */
    public DefaultTree(Type type, String label) {
        this.type = type;
        this.label = (label == null) ? NO_LABEL : label.intern();
        this.isaLabel = computeIsaLabel(this.label);
        this.children = new ArrayList<>();
    }

    private static final List<String> ISA_KEYWORDS = Arrays.asList(
            // ---- LoongArch ----
            "loongarch64","loongarch32","loongarch","loong64","loong32","loong",

            // ---- RISC-V ----
            "riscv64","riscv32","riscv",

            // ---- ARM64 ----
            "arm64","aarch64",

            // ---- ARM ----
            "aarch32","aarch","arm",

            // ---- X86 ----
            "x86_64","x64","x86","ia32","i386",

            // ---- S390 ----
            "s390x","s390","systemz",

            // ---- PowerPC ----
            "powerpc64","powerpc32","powerpc","ppc64","ppc32","ppc",

            // ---- MIPS ----
            "mips64","mips32","mips"
    );

    private static final Pattern ISA_PATTERN;

    static {
        // 按长度从大到小排序，避免 riscv 先吃掉 riscv64
        ISA_KEYWORDS.sort((a, b) -> Integer.compare(b.length(), a.length()));

        StringBuilder sb = new StringBuilder();
        sb.append("(?i)("); // ignore case

        for (int i = 0; i < ISA_KEYWORDS.size(); i++) {
            if (i > 0) sb.append("|");
            sb.append(Pattern.quote(ISA_KEYWORDS.get(i)));
        }

        sb.append(")");
        ISA_PATTERN = Pattern.compile(sb.toString());
    }

    private static String computeIsaLabel(String label) {
        if (label == null || label.isEmpty())
            return NO_LABEL;

        String newLabel = ISA_PATTERN.matcher(label).replaceAll("@");

        if (newLabel.equals(label))
            return label;

        return newLabel.intern();
    }

    /**
     * Construct a node using a given node as the model. It copies only
     * the local attributes of the given node, and not its parent and children.
     * @param other the model node, must be not null.
     */
    protected DefaultTree(Tree other) {
        this.type = other.getType();
        this.label = other.getLabel();
        this.isaLabel = other.getIsaLabel();
        this.pos = other.getPos();
        this.length = other.getLength();
        this.children = new ArrayList<>();
    }

    @Override
    public Tree deepCopy() {
        Tree copy = new DefaultTree(this);
        for (Tree child : getChildren())
            copy.addChild(child.deepCopy());
        return copy;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getIsaLabel() {
        return isaLabel;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public int getPos() {
        return pos;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void setLabel(String label) {
        this.label = (label == null) ? NO_LABEL : label.intern();
        this.isaLabel = computeIsaLabel(this.label);
    }

    @Override
    public void setLength(int length) {
        this.length = length;
    }

    @Override
    public void setPos(int pos) {
        this.pos = pos;
    }

    @Override
    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public Object getMetadata(String key) {
        if (metadata == null)
            return null;
        return metadata.get(key);
    }

    @Override
    public Object setMetadata(String key, Object value) {
        if (value == null) {
            if (metadata == null)
                return null;
            else
                return metadata.remove(key);
        }
        if (metadata == null)
            metadata = new AssociationMap();
        return metadata.set(key, value);
    }

    @Override
    public Iterator<Entry<String, Object>> getMetadata() {
        if (metadata == null)
            return new EmptyEntryIterator();
        return metadata.iterator();
    }
}
