package org.hkijena.jipipe.api.data;

import javax.swing.*;

/**
 * A user-interface operation on data
 */
public interface JIPipeDataOperation {
    /**
     * @return The name of this operation
     */
    String getName();

    /**
     * @return a description of the operation
     */
    String getDescription();

    /**
     * @return the order in menu. lower values are sorted to the top. The first one is used as default if the user did not select one.
     */
    int getOrder();

    /**
     * @return optional icon for the operation. can be null
     */
    Icon getIcon();
}
