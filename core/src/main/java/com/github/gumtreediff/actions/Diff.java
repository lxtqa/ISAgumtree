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

import com.github.gumtreediff.gen.TreeGenerators;
import com.github.gumtreediff.matchers.GumtreeProperties;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.actions.PreMatcher;

import java.io.IOException;
import java.io.Reader;

/**
 * Class to facilitate the computation of diffs between ASTs.
 */
public class Diff {
    /**
     * The source AST in its context.
     */
    public final TreeContext src;

    /**
     * The destination AST in its context.
     */
    public final TreeContext dst;

    /**
     * The mappings between the two ASTs.
     */
    public final MappingStore mappings;

    /**
     * The edit script between the two ASTs.
     */
    public final EditScript editScript;

    /**
     * Only output mappings without computing edit script.
     */
    public final Boolean onlyMatch;

    /**
     * Instantiate a diff object with the provided source and destination
     * ASTs, the provided mappings, and the provided editScript.
     */
    public Diff(TreeContext src, TreeContext dst,
                MappingStore mappings, EditScript editScript, Boolean onlyMatch) {
        this.src = src;
        this.dst = dst;
        this.mappings = mappings;
        this.editScript = editScript;
        this.onlyMatch = onlyMatch;
    }

    /**
     * Compute and return a diff.
     * @param srcFile The path to the source file.
     * @param dstFile The path to the destination file.
     * @param treeGenerator The id of the tree generator to use.
     * @param matcher The id of the the matcher to use.
     * @param properties The set of options.
     * @param onlyMatch If true, only compute mappings without computing edit script.
     * @throws IOException an IO exception is raised in case of IO problems related to the source
     *     or destination file.
     */
    public static Diff compute(String srcFile, String dstFile, String treeGenerator,
                               String matcher, GumtreeProperties properties, Boolean onlyMatch) throws IOException {
        TreeContext src = TreeGenerators.getInstance().getTree(srcFile, treeGenerator);
        TreeContext dst = TreeGenerators.getInstance().getTree(dstFile, treeGenerator);

        return compute(src, dst, treeGenerator, matcher, properties, onlyMatch);
    }

    private static Diff compute(TreeContext src, TreeContext dst, String treeGenerator,
                               String matcher, GumtreeProperties properties, Boolean onlyMatch) throws IOException {
        PreMatcher.preprocess(src, dst);
        Matcher m = Matchers.getInstance().getMatcherWithFallback(matcher);
        m.configure(properties);

        // long matchStartTime = System.nanoTime();

        MappingStore mappings = m.match(src.getRoot(), dst.getRoot());

        // long matchEndTime = System.nanoTime();
        // long matchTimeMillis = (matchEndTime - matchStartTime) / 1_000_000;
        // System.out.println("Match 阶段耗时: " + matchTimeMillis + " ms");
        
        if (onlyMatch) {
            return new Diff(src, dst, mappings, null, onlyMatch);
        }

        // 记录生成操作阶段开始时间
        // long actionsStartTime = System.nanoTime();

        EditScript editScript = new SimplifiedChawatheScriptGenerator().computeActions(mappings);

        // long actionsEndTime = System.nanoTime();
        // long actionsTimeMillis = (actionsEndTime - actionsStartTime) / 1_000_000;
        // System.out.println("生成操作阶段耗时: " + actionsTimeMillis + " ms");

        return new Diff(src, dst, mappings, editScript, onlyMatch);
    }

    /**
     * Compute and return a diff.
     * @param srcReader The reader to the source file.
     * @param dstReader The reader to the destination file.
     * @param treeGenerator The id of the tree generator to use.
     * @param matcher The id of the the matcher to use.
     * @param properties The set of options.
     * @param onlyMatch If true, only compute mappings without computing edit script.
     * @throws IOException an IO exception is raised in case of IO problems related to the source
     *     or destination file.
     */
    public static Diff compute(Reader srcReader, Reader dstReader, String treeGenerator,
                               String matcher, GumtreeProperties properties, Boolean onlyMatch) throws IOException {
        TreeContext src = TreeGenerators.getInstance().getTree(srcReader, treeGenerator);
        TreeContext dst = TreeGenerators.getInstance().getTree(dstReader, treeGenerator);
        return compute(src, dst, treeGenerator, matcher, properties, onlyMatch);
    }

