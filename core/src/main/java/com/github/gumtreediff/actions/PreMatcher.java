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
 * Copyright 2022 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2022 Raquel Pau <raquelpau@gmail.com>
 */

package com.github.gumtreediff.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.Type;

/**
 * PreMatcher performs a preprocessing step on TreeContexts
 * before GumTree matching.
 * It:
 * 1) Extracts top-level function declaration nodes using DFS (short-circuit).
 * 2) Assigns the same integer ID to functions with the same name in src and dst.
 * 3) Tags all nodes inside each function body with the corresponding function ID
 *    using Tree.funcId.
 */
public class PreMatcher {

    /**
     * Entry point for preprocessing.
     */
    public static void preprocess(TreeContext src, TreeContext dst) {
        // 1. Extract function declarations
        List<Tree> srcFuncs = extractFunctionDecls(src.getRoot());
        List<Tree> dstFuncs = extractFunctionDecls(dst.getRoot());


        // 2. Match functions by name and assign IDs
        Map<String, Integer> functionNameToId = new HashMap<>();
        assignFunctionIds(srcFuncs, dstFuncs, functionNameToId);

        // 3. Tag all nodes inside each function
        tagFunctions(srcFuncs, functionNameToId);
        tagFunctions(dstFuncs, functionNameToId);
    }

    /**
     * DFS traversal that extracts function declaration nodes.
     * Once a function declaration is found, its subtree is NOT traversed.
     */
    private static List<Tree> extractFunctionDecls(Tree root) {
        List<Tree> result = new ArrayList<>();
        dfsExtract(root, result);
        return result;
    }

    private static void dfsExtract(Tree node, List<Tree> result) {
        try {
            if (isFunctionDecl(node)) {
                // System.out.println("Extracted so far: " + result.size() + ", now processing: " + node.getType());
                result.add(node);
            }
            for (Tree child : node.getChildren()) {
                dfsExtract(child, result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void assignFunctionIds(
            List<Tree> srcFuncs,
            List<Tree> dstFuncs,
            Map<String, Integer> functionNameToId) {

        Set<String> srcNames = new HashSet<>();
        for (Tree t : srcFuncs) {
            String name = getFunctionName(t);
            if (name != null) {
                srcNames.add(name);
            }
        }

        Set<String> dstNames = new HashSet<>();
        for (Tree t : dstFuncs) {
            String name = getFunctionName(t);
            if (name != null) {
                dstNames.add(name);
            }
        }

        // ⭐ 只取同时出现在 src 和 dst 中的函数名
        Set<String> commonNames = new HashSet<>(srcNames);
        commonNames.retainAll(dstNames);

        int nextId = 1;
        for (String name : commonNames) {
            functionNameToId.put(name, nextId++);
        }
    }

    /**
     * Tag all nodes inside each function declaration subtree
     * with the corresponding function ID.
     */
    private static void tagFunctions(
            List<Tree> funcs,
            Map<String, Integer> functionNameToId) {

        // 按照节点在树中的深度（Depth）降序排序
        // 深度大的节点（子函数）先处理，深度小的节点（父函数）后处理
        funcs.sort((a, b) -> Integer.compare(b.getMetrics().depth, a.getMetrics().depth));

        for (Tree func : funcs) {
            String name = getFunctionName(func);
            Integer id = functionNameToId.get(name);
            if (id != null) {
                tagSubtree(func, id);
            }
        }
    }

    private static void tagSubtree(Tree node, int functionId) {
        // 如果当前节点已经属于某个（子）函数，则不再覆盖，也不再深入其子树
        // 这样可以确保“最深层”的函数 ID 优先级最高
        if (node.getFuncId() != -1) {
            return;
        }

        node.setFuncId(functionId);
        for (Tree child : node.getChildren()) {
            tagSubtree(child, functionId);
        }
    }

    /**
     * Determine whether a node is a function declaration.
     */
    private static boolean isFunctionDecl(Tree node) {
        String type = node.getType().toString();
        return type != null && type.equals("function");
    }

    /**
     * Extract function name from a function declaration node.
     */
    private static String getFunctionName(Tree node) {
        String functionName = "";
        for (Tree child : node.getChildren()) {
            if (child.getType() != Type.NO_TYPE && child.getType().toString().equals("name")) {
                if (child.getIsaLabel() != Tree.NO_LABEL) {
                    return child.getIsaLabel();
                }
                else {
                    for (Tree gc : child.getChildren()) {
                        if (gc.getIsaLabel() != Tree.NO_LABEL)
                            functionName += gc.getIsaLabel();
                    }
                    return functionName;
                }
            }
        }
        return null;
    }
}
