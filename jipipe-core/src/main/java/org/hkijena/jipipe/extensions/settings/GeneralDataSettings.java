package org.hkijena.jipipe.extensions.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.pairs.IntegerAndIntegerPairParameter;

public class GeneralDataSettings extends AbstractJIPipeParameterCollection {
    public static String ID = "general-data";

    private boolean autoSaveLastImporter = true;
    private boolean autoSaveLastDisplay = true;
    private int previewSize = 64;
    private int maxTableColumnSize = 250;
    private boolean generateCachePreviews = true;
    private boolean generateResultPreviews = true;
    private boolean autoRemoveOutdatedCachedData = true;

    private IntegerAndIntegerPairParameter.List exportedPreviewSizes = new IntegerAndIntegerPairParameter.List();

    public GeneralDataSettings() {
        exportedPreviewSizes.add(new IntegerAndIntegerPairParameter(64, 64));
        exportedPreviewSizes.add(new IntegerAndIntegerPairParameter(128, 128));
        exportedPreviewSizes.add(new IntegerAndIntegerPairParameter(256, 256));
    }

    public static GeneralDataSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, GeneralDataSettings.class);
    }

    @SetJIPipeDocumentation(name = "Remember last results data importer for type",
            description = "If enabled, JIPipe will remember the last used results data importer as default.")
    @JIPipeParameter("auto-save-last-importer")
    public boolean isAutoSaveLastImporter() {
        return autoSaveLastImporter;
    }

    @JIPipeParameter("auto-save-last-importer")
    public void setAutoSaveLastImporter(boolean autoSaveLastImporter) {
        this.autoSaveLastImporter = autoSaveLastImporter;
    }

    @SetJIPipeDocumentation(name = "Remember last cache display for type",
            description = "If enabled, JIPipe will remember the last used cache display method as default.")
    @JIPipeParameter("auto-save-last-display")
    public boolean isAutoSaveLastDisplay() {
        return autoSaveLastDisplay;
    }

    @JIPipeParameter("auto-save-last-display")
    public void setAutoSaveLastDisplay(boolean autoSaveLastDisplay) {
        this.autoSaveLastDisplay = autoSaveLastDisplay;
    }

    @SetJIPipeDocumentation(name = "Preview size", description = "The width and height for data previews")
    @JIPipeParameter("preview-size")
    public int getPreviewSize() {
        return previewSize;
    }

    @JIPipeParameter("preview-size")
    public void setPreviewSize(int previewSize) {
        this.previewSize = previewSize;
    }

    @SetJIPipeDocumentation(name = "Generate previews in cache browser", description = "If enabled, cached items are previewed in JIPipe")
    @JIPipeParameter("generate-cache-previews")
    public boolean isGenerateCachePreviews() {
        return generateCachePreviews;
    }

    @JIPipeParameter("generate-cache-previews")
    public void setGenerateCachePreviews(boolean generateCachePreviews) {
        this.generateCachePreviews = generateCachePreviews;
    }

    @SetJIPipeDocumentation(name = "Generate previews in results", description = "If enabled, result items are previewed in JIPipe")
    @JIPipeParameter("generate-result-previews")
    public boolean isGenerateResultPreviews() {
        return generateResultPreviews;
    }

    @JIPipeParameter("generate-result-previews")
    public void setGenerateResultPreviews(boolean generateResultPreviews) {
        this.generateResultPreviews = generateResultPreviews;
    }

    @SetJIPipeDocumentation(name = "Max table column size", description = "The maximum width of data table columns. Set to -1 for no limit.")
    @JIPipeParameter("max-table-column-size")
    public int getMaxTableColumnSize() {
        return maxTableColumnSize;
    }

    @JIPipeParameter("max-table-column-size")
    public void setMaxTableColumnSize(int maxTableColumnSize) {
        this.maxTableColumnSize = maxTableColumnSize;
    }

    @SetJIPipeDocumentation(name = "Automatically remove outdated cached data", description = "If enabled, outdated cached data will be automatically removed to save memory")
    @JIPipeParameter("auto-remove-outdated-cached-data")
    public boolean isAutoRemoveOutdatedCachedData() {
        return autoRemoveOutdatedCachedData;
    }

    @JIPipeParameter("auto-remove-outdated-cached-data")
    public void setAutoRemoveOutdatedCachedData(boolean autoRemoveOutdatedCachedData) {
        this.autoRemoveOutdatedCachedData = autoRemoveOutdatedCachedData;
    }

    @SetJIPipeDocumentation(name = "Exported preview sizes", description = "The preview sizes to be exported on exporting data/results")
    @JIPipeParameter("exported-preview-sizes")
    @PairParameterSettings(keyLabel = "Width", valueLabel = "Height")
    public IntegerAndIntegerPairParameter.List getExportedPreviewSizes() {
        return exportedPreviewSizes;
    }

    @JIPipeParameter("exported-preview-sizes")
    public void setExportedPreviewSizes(IntegerAndIntegerPairParameter.List exportedPreviewSizes) {
        this.exportedPreviewSizes = exportedPreviewSizes;
    }
}
