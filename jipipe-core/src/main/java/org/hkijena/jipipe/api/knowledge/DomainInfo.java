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

package org.hkijena.jipipe.api.knowledge;

/**
 * One domain of the loaded corpus with its entry count.
 *
 * @param id         the stable domain id (the first path segment of the
 *                   entry keys, identical to the {@link KdbDomain} id)
 * @param label      the human readable label derived from the id
 * @param entryCount how many corpus entries belong to the domain (counting
 *                   every loaded entry, including entries that the version
 *                   drop hides from search)
 */
public record DomainInfo(String id, String label, long entryCount) {
}
