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

/**
 * #1339: pagination state for the inline list parameter editors.
 * <p>
 * A list with more than {@code pageSize} items shows the first page only;
 * a "+k more" button reveals another page each click, and "Show less"
 * collapses the list back to a single page. Lists rendered with a scroll bar
 * (the {@code withScrollBar} setting) show all rows at once — pagination is
 * disabled for them.
 * <p>
 * Pure state, no Swing — headless-testable.
 */
public final class ListPaginationModel {
    private final int total;
    private final int pageSize;
    private final boolean paginationEnabled;
    private int revealed;

    public ListPaginationModel(int total, int pageSize, boolean paginationEnabled) {
        if (total < 0) {
            throw new IllegalArgumentException("total must be >= 0, was " + total);
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be > 0, was " + pageSize);
        }
        this.total = total;
        this.pageSize = pageSize;
        this.paginationEnabled = paginationEnabled;
        this.revealed = Math.min(pageSize, total);
    }

    /** @return whether rows are paged (false for scroll-bar lists: all rows visible) */
    public boolean paginationEnabled() {
        return paginationEnabled;
    }

    /** @return the number of rows currently visible (all rows when disabled) */
    public int visibleCount() {
        return paginationEnabled ? Math.min(revealed, total) : total;
    }

    /** @return how many rows remain hidden beyond the visible page */
    public int loadMoreRemaining() {
        return paginationEnabled ? Math.max(0, total - visibleCount()) : 0;
    }

    /** @return whether the "+k more" button should be shown */
    public boolean showLoadMore() {
        return loadMoreRemaining() > 0;
    }

    /** @return whether the "Show less" button should be shown (list is expanded beyond one page) */
    public boolean showShowLess() {
        return paginationEnabled && visibleCount() > pageSize;
    }

    /** Reveals another page (no-op when pagination is disabled or fully revealed). */
    public void loadMore() {
        if (!paginationEnabled) {
            return;
        }
        revealed = Math.min(revealed + pageSize, total);
    }

    /** Collapses back to the first page (no-op when pagination is disabled). */
    public void showLess() {
        if (!paginationEnabled) {
            return;
        }
        revealed = pageSize;
    }

    /** @return the total number of rows, regardless of pagination */
    public int size() {
        return total;
    }
}
