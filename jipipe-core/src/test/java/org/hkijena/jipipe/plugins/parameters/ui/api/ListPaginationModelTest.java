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

package org.hkijena.jipipe.plugins.parameters.ui.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * #1339: inline list pagination state — a pure (headless) model of how many
 * rows the inline list view shows, with "+k more" / "Show less" transitions.
 */
class ListPaginationModelTest {
    @Test void loadMoreRevealsPageSizeAtATime() {
        ListPaginationModel m = new ListPaginationModel(12, 5, true);
        assertEquals(5, m.visibleCount());
        assertEquals(7, m.loadMoreRemaining());
        assertTrue(m.showLoadMore());
        m.loadMore();
        assertEquals(10, m.visibleCount());
        assertEquals(2, m.loadMoreRemaining());
        m.loadMore();
        assertEquals(12, m.visibleCount());
        assertEquals(0, m.loadMoreRemaining());
        assertFalse(m.showLoadMore());
        assertTrue(m.showShowLess());
        m.loadMore();                 // at the end: no-op
        assertEquals(12, m.visibleCount());
    }
    @Test void showLessCollapsesBackToPageSize() {
        ListPaginationModel m = new ListPaginationModel(12, 5, true);
        m.loadMore(); m.loadMore();
        m.showLess();
        assertEquals(5, m.visibleCount());
        assertTrue(m.showLoadMore());
        assertFalse(new ListPaginationModel(3, 5, true).showLoadMore()); // n < pageSize
    }
    @Test void withScrollBarDisablesPagination() {
        ListPaginationModel m = new ListPaginationModel(50, 5, false);
        assertEquals(50, m.visibleCount());   // all rows, scrolling body
        assertEquals(0, m.loadMoreRemaining());
        m.loadMore(); m.showLess();           // no-ops
        assertEquals(50, m.visibleCount());
        assertFalse(m.showLoadMore());
        assertFalse(m.showShowLess());
    }
    @Test void emptyList() {
        ListPaginationModel m = new ListPaginationModel(0, 5, true);
        assertEquals(0, m.visibleCount());
        assertFalse(m.showLoadMore());
    }
}
