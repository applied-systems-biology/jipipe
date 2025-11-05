package org.hkijena.jipipe.api.data.documentation;

import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class JIPipeDataCrateMetadataEntry {
    private List<String> encodingFormat = new ArrayList<>();
    private String description = "";
    private String name = "";
    private JIPipeDataCrateEntityType type;
    private String id;
    private JIPipeDataCrateEntityPresence presence = JIPipeDataCrateEntityPresence.Mandatory;

    public JIPipeDataCrateMetadataEntry() {
    }

    /**
     * The unique ID of the entity within the data container.
     * <p>
     * In the case of type=File
     * MUST be either a fully resolved path to a file, relative to the root of the data container (according to RO-Crate 1.2)
     * OR can be also a glob by using the glob: protocol for matching the FIRST file that matches the GLOB operation.
     * OR can be also a regex by using the regex: protocol for matching the FIRST file that matches the regular expression.
     * OR can be a path spec where variables/placeholders are defined through {variable_name} using the path: protocol.
     * MUST begin with a ./ (excluding protocol)
     * </p>
     * <p>
     * In the case of type=Dataset (i.e., a directory)
     * MUST be either a fully resolved path to a file, relative to the root of the data container (according to RO-Crate 1.2)
     * OR can be also a glob by using the glob: protocol for matching the FIRST directory that matches the GLOB operation.
     * OR can be also a regex by using the regex: protocol for matching the FIRST directory that matches the regular expression.
     * OR can be a path spec where variables/placeholders are defined through {variable_name} using the path: protocol.
     * MUST end with a /
     * MUST begin with a ./ (excluding protocol)
     * </p>
     * <p>
     * Examples:
     *     <ul>
     *         <li>./table.csv</li>
     *         <li>glob:./*.csv</li>
     *         <li>path:./{name}-{role}.csv</li>
     *         <li>regex:\\./image.*\\.(png|tif|bmp)</li>
     *         <li>./directory/</li>
     *         <li>path:./directory{num}/</li>
     *     </ul>
     * </p>
     *
     * @return the entity ID
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the entity type
     *
     * @return the entity type
     */
    public JIPipeDataCrateEntityType getType() {
        return type;
    }

    public void setType(JIPipeDataCrateEntityType type) {
        this.type = type;
    }

    /**
     * Human-readable name. Can be different from ID.
     *
     * @return the human-readable name
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Human-readable description
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * The encoding format. CAN be a MIME-Type or a URL. See <a href="https://schema.org/encodingFormat">schema.org documentation</a>.
     * You can use the {@link EncodingFormats} constants to access common MIME-Types.
     *
     * @return the encoding format
     */
    public List<String> getEncodingFormat() {
        return encodingFormat;
    }

    public void setEncodingFormat(List<String> encodingFormat) {
        this.encodingFormat = encodingFormat;
    }

    /**
     * Returns the protocol of the ID. For example, returns "glob" for an id "glob:./*.png"
     *
     * @return the protocol. Returns null if the ID is invalid.
     */
    public String getIdProtocol() {
        // It's fine like that, because we don't expect standard paths to have :
        if (!id.contains(":")) {
            return "";
        }
        return id.substring(0, id.indexOf(":"));
    }

    public String getIdPath() {
        // It's fine like that, because we don't expect standard paths to have :
        if (!id.contains(":")) {
            return id;
        }
        return id.substring(id.indexOf(":") + 1);
    }

    public boolean isValid() {
        if (StringUtils.isNullOrEmpty(id)) {
            return false;
        }
        if (StringUtils.isNullOrEmpty(getIdPath())) {
            return false;
        }
        String proto = getIdProtocol();
        if (proto == null) {
            return false;
        }
        if (!Set.of("", "glob", "regex", "path").contains(proto)) {
            return false;
        }
        if (type == null) {
            return false;
        }

        return true;
    }

    public JIPipeDataCrateEntityPresence getPresence() {
        return presence;
    }

    public void setPresence(JIPipeDataCrateEntityPresence presence) {
        this.presence = presence;
    }
}
