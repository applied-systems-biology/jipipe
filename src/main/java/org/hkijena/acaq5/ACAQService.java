package org.hkijena.acaq5;

import org.hkijena.acaq5.ui.ACAQUIService;
import org.hkijena.acaq5.ui.registries.ACAQPlotBuilderRegistry;

public interface ACAQService extends ACAQUIService {
    ACAQPlotBuilderRegistry getPlotBuilderRegistry();
}
