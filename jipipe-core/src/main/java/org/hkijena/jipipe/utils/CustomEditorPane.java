package org.hkijena.jipipe.utils;

import org.scijava.script.ScriptLanguage;
import org.scijava.ui.swing.script.EditorPane;

/**
 * Version of the {@link EditorPane} that allows access to protected methods
 */
public class CustomEditorPane extends EditorPane {
    @Override
    public void setLanguage(ScriptLanguage language) {
        super.setLanguage(language);
    }
}
