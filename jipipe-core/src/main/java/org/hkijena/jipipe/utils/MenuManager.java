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
 *
 */

package org.hkijena.jipipe.utils;

import javax.swing.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for managing menus
 */
public class MenuManager {
    private final JMenuBar menuBar;
    private final Map<String, JMenu> menus = new HashMap<>();

    public MenuManager() {
        this.menuBar = new JMenuBar();
    }

    public MenuManager(JMenuBar menuBar) {
        this.menuBar = menuBar;
    }

    public JMenu getOrCreateMenu(String... hierarchy) {
        String id = String.join("\n", hierarchy);
        JMenu result = menus.getOrDefault(id, null);
        if(result == null) {
            if(hierarchy.length == 1) {
                result = new JMenu(hierarchy[0]);
                menuBar.add(result);
            }
            else {
                result = new JMenu(hierarchy[hierarchy.length - 1]);
                getOrCreateMenu(Arrays.copyOf(hierarchy, hierarchy.length - 1)).add(result);
            }
            menus.put(id, result);
        }
        return result;
    }

    public JMenuBar getMenuBar() {
        return menuBar;
    }
}
