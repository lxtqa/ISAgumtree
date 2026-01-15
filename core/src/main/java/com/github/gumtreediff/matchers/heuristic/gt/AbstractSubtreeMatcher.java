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

package com.github.gumtreediff.matchers.heuristic.gt;

import java.util.*;
import java.util.function.Function;

import com.github.gumtreediff.matchers.*;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import com.google.common.collect.Sets;

public abstract class AbstractSubtreeMatcher implements Matcher {
    private static final int DEFAULT_MIN_PRIORITY = 1;
    protected int minPriority = DEFAULT_MIN_PRIORITY;

    private static final String DEFAULT_PRIORITY_CALCULATOR = "height";
    protected Function<Tree, Integer> priorityCalculator = PriorityTreeQueue
            .getPriorityCalculator(DEFAULT_PRIORITY_CALCULATOR);

    protected Tree src;
    protected Tree dst;
    protected MappingStore mappings;

    public AbstractSubtreeMatcher() {
    }

    @Override
    public void configure(GumtreeProperties properties) {
        this.minPriority = properties.tryConfigure(ConfigurationOptions.st_minprio, minPriority);
        this.priorityCalculator = PriorityTreeQueue.getPriorityCalculator(
                properties.tryConfigure(ConfigurationOptions.st_priocalc, DEFAULT_PRIORITY_CALCULATOR));
    }

    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        this.src = src;
        this.dst = dst;
        this.mappings = mappings;
        List<Pair<Set<Tree>, Set<Tree>>> ambiguousMappings = new ArrayList<>();

        PriorityTreeQueue srcTrees = new DefaultPriorityTreeQueue(src, this.minPriority, this.priorityCalculator);
        PriorityTreeQueue dstTrees = new DefaultPriorityTreeQueue(dst, this.minPriority, this.priorityCalculator);

        // 整个match开始计时
        // long matchStartTime = System.nanoTime();

        while (PriorityTreeQueue.synchronize(srcTrees, dstTrees)) {
            var localHashMappings = new HashBasedMapper();
            localHashMappings.addSrcs(srcTrees.pop());
            localHashMappings.addDsts(dstTrees.pop());

            localHashMappings.unique().forEach(pair -> {
                Tree s = pair.first.stream().findAny().get();
                Tree d = pair.second.stream().findAny().get();
                if (s.getFuncId() == d.getFuncId() || d.getFuncId() == -1 || s.getFuncId() == -1) {
                    mappings.addMappingRecursively(s, d);
                }
            });


            localHashMappings.ambiguous().forEach(
                    (pair) -> ambiguousMappings.add(pair));

            localHashMappings.unmapped().forEach((pair) -> {
                pair.first.forEach(tree -> srcTrees.open(tree));
                pair.second.forEach(tree -> dstTrees.open(tree));
            });
        }

        // 模糊匹配开始计时
        // long ambiguousStartTime = System.nanoTime();
        handleAmbiguousMappings(ambiguousMappings);
        // // 模糊匹配结束计时
        // long ambiguousEndTime = System.nanoTime();

        // // 整个match结束计时
        // long matchEndTime = System.nanoTime();

        // long totalMatchTimeMillis = (matchEndTime - matchStartTime) / 1_000_000;
        // long ambiguousTimeMillis = (ambiguousEndTime - ambiguousStartTime) / 1_000_000;

        // double ambiguousRatio = (double) ambiguousTimeMillis / totalMatchTimeMillis * 100;

        // System.out.println("match总耗时: " + totalMatchTimeMillis + " ms");
        // System.out.println("模糊匹配耗时: " + ambiguousTimeMillis + " ms");
        // System.out.println("模糊匹配占比: " + String.format("%.2f", ambiguousRatio) + " %");
        return this.mappings;
    }

    public abstract void handleAmbiguousMappings(List<Pair<Set<Tree>, Set<Tree>>> ambiguousMappings);

    public int getMinPriority() {
        return minPriority;
    }

    public void setMinPriority(int minPriority) {
        this.minPriority = minPriority;
    }

    @Override
    public Set<ConfigurationOptions> getApplicableOptions() {
        return Sets.newHashSet(ConfigurationOptions.st_priocalc, ConfigurationOptions.st_minprio);
    }
}
