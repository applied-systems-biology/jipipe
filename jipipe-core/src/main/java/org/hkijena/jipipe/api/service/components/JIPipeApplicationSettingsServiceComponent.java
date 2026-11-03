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

package org.hkijena.jipipe.api.service.components;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;
import org.hkijena.jipipe.api.settings.JIPipeApplicationSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeSettingKey;
import org.hkijena.jipipe.api.settings.JIPipeSettingsSheet;
import org.hkijena.jipipe.api.settings.SettingsDocument;
import org.hkijena.jipipe.api.settings.SettingsFileStore;
import org.hkijena.jipipe.api.settings.SettingsKind;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JIPipePathMetadataStore;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Legacy facade for application settings: the sheet registry plus the
 * registry-database API.
 * <p>
 * <b>This component no longer stores anything itself.</b> Since the modern
 * settings system (Task 13 of the 2026 settings redesign) every value lives in
 * the {@link SettingsScope#APPLICATION} document owned by
 * {@link JIPipeSettingsServiceComponent}:
 * <ul>
 * <li>{@link #register(JIPipeApplicationSettingsSheet)} keeps the lookup maps
 * ({@link #getById}, {@link #getByType}, {@link #getRegisteredSheets}) and
 * forwards the sheet to {@link JIPipeSettingsServiceComponent#attachSheet}, so
 * its values are projected onto the {@code "sheet:<id>"} block.</li>
 * <li>{@link #putIntoRegistry}, {@link #getFromRegistry} and
 * {@link #getListFromRegistry} read and write the {@code "registry:<db>"}
 * block.</li>
 * <li>{@link #save()}, {@link #saveLater()} and {@link #reload()} delegate to
 * the same component.</li>
 * </ul>
 * The signatures are unchanged, so every existing caller keeps working.
 * <p>
 * <b>No per-call bindings.</b> A {@code SettingsBinding} registers a path
 * listener on the (long-lived) document and must be closed; creating one per
 * registry read would leak one listener per call (Task 5 review). The registry
 * methods therefore use the document's own get/set API, and they register the
 * {@code registry:<db>} block once per component instance through
 * {@link JIPipeSettingsServiceComponent#registerKey}, so its changes are
 * observed and re-emitted like any other setting's.
 * <p>
 * <b>File formats.</b> {@link #getSheetsFile(boolean)} still names
 * {@code settings.json}, which now holds the scoped document
 * ({@code $schema}/{@code $meta} plus prefixed blocks) instead of the legacy
 * flat sheet map; {@link #getRegistryFile(String, boolean)} no longer names a
 * file on its own, because all registry databases live in that one document —
 * <b>there is no per-database file any more</b>: every registry database is a
 * {@code registry:<key>} block of {@code settings.json}, and
 * {@code getRegistryFile} exists only for signature compatibility, returning
 * that file for every database key.
 */
public final class JIPipeApplicationSettingsServiceComponent extends JIPipeServiceComponent {
    private static final Logger logger = LoggerFactory.getLogger(JIPipeApplicationSettingsServiceComponent.class);
    private static final String REGISTRY_PREFIX = SettingsKind.REGISTRY.prefix() + ":";
    private static final String SHEET_PREFIX = SettingsKind.SHEET.prefix() + ":";

    private final BiMap<String, JIPipeApplicationSettingsSheet> registeredSheets = HashBiMap.create();
    private final Map<Class<? extends JIPipeApplicationSettingsSheet>, JIPipeApplicationSettingsSheet> registeredSheetsByType = new HashMap<>();
    /**
     * Registry block paths ({@code "registry:<db>"}) already registered with
     * the settings service, so a block is observed exactly once.
     */
    private final Map<String, Boolean> observedRegistryBlocks = new LinkedHashMap<>();
    private final ChangedEventEmitter changedEventEmitter = new ChangedEventEmitter();

    public JIPipeApplicationSettingsServiceComponent(JIPipeService service) {
        super(service);
    }

    /**
     * Subscribes the legacy aggregate emitter to the new component's aggregate
     * event once the service is fully constructed, so every pre-existing
     * subscriber keeps receiving changes (sheet edits, registry writes,
     * external file changes). Not done in the constructor: this component is
     * created before the settings component is assigned on the service, so a
     * constructor-time call would re-enter the initialization.
     */
    @Override
    public void postprocess(JIPipeProgressInfo progressInfo) {
        getSettingsService().getLegacyChangedEventEmitter()
                .subscribeLambda((emitter, event) -> changedEventEmitter.emit(new ChangedEvent(this)));
    }

    /**
     * Gets the raw {@code sheet:<id>} blocks of the application settings file
     * as they are on disk, keyed by sheet ID (the block prefix is stripped, so
     * the result has the shape of the legacy flat settings file).
     * <p>
     * Works before settings sheets are registered and before the JIPipe
     * service is initialized (splash screen, early startup), so it reads the
     * file directly.
     * <p>
     * <b>Contract — preferred before the profile migration has run.</b> The
     * file is read first and is <em>authoritative</em> whenever it is parseable:
     * <ul>
     * <li>new-format document ({@code $schema} present) → its {@code sheet:}
     * blocks, readable;</li>
     * <li>legacy 5.x flat file (unprefixed sheet IDs) → an <b>empty</b> object:
     * the values are not visible here because the 5.x profile has not been
     * migrated yet, and the caller must treat that as "defaults". After the
     * migration (which runs early in startup, before contexts that read this)
     * the same file yields the values.</li>
     * </ul>
     * The in-memory document is consulted <em>only</em> when the file is
     * absent, or when it is not parseable <em>and</em>
     * {@code JIPipe.getSettingsService().document(APPLICATION)} has already
     * been materialized — never to paper over the legacy shape (the document
     * would be the pre-migration state, so mixing the two would make the
     * result depend on whether anything materialized the document yet). It
     * must never <em>materialize</em> the document itself: doing so during the
     * startup would load the profile directory at that instant, and a
     * {@code --override-profile} run would then be split-brained between the
     * override profile (read here and migrated by the initializer) and the real
     * profile (loaded into the settings component).
     *
     * @return the node keyed by sheet ID. Never null.
     */
    public static JsonNode getRawSheetsNode() {
        Path propertyFile = getSheetsFile(false);
        if (Files.isRegularFile(propertyFile)) {
            JsonNode node;
            try {
                node = JsonUtils.getObjectMapper().readTree(propertyFile.toFile());
            } catch (Exception e) {
                logger.debug("Could not read the raw settings file {}; falling back to the in-memory document",
                        propertyFile, e);
                return inMemoryRawSheetsNode();
            }
            if (node != null && node.isObject()) {
                return sheetBlocks(node);
            }
            logger.debug("The raw settings file {} is not a JSON object; falling back to the in-memory document",
                    propertyFile);
        }
        return inMemoryRawSheetsNode();
    }

    /**
     * @return the {@code sheet:<id>} blocks of the in-memory application
     * document <em>if it is already materialized</em>, or {@link MissingNode}
     * when there is none. Never triggers the first load: that would resolve the
     * profile directory during the startup, before a {@code --override-profile}
     * override is authoritative (see {@link #getRawSheetsNode()}).
     */
    private static JsonNode inMemoryRawSheetsNode() {
        if (!JIPipe.isInstantiated() || JIPipe.getInstance() == null) {
            return MissingNode.getInstance();
        }
        try {
            SettingsDocument doc = JIPipe.getInstance().getSettings().materializedDocument(SettingsScope.APPLICATION);
            if (doc == null) {
                return MissingNode.getInstance();
            }
            return sheetBlocks(doc.snapshotJson());
        } catch (RuntimeException e) {
            logger.debug("Could not read the raw settings from the in-memory document", e);
            return MissingNode.getInstance();
        }
    }

    /**
     * @param documentRoot a settings document's root
     * @return a copy holding only the {@code sheet:} blocks, keyed by sheet ID
     * (legacy flat shape used by the raw readers)
     */
    private static ObjectNode sheetBlocks(JsonNode documentRoot) {
        return rawSheetsOf(documentRoot);
    }

    /**
     * The <em>file-shape contract</em> of the raw settings reader, extracted so
     * it can be pinned without a profile or a JIPipe service: a settings
     * document's {@code sheet:} blocks, keyed by sheet ID (the legacy flat
     * shape).
     * <p>
     * A legacy 5.x flat document has no {@code sheet:} blocks, so the result is
     * <b>empty</b> — before the profile migration has run, raw readers see
     * defaults. A non-object document yields an empty object.
     *
     * @param documentRoot a settings document's root (may be {@code null})
     * @return a copy holding only the {@code sheet:} blocks, keyed by sheet ID
     */
    static ObjectNode rawSheetsOf(JsonNode documentRoot) {
        ObjectNode result = JsonUtils.getObjectMapper().createObjectNode();
        if (documentRoot == null || !documentRoot.isObject()) {
            return result;
        }
        documentRoot.fields().forEachRemaining(entry -> {
            if (entry.getKey().startsWith(SHEET_PREFIX)) {
                result.set(entry.getKey().substring(SHEET_PREFIX.length()), entry.getValue());
            }
        });
        return result;
    }

    /**
     * Return the global settings file.
     *
     * @param loadFromOldProfile if the settings can be loaded from an old profile
     * @return the path
     */
    public static Path getSheetsFile(boolean loadFromOldProfile) {
        return JIPipe.getJIPipeUserDir(loadFromOldProfile).resolve("settings.json");
    }

    /**
     * Returns the file the registry data is persisted to.
     * <p>
     * <b>There is no per-database file any more.</b> Every registry database
     * lives as a {@code registry:<key>} block inside the single application
     * settings document ({@link #getSheetsFile(boolean)}); no
     * {@code settings-db-<key>.json} file is written, read or migrated from
     * here (the 5.x migration reads those files once, in
     * {@code SettingsMigration}). This method exists only for signature
     * compatibility with pre-6.0 callers and always returns that document's
     * file, regardless of {@code databaseKey}.
     *
     * @param databaseKey        the database key; only validated, it does not
     *                           select a file
     * @param loadFromOldProfile if the file of an older profile may be returned
     * @return the application settings file (the same for every database key)
     */
    public static Path getRegistryFile(String databaseKey, boolean loadFromOldProfile) {
        if (!isValidRegistryDatabaseKey(databaseKey)) {
            throw new IllegalArgumentException("Invalid database key: " + databaseKey);
        }
        return getSheetsFile(loadFromOldProfile);
    }

    public static boolean isValidRegistryDatabaseKey(String databaseKey) {
        return databaseKey.equals(databaseKey.toLowerCase(Locale.ROOT)) && StringUtils.isFilesystemCompatible(databaseKey);
    }

    /**
     * Registers a new settings sheet. The sheet's values are projected onto
     * the {@code "sheet:<id>"} block of the application settings document (the
     * bridge is attached by the settings service).
     *
     * @param sheet the sheet
     */
    public void register(JIPipeApplicationSettingsSheet sheet) {
        if (StringUtils.isNullOrEmpty(sheet.getId())) {
            throw new IllegalArgumentException("Invalid ID for settings sheet " + sheet);
        }
        if (StringUtils.isNullOrEmpty(sheet.getIcon())) {
            throw new IllegalArgumentException("Invalid icon for settings sheet " + sheet);
        }
        if (StringUtils.isNullOrEmpty(sheet.getCategory()) || sheet.getCategoryIcon() == null) {
            throw new IllegalArgumentException("Invalid category for settings sheet " + sheet);
        }
        registeredSheets.put(sheet.getId(), sheet);
        registeredSheetsByType.put(sheet.getClass(), sheet);
        getSettingsService().attachSheet(sheet);
        getProgressInfo().log("Registered application settings sheet id=" + sheet.getId() + " in category '" + sheet.getCategory() + "' object=" + sheet);
    }

    /**
     * Gets the settings instance with given ID
     *
     * @param id    the ID
     * @param klass the settings class
     * @param <T>   the settings class
     * @return the settings instance.
     */
    public <T extends JIPipeSettingsSheet> T getById(String id, Class<T> klass) {
        JIPipeApplicationSettingsSheet sheet = registeredSheets.getOrDefault(id, null);
        if (sheet != null) {
            return (T) sheet;
        } else {
            return null;
        }
    }

    /**
     * Gets the settings instance with given ID
     *
     * @param klass the settings class
     * @param <T>   the settings class
     * @return the settings instance.
     */
    public <T extends JIPipeApplicationSettingsSheet> T getByType(Class<T> klass) {
        JIPipeApplicationSettingsSheet sheet = registeredSheetsByType.getOrDefault(klass, null);
        if (sheet != null) {
            return (T) sheet;
        } else {
            return null;
        }
    }

    public BiMap<String, JIPipeApplicationSettingsSheet> getRegisteredSheets() {
        return ImmutableBiMap.copyOf(registeredSheets);
    }

    /**
     * Saves the settings: the registered sheets are flushed into the
     * application settings document and every file-backed document is written
     * atomically. Delegates to {@link JIPipeSettingsServiceComponent#save()}.
     */
    public void save() {
        getSettingsService().save();
    }

    /**
     * Coalesces rapid changes into a single save after a short quiet period.
     * Delegates to {@link JIPipeSettingsServiceComponent#saveLater()}.
     */
    public void saveLater() {
        getSettingsService().saveLater();
    }

    /**
     * Reloads the settings from the default file if it exists and re-attaches
     * every sheet bridge to the fresh document, so the registered sheet
     * instances show the reloaded values.
     */
    public void reload() {
        getSettingsService().reload();
    }

    /**
     * Puts an object into the registry.
     * <p>
     * The value is written to the {@code <key>} sub-path of the
     * {@code "registry:<databaseKey>"} block of the application settings
     * document. Like before, the write is debounced ({@link #saveLater()}).
     *
     * @param databaseKey the database key
     * @param registryKey the registry key
     * @param value       the value; {@code null} removes the entry
     */
    public void putIntoRegistry(String databaseKey, JIPipePathMetadataStore.Key registryKey, Object value) {
        if (!isValidRegistryDatabaseKey(databaseKey)) {
            throw new IllegalArgumentException("Invalid database key: " + databaseKey);
        }
        String blockPath = REGISTRY_PREFIX + databaseKey;
        String subPath = subPathOf(registryKey);
        SettingsDocument document = getSettingsService().document(SettingsScope.APPLICATION);
        registerRegistryBlock(blockPath, subPath);
        if (value == null) {
            document.remove(blockPath, subPath);
        } else {
            document.set(blockPath, subPath, valueToTree(value));
        }
        saveLater();
    }

    /**
     * Gets an object from the registry.
     * <p>
     * Reads the {@code <key>} sub-path of the {@code "registry:<databaseKey>"}
     * block of the application settings document and converts it to
     * {@code type}. The {@code destructive} flag keeps its exact historic
     * meaning, which is two rules and not one:
     * <ul>
     * <li>an <em>absent</em> location is always seeded with the default, for
     * both flag values (the legacy store wrote whenever the key was
     * missing);</li>
     * <li>an <em>existing</em> location whose value yields {@code null}
     * (a stored JSON null, or a value that cannot be converted to
     * {@code type}) is rewritten with the default only when
     * {@code destructive} is {@code true}; with {@code false} it is left
     * untouched.</li>
     * </ul>
     *
     * @param databaseKey  the database key
     * @param registryKey  the registry key
     * @param type         the type
     * @param defaultValue the default value
     * @param destructive  if true and the current value is null, always replace it with the default value
     * @param <T>          the type
     */
    public <T> T getFromRegistry(String databaseKey, JIPipePathMetadataStore.Key registryKey, Class<T> type, T defaultValue, boolean destructive) {
        if (!isValidRegistryDatabaseKey(databaseKey)) {
            throw new IllegalArgumentException("Invalid database key: " + databaseKey);
        }
        String blockPath = REGISTRY_PREFIX + databaseKey;
        String subPath = subPathOf(registryKey);
        SettingsDocument document = getSettingsService().document(SettingsScope.APPLICATION);
        registerRegistryBlock(blockPath, subPath);

        JsonNode node = document.get(blockPath, subPath);
        T value = null;
        if (!node.isMissingNode() && !node.isNull()) {
            value = convert(blockPath, subPath, node, type);
        }
        if (value == null) {
            value = defaultValue;
            // destructive: rewrite the location even if a value exists but did
            // not convert; otherwise only a genuinely absent location is seeded
            if (destructive) {
                writeRegistryValue(document, blockPath, subPath, value);
                saveLater();
            } else if (!document.has(blockPath, subPath)) {
                writeRegistryValue(document, blockPath, subPath, value);
                saveLater();
            }
        }
        return value;
    }

    /**
     * Writes a registry value back into the document, or removes the location
     * when the value is {@code null}.
     */
    private static void writeRegistryValue(SettingsDocument document, String blockPath, String subPath, Object value) {
        if (value == null) {
            document.remove(blockPath, subPath);
        } else {
            document.set(blockPath, subPath, valueToTree(value));
        }
    }

    /**
     * Gets a list object from the registry.
     * <p>
     * Reads the {@code <key>} sub-path of the {@code "registry:<databaseKey>"}
     * block of the application settings document as a list. The default is an
     * empty list; the {@code destructive} flag keeps its historic meaning (see
     * {@link #getFromRegistry(String, JIPipePathMetadataStore.Key, Class, Object, boolean)}).
     *
     * @param databaseKey the database key
     * @param registryKey the registry key
     * @param type        the type
     * @param destructive if true and the current value is null, always replace it with the default value
     * @param <T>         the type
     */
    public <T> List<T> getListFromRegistry(String databaseKey, JIPipePathMetadataStore.Key registryKey, Class<T> type, boolean destructive) {
        if (!isValidRegistryDatabaseKey(databaseKey)) {
            throw new IllegalArgumentException("Invalid database key: " + databaseKey);
        }
        String blockPath = REGISTRY_PREFIX + databaseKey;
        String subPath = subPathOf(registryKey);
        SettingsDocument document = getSettingsService().document(SettingsScope.APPLICATION);
        registerRegistryBlock(blockPath, subPath);

        List<T> object = null;
        JsonNode node = document.get(blockPath, subPath);
        if (node.isArray()) {
            try {
                object = JsonUtils.getObjectMapper().readerForListOf(type).readValue(node);
            } catch (Exception e) {
                object = null;
            }
        } else if (!node.isMissingNode() && !node.isNull()) {
            // A single value where a list is expected (e.g. "a" for ["a"]):
            // keep the legacy tolerance instead of dropping the stored data.
            T single = convert(blockPath, subPath, node, type);
            if (single != null) {
                object = new ArrayList<>();
                object.add(single);
            }
        }
        if (object == null) {
            object = new ArrayList<>();
            if (destructive || !document.has(blockPath, subPath)) {
                document.set(blockPath, subPath, JsonUtils.getObjectMapper().valueToTree(object));
                saveLater();
            }
        }
        return object;
    }

    /**
     * Registers a {@code registry:<db>} block and one sub-path with the
     * settings service exactly once, so its changes are observed and re-emitted
     * as change events. Registering a key never creates a binding (and with it
     * a path listener per call) — see the class documentation.
     *
     * @param blockPath the registry block path
     * @param subPath   the sub-path read or written
     */
    private void registerRegistryBlock(String blockPath, String subPath) {
        JIPipeSettingsServiceComponent settings = getSettingsService();
        if (subPath == null || subPath.isEmpty()) {
            return;
        }
        String location = blockPath + "/" + subPath;
        if (observedRegistryBlocks.put(location, Boolean.TRUE) != null) {
            return;
        }
        settings.registerKey(JIPipeSettingKey.object(blockPath, subPath, JsonNode.class));
    }

    /**
     * @param registryKey the registry key
     * @return the slash-separated sub-path within the registry block
     */
    private static String subPathOf(JIPipePathMetadataStore.Key registryKey) {
        Objects.requireNonNull(registryKey, "registryKey");
        return String.join("/", registryKey.getItems());
    }

    /**
     * Converts a stored value to the requested type. A value that cannot be
     * converted yields {@code null}, which makes the caller fall back to the
     * default exactly like the legacy store did.
     */
    private static <T> T convert(String blockPath, String subPath, JsonNode node, Class<T> type) {
        try {
            if (type == JsonNode.class) {
                @SuppressWarnings("unchecked")
                T asNode = (T) node;
                return asNode;
            }
            T value = JsonUtils.getObjectMapper().treeToValue(node, type);
            if (value == null && type == String.class && node.isContainerNode()) {
                // The legacy store serialized non-string objects to their JSON
                // text when a String was requested.
                @SuppressWarnings("unchecked")
                T text = (T) JsonUtils.toJsonString(node);
                return text;
            }
            return value;
        } catch (Exception e) {
            logger.debug("Could not convert registry value at {}/{} to {}", blockPath, subPath, type, e);
            return null;
        }
    }

    /**
     * Converts a value to its stored JSON form. {@link JsonNode} values are
     * stored as they are (they are copied, so a caller mutating its node does
     * not mutate the document).
     */
    private static JsonNode valueToTree(Object value) {
        if (value instanceof JsonNode node) {
            return node.deepCopy();
        }
        return JsonUtils.getObjectMapper().valueToTree(value);
    }

    /**
     * @return the modern settings component every value is stored in
     */
    private JIPipeSettingsServiceComponent getSettingsService() {
        JIPipeService service = getService();
        if (service != null) {
            return service.getSettings();
        }
        // Only reachable from a test-constructed component without a service.
        JIPipeService instance = JIPipe.getInstance();
        return instance.getSettings();
    }

    public ChangedEventEmitter getChangedEventEmitter() {
        return changedEventEmitter;
    }

    public interface ChangedEventListener {
        void onApplicationSettingsChanged();
    }

    public static class ChangedEvent extends AbstractJIPipeEvent {
        public ChangedEvent(Object source) {
            super(source);
        }
    }

    public static class ChangedEventEmitter extends JIPipeEventEmitter<ChangedEvent, ChangedEventListener> {

        @Override
        protected void call(ChangedEventListener changedEventListener, ChangedEvent event) {
            changedEventListener.onApplicationSettingsChanged();
        }
    }
}