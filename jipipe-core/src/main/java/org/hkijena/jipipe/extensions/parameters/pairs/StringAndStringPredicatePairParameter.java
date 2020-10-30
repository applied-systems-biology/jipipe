/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.parameters.pairs;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;
import org.hkijena.jipipe.extensions.parameters.predicates.StringPredicate;
import org.hkijena.jipipe.extensions.parameters.util.LogicalOperation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * A parameter that renames an integer into another integer
 */
public class StringAndStringPredicatePairParameter extends PairParameter<String, StringPredicate> {

    /**
     * Creates a new instance
     */
    public StringAndStringPredicatePairParameter() {
        super(String.class, StringPredicate.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringAndStringPredicatePairParameter(StringAndStringPredicatePairParameter other) {
        super(other);
    }

    /**
     * A collection of multiple {@link StringAndStringPredicatePairParameter}
     */
    public static class List extends ListParameter<StringAndStringPredicatePairParameter> {
        /**
         * Creates a new instance
         */
        public List() {
            super(StringAndStringPredicatePairParameter.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(StringAndStringPredicatePairParameter.class);
            for (StringAndStringPredicatePairParameter filter : other) {
                add(new StringAndStringPredicatePairParameter(filter));
            }
        }

        /**
         * Tests if the filter list applies to the map
         * @param map the map
         * @param same operation within the same key
         * @param different operation between different keys
         * @return if the filter applies to the map
         */
        public boolean test(Map<String, String> map, LogicalOperation same, LogicalOperation different) {
            Multimap<String, StringPredicate> multimap = HashMultimap.create();
            for (StringAndStringPredicatePairParameter pair : this) {
                multimap.put(pair.getKey(), pair.getValue());
            }
            java.util.List<Boolean> betweenResults = new ArrayList<>();
            for (Map.Entry<String, Collection<StringPredicate>> entry : multimap.asMap().entrySet()) {
                String value = map.getOrDefault(entry.getKey(), null);
                if(value == null) {
                    betweenResults.add(false);
                    continue;
                }
                java.util.List<Boolean> inResults = new ArrayList<>();
                for (StringPredicate predicate : entry.getValue()) {
                    inResults.add(predicate.test(value));
                }
                switch (same) {
                    case LogicalOr:
                        betweenResults.add(inResults.contains(true));
                        break;
                    case LogicalAnd:
                        betweenResults.add(!inResults.contains(false));
                        break;
                    case LogicalXor:
                        betweenResults.add(inResults.stream().filter(x -> x).count() == 1);
                        break;
                }
            }
            switch (different) {
                case LogicalOr:
                    return betweenResults.contains(true);
                case LogicalAnd:
                    return !betweenResults.contains(false);
                case LogicalXor:
                    return betweenResults.stream().filter(x -> x).count() == 1;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }
}
