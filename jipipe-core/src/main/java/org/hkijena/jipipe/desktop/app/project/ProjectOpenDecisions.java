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

package org.hkijena.jipipe.desktop.app.project;

/**
 * The explicit decisions of one project-open run through
 * {@code JIPipeDesktopProjectWindow.openProject} (issue #1379). A null field keeps the
 * interactive behavior (the corresponding prompt is shown); a non-null field supplies
 * the decision, so the automation open_project operation runs the same flow without
 * showing prompts. The decisions never bypass a flow step: each one is the same choice
 * the interactive prompt would have offered.
 *
 * @param siblingChoice         the sibling decision (null shows the "Project crate
 *                              available" prompt)
 * @param alreadyOpenChoice     the decision for a project that is already open in a
 *                              window (null shows the "Project already open" prompt)
 * @param outputFolderChoice    the results decision of the output-folder flow (null
 *                              shows the "Open JIPipe output" prompt)
 * @param loadIntoThisWindow    the placement decision (null shows the "This window" /
 *                              "New window" prompt unless forceCurrentWindow is set)
 */
public record ProjectOpenDecisions(JIPipeProjectFlowDecisions.SiblingChoice siblingChoice,
                                   JIPipeProjectFlowDecisions.AlreadyOpenChoice alreadyOpenChoice,
                                   JIPipeProjectFlowDecisions.OutputFolderChoice outputFolderChoice,
                                   Boolean loadIntoThisWindow) {

    /**
     * The interactive decisions: every prompt is shown (the GUI behavior).
     */
    public static final ProjectOpenDecisions INTERACTIVE = new ProjectOpenDecisions(null, null, null, null);

    /**
     * The automation decisions for the open_project operation: the sibling choice
     * follows the format (auto/jipx open the crate sibling, legacy opens the legacy
     * file), an already-open project is brought to the front (the prompt's default),
     * an output folder loads its results into a new tab (the prompt's default), and
     * the placement follows forceCurrentWindow (true: the calling window, false:
     * a new window, the prompt's default).
     *
     * @param format             the resolved open format
     * @param forceCurrentWindow the force-current-window parameter
     * @return the decisions for one automated open
     */
    public static ProjectOpenDecisions forAutomation(JIPipeProjectFlowDecisions.OpenFormat format, boolean forceCurrentWindow) {
        return new ProjectOpenDecisions(
                JIPipeProjectFlowDecisions.siblingChoiceFor(format),
                JIPipeProjectFlowDecisions.AlreadyOpenChoice.BRING_TO_FRONT,
                JIPipeProjectFlowDecisions.OutputFolderChoice.NEW_TAB,
                forceCurrentWindow);
    }
}