    /**
     * Compute and return a diff.
     * @param srcFile The path to the source file.
     * @param dstFile The path to the destination file.
     * @param command The executable command in the form: command $FILE.
     * @param matcher The id of the the matcher to use.
     * @param properties The set of options.
     * @throws IOException an IO exception is raised in case of IO problems related to the source
     *     or destination file.
     */
    public static Diff computeWithCommand(String srcFile, String dstFile, String command,
                               String matcher, GumtreeProperties properties, Boolean onlyMatch) throws IOException {
        TreeContext src = TreeGenerators.getInstance().getTreeFromCommand(srcFile, command);
        TreeContext dst = TreeGenerators.getInstance().getTreeFromCommand(dstFile, command);
        
        PreMatcher.preprocess(src, dst);

        Matcher m = Matchers.getInstance().getMatcherWithFallback(matcher);
        m.configure(properties);

        // 记录 match 阶段开始时间
        // long matchStartTime = System.nanoTime();

        MappingStore mappings = m.match(src.getRoot(), dst.getRoot());

        // long matchEndTime = System.nanoTime();
        // long matchTimeMillis = (matchEndTime - matchStartTime) / 1_000_000;
        // System.out.println("Match 阶段耗时: " + matchTimeMillis + " ms");

        if (onlyMatch) {
            return new Diff(src, dst, mappings, null, onlyMatch);
        }

        // 记录生成操作阶段开始时间
        // long actionsStartTime = System.nanoTime();

        EditScript editScript = new SimplifiedChawatheScriptGenerator().computeActions(mappings);

        // long actionsEndTime = System.nanoTime();
        // long actionsTimeMillis = (actionsEndTime - actionsStartTime) / 1_000_000;
        // System.out.println("生成操作阶段耗时: " + actionsTimeMillis + " ms");

        return new Diff(src, dst, mappings, editScript, onlyMatch);
    }

    /**
     * Compute and return a diff.
     * @param srcFile The path to the source file.
     * @param dstFile The path to the destination file.
     * @param treeGenerator The id of the tree generator to use.
     * @param matcher The id of the the matcher to use.
     * @param onlyMatch If true, only compute mappings without computing edit script.
     * @throws IOException an IO exception is raised in case of IO problems related to the source
     *     or destination file.
     */
    public static Diff compute(String srcFile, String dstFile,
                               String treeGenerator, String matcher, Boolean onlyMatch) throws IOException {
        return compute(srcFile, dstFile, treeGenerator, matcher, new GumtreeProperties(), onlyMatch);
    }

    /**
     * Compute and return a diff, using the default matcher and tree generators automatically
     * retrieved according to the file extensions.
     * @param srcFile The path to the source file.
     * @param dstFile The path to the destination file.
     * @throws IOException an IO exception is raised in case of IO problems related to the source
     *     or destination file.
     */
    public static Diff compute(String srcFile, String dstFile) throws IOException {
        return compute(srcFile, dstFile, null, null, false);
    }

    /**
     * Compute and return a all node classifier that indicates which node have
     * been added/deleted/updated/moved.
     */
    public TreeClassifier createAllNodeClassifier() {
        return new AllNodesClassifier(this);
    }

    /**
     * Compute and return a root node classifier that indicates which node have
     * been added/deleted/updated/moved. Only the root note is marked when a whole
     * subtree has been subject to a same operation.
     */
    public TreeClassifier createRootNodesClassifier() {
        return new OnlyRootsClassifier(this);
    }
}
