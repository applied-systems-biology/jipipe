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

package org.hkijena.jipipe.plugins.canvasnotes;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutAction;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutRegistry;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.canvasnotes.nodes.ArrowGraphCanvasNote;
import org.hkijena.jipipe.plugins.canvasnotes.nodes.ImageBoxGraphCanvasNote;
import org.hkijena.jipipe.plugins.canvasnotes.nodes.TextBoxGraphCanvasNote;
import org.hkijena.jipipe.plugins.canvasnotes.tools.ArrowNoteGraphNodeTool;
import org.hkijena.jipipe.plugins.canvasnotes.tools.EditNotesGraphNodeTool;
import org.hkijena.jipipe.plugins.canvasnotes.tools.ImageBoxNoteGraphNodeTool;
import org.hkijena.jipipe.plugins.canvasnotes.tools.TextBoxNoteGraphNodeTool;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import javax.swing.KeyStroke;

@Plugin(type = JIPipeJavaPlugin.class)
public class CanvasNotesPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Graph annotations";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides the default graph annotations");
    }

    @Override
    public void register(JIPipeService service, Context context, JIPipeProgressInfo progressInfo) {
        registerAnnotationNodeType("graph-annotation-text-box", TextBoxGraphCanvasNote.class, TextBoxNoteGraphNodeTool.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-text-frame.png"));
        registerAnnotationNodeType("graph-annotation-arrow", ArrowGraphCanvasNote.class, ArrowNoteGraphNodeTool.class, JIPipe.RESOURCES.getIcon16URL("actions/draw-arrow.png"));
        registerAnnotationNodeType("graph-annotation-image", ImageBoxGraphCanvasNote.class, ImageBoxNoteGraphNodeTool.class, JIPipe.RESOURCES.getIcon16URL("actions/insert-image.png"));
        registerGraphEditorTool(EditNotesGraphNodeTool.class);
        registerNotesToolShortcutActions();
    }

    /**
     * Registers the shortcut actions of the four canvas notes tools in the
     * application shortcut registry.
     * <p>
     * The ids are the stable string literals declared by the tool classes
     * (the same ids the LEGACY preset references for the pre-6.0 F-key
     * bindings). All actions are in the "Graph editor tools" category and
     * the graph scope family, are not text-input-safe, and default to the
     * {@code Ctrl+T} chord followed by a letter. Idempotent: an id that is
     * already registered is skipped.
     */
    private void registerNotesToolShortcutActions() {
        registerShortcutAction(JIPipeShortcutAction.builder(EditNotesGraphNodeTool.SHORTCUT_ACTION_ID, "Notes: Edit", "Graph editor tools")
                .description("Allows to move, edit, and delete canvas notes while active")
                .defaultChord(KeyStroke.getKeyStroke("control pressed T"), KeyStroke.getKeyStroke("pressed E"))
                .build());
        registerShortcutAction(JIPipeShortcutAction.builder(TextBoxNoteGraphNodeTool.SHORTCUT_ACTION_ID, "Notes: Text box", "Graph editor tools")
                .description("Creates a text box canvas note")
                .defaultChord(KeyStroke.getKeyStroke("control pressed T"), KeyStroke.getKeyStroke("pressed T"))
                .build());
        registerShortcutAction(JIPipeShortcutAction.builder(ArrowNoteGraphNodeTool.SHORTCUT_ACTION_ID, "Notes: Arrow", "Graph editor tools")
                .description("Creates an arrow canvas note")
                .defaultChord(KeyStroke.getKeyStroke("control pressed T"), KeyStroke.getKeyStroke("pressed A"))
                .build());
        registerShortcutAction(JIPipeShortcutAction.builder(ImageBoxNoteGraphNodeTool.SHORTCUT_ACTION_ID, "Notes: Image box", "Graph editor tools")
                .description("Creates an image canvas note")
                .defaultChord(KeyStroke.getKeyStroke("control pressed T"), KeyStroke.getKeyStroke("pressed I"))
                .build());
    }

    /**
     * Registers one shortcut action, skipping ids that are already
     * registered (same idempotent semantics as the core catalog).
     */
    private void registerShortcutAction(JIPipeShortcutAction action) {
        JIPipeShortcutRegistry registry = JIPipeShortcutRegistry.getInstance();
        if (!registry.isRegistered(action.id())) {
            registry.register(action);
        }
    }

    @Override
    public String getDependencyId() {
        return "jipipe:graph-annotations";
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public boolean isCorePlugin() {
        return true;
    }
}
