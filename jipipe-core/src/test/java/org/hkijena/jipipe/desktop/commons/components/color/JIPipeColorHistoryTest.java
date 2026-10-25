/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.commons.components.color;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The color-history persistence behind the color dialog (#1339): recent-color
 * ordering/dedup/cap, saved-color round-trip, and re-save name update.
 */
class JIPipeColorHistoryTest {

    @TempDir
    Path dir;

    private JIPipeColorHistory fresh() {
        return new JIPipeColorHistory(dir.resolve("color-history.json"));
    }

    @Test
    void rememberMovesToFrontDedupesAndCaps() {
        JIPipeColorHistory h = fresh();
        Color a = new Color(10, 20, 30);
        Color b = new Color(40, 50, 60);
        h.remember(a);
        h.remember(b);
        // most recent first
        assertEquals(2, h.getRecent().size());
        assertEquals(b.getRGB(), h.getRecent().get(0).getColor().getRGB());
        // remembering a again moves it to the front, no duplicate
        h.remember(a);
        assertEquals(2, h.getRecent().size());
        assertEquals(a.getRGB(), h.getRecent().get(0).getColor().getRGB());
        // cap
        for (int i = 0; i < JIPipeColorHistory.RECENT_CAP + 3; i++) {
            h.remember(new Color(i * 10, i, 255 - i));
        }
        assertEquals(JIPipeColorHistory.RECENT_CAP, h.getRecent().size());
    }

    @Test
    void savedRoundTripsAndNameUpdates() {
        Path file = dir.resolve("saved.json");
        JIPipeColorHistory h = new JIPipeColorHistory(file);
        Color red = new Color(0xe0, 0x1b, 0x24);
        h.save(new NamedColor(red, "my red"));
        assertEquals(1, h.getSaved().size());
        assertEquals("my red", h.getSaved().get(0).getName());

        // reload from disk
        JIPipeColorHistory h2 = new JIPipeColorHistory(file);
        assertEquals(1, h2.getSaved().size());
        assertEquals(red.getRGB(), h2.getSaved().get(0).getColor().getRGB());
        assertEquals("my red", h2.getSaved().get(0).getName());

        // re-saving the same RGB updates the name in place (no duplicate)
        h2.save(new NamedColor(red, "renamed"));
        assertEquals(1, h2.getSaved().size());
        assertEquals("renamed", h2.getSaved().get(0).getName());
    }

    @Test
    void removeSavedWorks() {
        JIPipeColorHistory h = fresh();
        Color c = new Color(1, 2, 3);
        h.save(new NamedColor(c, "x"));
        assertEquals(1, h.getSaved().size());
        h.removeSaved(c);
        assertEquals(0, h.getSaved().size());
    }

    @Test
    void recentAlphaRoundTrips() {
        Path file = dir.resolve("alpha.json");
        JIPipeColorHistory h = new JIPipeColorHistory(file);
        Color semi = new Color(59, 157, 255, 0x80);
        h.remember(semi);
        JIPipeColorHistory h2 = new JIPipeColorHistory(file);
        List<NamedColor> r = h2.getRecent();
        assertEquals(semi.getRGB(), r.get(0).getColor().getRGB(), "alpha must round-trip");
    }
}
