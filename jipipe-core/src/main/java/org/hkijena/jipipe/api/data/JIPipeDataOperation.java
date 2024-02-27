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

package org.hkijena.jipipe.api.data;

import javax.swing.*;

/**
 * A user-interface operation on data
 */
public interface JIPipeDataOperation {

    /**
     * @return Unique ID of this operation
     */
    String getId();

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
