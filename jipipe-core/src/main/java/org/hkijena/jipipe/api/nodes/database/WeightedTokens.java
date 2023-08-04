package org.hkijena.jipipe.api.nodes.database;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class WeightedTokens {

    public static final int WEIGHT_NAME = 3;
    public static final int WEIGHT_MENU = 2;
    public static final int WEIGHT_DESCRIPTION = 1;

    public final List<String> tokens = new ArrayList<>();
    public final TIntList weights = new TIntArrayList();

    /**
     * Adds a text to tokenize
     * @param text the text
     * @param weight the weight
     */
    public void add(String text, int weight) {
        if(StringUtils.isNullOrEmpty(text))
            return;
        text = text.toLowerCase().replace('\n', ' ');
        for (String s : text.split(" ")) {
            if(!StringUtils.isNullOrEmpty(s)) {
                tokens.add(s);
                weights.add(weight);
            }
        }
    }

    public String getToken(int index) {
        return tokens.get(index);
    }

    public int getWeight(int index) {
        return weights.get(index);
    }
    public int size() {
        return tokens.size();
    }
}
