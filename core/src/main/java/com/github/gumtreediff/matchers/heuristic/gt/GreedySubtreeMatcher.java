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

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;

public class GreedySubtreeMatcher extends AbstractSubtreeMatcher {
    public GreedySubtreeMatcher() {
    }

    @Override
    public void handleAmbiguousMappings(List<Pair<Set<Tree>, Set<Tree>>> ambiguousMappings) {
        MappingComparators.FullMappingComparator comparator = new MappingComparators.FullMappingComparator(mappings);
        ambiguousMappings.sort(new AmbiguousMappingsComparator());
        //TODO：在此添加逻辑，计算平均耗时减少了多少
        //TODO: 再其他地方添加逻辑，计算预匹配耗时，作为对比
        ambiguousMappings.forEach((pair) -> {
            List<Mapping> candidates = convertToMappings(pair);
            candidates.sort(comparator);
            candidates.forEach(mapping -> {
                if (mappings.areBothUnmapped(mapping.first, mapping.second))
                    mappings.addMappingRecursively(mapping.first, mapping.second);
            });
        });
    }

    public static final List<Mapping> convertToMappings(Pair<Set<Tree>, Set<Tree>> ambiguousMapping) {
        List<Mapping> mappings = new ArrayList<>();
        // Integer count = 0;
        // Integer total = ambiguousMapping.first.size() * ambiguousMapping.second.size();
        for (Tree src : ambiguousMapping.first)
            for (Tree dst : ambiguousMapping.second)
                //TODO: 在此添加逻辑，当且仅当src和dst不构成映射时不添加
                if (src.getFuncId() == dst.getFuncId() || dst.getFuncId() == -1 || src.getFuncId() == -1)
                    mappings.add(new Mapping(src, dst));
                // else
                //     count += 1;
                //TODO: 在此添加逻辑，计算平均减少了多少次配对
        // System.out.println("Reduced " + count + " in " + total + " invalid mapping attempts based on function IDs.");
        return mappings;
    }

    // public static final List<Mapping> convertToMappings(Pair<Set<Tree>, Set<Tree>> ambiguousMapping) {
    //     List<Mapping> mappings = new ArrayList<>();
    //     for (Tree src : ambiguousMapping.first)
    //         for (Tree dst : ambiguousMapping.second)
    //             mappings.add(new Mapping(src, dst));
    //     return mappings;
    // }

    public static class AmbiguousMappingsComparator implements Comparator<Pair<Set<Tree>, Set<Tree>>> {
        @Override
        public int compare(Pair<Set<Tree>, Set<Tree>> m1, Pair<Set<Tree>, Set<Tree>> m2) {
            int s1 = m1.first.stream().max(Comparator.comparingInt(t -> t.getMetrics().size)).get().getMetrics().size;
            int s2 = m2.first.stream().max(Comparator.comparingInt(t -> t.getMetrics().size)).get().getMetrics().size;
            return Integer.compare(s2, s1);
        }
    }
}
