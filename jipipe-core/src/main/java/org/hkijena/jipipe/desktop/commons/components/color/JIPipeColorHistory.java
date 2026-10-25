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

import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.color.ColorSpaces;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Recent + user-saved colors for the color dialog (#1339), persisted as a JSON
 * file in the JIPipe user directory.
 * <p>
 * Pure persistence logic (no Swing) so it is testable headless. The singleton
 * {@link #get()} uses the real user-dir path; tests use the injectable
 * path constructor.
 */
public class JIPipeColorHistory {

    /** The maximum number of "recent" colors kept. */
    public static final int RECENT_CAP = 8;

    /**
     * JSON store layout. JIPipe's mapper disables property auto-detection, so
     * the fields are explicitly annotated.
     */
    @com.fasterxml.jackson.annotation.JsonAutoDetect(
            fieldVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
    private static class Store {
        public List<String> recent = new ArrayList<>();   // hex strings, most recent first
        public List<SavedDto> saved = new ArrayList<>();
    }

    @com.fasterxml.jackson.annotation.JsonAutoDetect(
            fieldVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
    private static class SavedDto {
        public String hex;
        public String name;
    }

    private final Path file;
    private final List<NamedColor> recent = new CopyOnWriteArrayList<>();
    private final List<NamedColor> saved = new CopyOnWriteArrayList<>();

    private static volatile JIPipeColorHistory instance;

    /**
     * Creates an instance backed by an explicit file (package-private, for
     * tests). Loads existing data if present.
     */
    JIPipeColorHistory(Path file) {
        this.file = file;
        load();
    }

    /** The shared instance (stored in {@code <jipipe-user-dir>/color-history.json}). */
    public static JIPipeColorHistory get() {
        if (instance == null) {
            synchronized (JIPipeColorHistory.class) {
                if (instance == null) {
                    instance = new JIPipeColorHistory(PathUtils.getJIPipeUserDir().resolve("color-history.json"));
                }
            }
        }
        return instance;
    }

    // ------------------------------------------------------------------
    // API
    // ------------------------------------------------------------------

    /** @return the recent colors, most recent first (max {@link #RECENT_CAP}) */
    public List<NamedColor> getRecent() {
        return List.copyOf(recent);
    }

    /** @return the user-saved colors */
    public List<NamedColor> getSaved() {
        return List.copyOf(saved);
    }

    /**
     * Records a color as recently used: moved to the front, deduplicated
     * (by RGB, alpha included), capped at {@link #RECENT_CAP}.
     */
    public void remember(Color color) {
        recent.removeIf(c -> c.getColor().getRGB() == color.getRGB());
        recent.add(0, new NamedColor(color, null));
        while (recent.size() > RECENT_CAP) {
            recent.remove(recent.size() - 1);
        }
        save();
    }

    /**
     * Adds a color to the user's saved colors (named). Deduplicated by RGB;
     * re-saving with a new name updates the name in place.
     */
    public void save(NamedColor named) {
        for (int i = 0; i < saved.size(); i++) {
            if (saved.get(i).getColor().getRGB() == named.getColor().getRGB()) {
                saved.set(i, new NamedColor(named.getColor(), named.getName()));
                save();
                return;
            }
        }
        saved.add(new NamedColor(named.getColor(), named.getName()));
        save();
    }

    /** Removes a saved color (matched by RGB). */
    public void removeSaved(Color color) {
        saved.removeIf(c -> c.getColor().getRGB() == color.getRGB());
        save();
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try {
            Store store = JsonUtils.getObjectMapper().readValue(Files.readAllBytes(file), Store.class);
            if (store.recent != null) {
                for (String hex : store.recent) {
                    recent.add(new NamedColor(ColorSpaces.fromHex(hex), null));
                }
            }
            if (store.saved != null) {
                for (SavedDto dto : store.saved) {
                    if (dto.hex != null) {
                        saved.add(new NamedColor(ColorSpaces.fromHex(dto.hex), dto.name));
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            // A corrupt history file must never break the color dialog.
            System.err.println("Could not load color history from " + file + ": " + e.getMessage());
        }
    }

    private synchronized void save() {
        Store store = new Store();
        for (NamedColor c : recent) {
            store.recent.add(ColorSpaces.toHex(c.getColor()));
        }
        for (NamedColor c : saved) {
            SavedDto dto = new SavedDto();
            dto.hex = ColorSpaces.toHex(c.getColor());
            dto.name = c.getName();
            store.saved.add(dto);
        }
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(file.toFile(), store);
        } catch (IOException e) {
            System.err.println("Could not save color history to " + file + ": " + e.getMessage());
        }
    }
}
