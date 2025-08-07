package edu.kit.datamanager.ro_crate.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.google.common.collect.ImmutableList;
import edu.kit.datamanager.ro_crate.entities.data.RootDataEntity;
import edu.kit.datamanager.ro_crate.entities.serializers.ObjectNodeSerializer;
import edu.kit.datamanager.ro_crate.entities.validation.EntityValidation;
import edu.kit.datamanager.ro_crate.entities.validation.JsonSchemaValidation;
import edu.kit.datamanager.ro_crate.objectmapper.MyObjectMapper;
import edu.kit.datamanager.ro_crate.payload.Observer;
import edu.kit.datamanager.ro_crate.special.JsonUtilFunctions;
import edu.kit.datamanager.ro_crate.special.IdentifierUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract Entity parent class of every singe item in the json metadata file.
 *
 * @author Nikola Tzotchev on 3.2.2022 г.
 * @version 1
 */
public class AbstractEntity {

    /**
     * This set contains the types of an entity (ex. File, Dataset, ect.) It is
     * a set because it does not make sense to have duplicates
     */
    @JsonIgnore
    private Set<String> types;

    /**
     * Contains the whole list of properties of the entity It uses a custom
     * serializer because of cases where a single array element should be
     * displayed as a single value. ex: "key" : ["value"]
     * <=> "key" : "value"
     */
    @JsonUnwrapped
    @JsonSerialize(using = ObjectNodeSerializer.class)
    private ObjectNode properties;

    private static final EntityValidation entityValidation
            = new EntityValidation(new JsonSchemaValidation());

    @JsonIgnore
    private final Set<String> linkedTo;

    @JsonIgnore
    private final Set<Observer> observers;

    public void addObserver(Observer observer) {
        this.observers.add(observer);
    }

    private void notifyObservers() {
        this.observers.forEach(observer -> observer.update(this.getId()));
    }

    /**
     * Constructor that takes a builder and instantiates all the fields from it.
     *
     * @param entityBuilder the entity builder passed to the constructor.
     */
    public AbstractEntity(AbstractEntityBuilder<?> entityBuilder) {
        this.types = entityBuilder.types;
        this.properties = entityBuilder.properties;
        this.linkedTo = entityBuilder.relatedItems;
        this.observers = new HashSet<>();
        if (this.properties.get("@id") == null) {
            if (entityBuilder.id == null) {
                this.properties.put("@id", UUID.randomUUID().toString());
            } else {
                this.properties.put("@id", entityBuilder.id);
            }
        }
    }

    public Set<String> getLinkedTo() {
        return linkedTo;
    }

    /**
     * Returns the types of this entity.
     *
     * @return a set of type strings.
     */
    public Set<String> getTypes() {
        return types;
    }

    /**
     * Returns a Json object containing the properties of the entity.
     *
     * @return ObjectNode representing the properties.
     */
    public ObjectNode getProperties() {
        if (this.types != null) {
            JsonNode node = MyObjectMapper.getMapper().valueToTree(this.types);
            this.properties.set("@type", node);
        }
        return properties;
    }

    public JsonNode getProperty(String propertyKey) {
        return this.properties.get(propertyKey);
    }

    /**
     * Returns the value of the property with the given key as a String.
     * If the property is not found, it returns null.
     *
     * @param propertyKey the key of the property.
     * @return the value of the property as a String or null if not found.
     */
    public String getIdProperty(String propertyKey) {
        return Optional.ofNullable(this.properties.get(propertyKey))
                .map(jsonNode -> jsonNode.path("@id").asText(null))
                .orElse(null);
    }

    @JsonIgnore
    public String getId() {
        JsonNode id = this.properties.get("@id");
        return id == null ? null : id.asText();
    }

    /**
     * Set all the properties from a Json object to the Entity. The entities are
     * first validated to filter any invalid entity properties.
     *
     * @param obj the object that contains all the json properties that should
     * be added.
     */
    public void setProperties(JsonNode obj) {
        // validate whole entity
        if (entityValidation.entityValidation(obj)) {
            this.properties = obj.deepCopy();
            this.notifyObservers();
        }
    }

    protected void setId(String id) {
        this.properties.put("@id", id);
    }

    /**
     * removes one property from an entity.
     *
     * @param key the key of the entity, which will be removed.
     */
    public void removeProperty(String key) {
        this.getProperties().remove(key);
        this.notifyObservers();
    }

    /**
     * Removes a collection of properties from an entity.
     *
     * @param keys collection of keys, which will be removed.
     */
    public void removeProperties(Collection<String> keys) {
        this.getProperties().remove(keys);
        this.notifyObservers();
    }

    /**
     * Adds a property to the entity.
     * <p>
     * It may override values, if the key already exists.
     *
     * @param key the key of the property.
     * @param value value of the property.
     */
    public void addProperty(String key, String value) {
        if (key != null && value != null) {
            this.properties.put(key, value);
            this.notifyObservers();
        }
    }

    /**
     * Adds a property to the entity.
     * <p>
     * It may override values, if the key already exists.
     *
     * @param key the key of the property.
     * @param value value of the property.
     */
    public void addProperty(String key, long value) {
        if (key != null) {
            this.properties.put(key, value);
            this.notifyObservers();
        }
    }

    /**
     * Adds a property to the entity.
     * <p>
     * It may override values, if the key already exists.
     *
     * @param key the key of the property.
     * @param value value of the property.
     */
    public void addProperty(String key, double value) {
        if (key != null) {
            this.properties.put(key, value);
            this.notifyObservers();
        }
    }

    /**
     * Adds a generic property to the entity.
     * <p>
     * It may fail with an error message on stdout, in which case the
     * value is not added.
     * It may override values, if the key already exists.
     * This is the most generic way to add a property. The value is a
     * JsonNode that could contain anything possible. It is limited to
     * objects allowed to flattened documents, which means any literal,
     * an array of literals, or an object with an @id property.
     *
     * @param key   String key of the property.
     * @param value The JsonNode representing the value.
     */
    public void addProperty(String key, JsonNode value) {
        if (addProperty(this.properties, key, value)) {
            notifyObservers();
        }
    }

    private static boolean addProperty(ObjectNode whereToAdd, String key, JsonNode value) {
        boolean validInput = key != null && value != null;
        if (validInput && entityValidation.fieldValidation(value)) {
            whereToAdd.set(key, value);
            return true;
        }
        return false;
    }

    /**
     * Add a property that looks like this: "name" : {"@id" : "id"} If the
     * name property already exists add a second @id to it.
     *
     * @param name the "key" of the property.
     * @param id the "id" of the property.
     */
    public void addIdProperty(String name, String id) {
        if (id == null || id.isBlank()) { return; }
        mergeIdIntoValue(id, this.properties.get(name))
                .ifPresent(newValue -> this.properties.set(name, newValue));
        this.linkedTo.add(id);
        this.notifyObservers();
    }

    /**
     * Merges the given id into the current value,
     * using this representation: {"@id" : "id"}.
     * <p>
     * The current value can be null without errors.
     * Only the id will be considered in this case.
     * <p>
     * If the id is null-ish, it will not be added, similar to a null-ish value.
     * If the id is already present, nothing will be done.
     * If it is not an array and the id is not present, an array will be applied.
     *
     * @param id the id to add.
     * @param currentValue the current value of the property.
     * @return The updated value of the property.
     *               Empty if value does not change!
     */
    protected static Optional<JsonNode> mergeIdIntoValue(String id, JsonNode currentValue) {
        if (id == null || id.isBlank()) { return Optional.empty(); }

        ObjectMapper jsonBuilder = MyObjectMapper.getMapper();
        ObjectNode newIdObject = jsonBuilder.createObjectNode().put("@id", id);
        if (currentValue == null || currentValue.isNull() || currentValue.isMissingNode()) {
            return Optional.ofNullable(newIdObject);
        }

        boolean isIdAlready = currentValue.asText().equals(id);
        boolean isIdObjectAlready = currentValue.path("@id").asText().equals(id);
        boolean isArrayWithIdPresent = currentValue.isArray() && ImmutableList.copyOf(currentValue.elements()).stream()
                .anyMatch(node -> node
                        .path("@id")
                        .asText()
                        .equals(id));
        if (isIdAlready || isIdObjectAlready || isArrayWithIdPresent) {
            return Optional.empty();
        }

        if (currentValue.isArray() && currentValue instanceof ArrayNode currentValueAsArray) {
            currentValueAsArray.add(newIdObject);
            return Optional.of(currentValueAsArray);
        } else {
            // property is not an array, so we make it an array
            ArrayNode newNodes = jsonBuilder.createArrayNode();
            newNodes.add(currentValue);
            newNodes.add(newIdObject);
            return Optional.of(newNodes);
        }
    }

    /**
     * Adds everything from the properties to the property "name" as id.
     *
     * @param name the key of the property.
     * @param properties a collection containing all the id as String.
     */
    public void addIdListProperties(String name, Collection<String> properties) {
        ObjectMapper objectMapper = MyObjectMapper.getMapper();
        ArrayNode node = objectMapper.createArrayNode();
        if (this.properties.get(name) == null) {
            node = objectMapper.createArrayNode();
        } else {
            if (!this.properties.get(name).isArray()) {
                node.add(this.properties.get(name));
            }
        }
        for (String s : properties) {
            this.linkedTo.add(s);
            node.add(objectMapper.createObjectNode().put("@id", s));
        }
        if (node.size() == 1) {
            this.properties.set(name, node.get(0));
        } else {
            this.properties.set(name, node);
        }
        notifyObservers();
    }

    /**
     * Adding new type to the property (which may have multiple such ones).
     *
     * @param type the String representing the type.
     */
    public void addType(String type) {
        if (this.types == null) {
            this.types = new HashSet<>();
        }
        this.types.add(type);
        JsonNode node = MyObjectMapper.getMapper().valueToTree(this.types);
        this.properties.set("@type", node);
    }

    /**
     * Checks if the date matches the ISO 8601 date format.
     *
     * @param date the date as a string
     * @throws IllegalArgumentException if format does not match
     */
    private static void checkFormatISO8601(String date) throws IllegalArgumentException {
        String regex = "^([\\+-]?\\d{4}(?!\\d{2}\\b))((-?)((0[1-9]|1[0-2])(\\3([12]\\d|0[1-9]|3[01]))?|W([0-4]\\d|5[0-2])(-?[1-7])?|(00[1-9]|0[1-9]\\d|[12]\\d{2}|3([0-5]\\d|6[1-6])))([T\\s]((([01]\\d|2[0-3])((:?)[0-5]\\d)?|24\\:?00)([\\.,]\\d+(?!:))?)?(\\17[0-5]\\d([\\.,]\\d+)?)?([zZ]|([\\+-])([01]\\d|2[0-3]):?([0-5]\\d)?)?)?)?$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(date);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Date MUST be a string in ISO 8601 format");
        }
    }

    /**
     * Adds a property with date time format. The property should match the ISO 8601
     * date format.
     * <p>
     * Same as {@link #addProperty(String, String)} but with internal check.
     *
     * @param key   key of the property (e.g. datePublished)
     * @param value time string in ISO 8601 format
     * @throws IllegalArgumentException if format is not ISO 8601
     */
    public void addDateTimePropertyWithExceptions(String key, String value) throws IllegalArgumentException {
        if (value != null) {
            checkFormatISO8601(value);
            this.properties.put(key, value);
            this.notifyObservers();
        }
    }

    /**
     * This a builder inner class that should allow for an easier creating of
     * entities.
     *
     * @param <T> The type of the child builders so that they to can use the
     * methods provided here.
     */
    public abstract static class AbstractEntityBuilder<T extends AbstractEntityBuilder<T>> {

        private Set<String> types;
        protected Set<String> relatedItems;
        private ObjectNode properties;
        private String id;

        protected AbstractEntityBuilder() {
            this.properties = MyObjectMapper.getMapper().createObjectNode();
            this.relatedItems = new HashSet<>();
        }

        protected String getId() {
            return this.id;
        }

        /**
         * Setting the id property of the entity, if the given value is not
         * null. If the id is not encoded, the encoding will be done.
         * <p>
         * <b>NOTE: IDs are not just names!</b> The ID may have effects
         * on parts of your crate! For example: If the entity represents a
         * file which will be copied into the crate, writers must use the
         * ID as filename.
         *
         * @param id the String representing the id.
         * @return the generic builder.
         */
        public T setId(String id) {
            if (id != null && !id.equals(RootDataEntity.ID)) {
                if (IdentifierUtils.isValidUri(id)) {
                    this.id = id;
                } else {
                    this.id = IdentifierUtils.encode(id).orElse(this.id);
                }
            }
            return self();
        }

        /**
         * Adding a type to the builder types.
         *
         * @param type the type to add.
         * @return the generic builder.
         */
        public T addType(String type) {
            if (this.types == null) {
                this.types = new HashSet<>();
            }
            this.types.add(type);
            return self();
        }

        /**
         * Adds multiple types in one call.
         *
         * @param types the types to add.
         * @return the generic builder.
         */
        public T addTypes(Collection<String> types) {
            if (this.types == null) {
                this.types = new HashSet<>();
            }
            this.types.addAll(types);
            return self();
        }

        /**
         * Adds a property with date time format. The property should match the ISO 8601
         * date format.
         * <p>
         * Same as {@link #addProperty(String, String)} but with internal check.
         *
         * @param key   key of the property (e.g. datePublished)
         * @param value time string in ISO 8601 format
         * @return this builder
         * @throws IllegalArgumentException if format is not ISO 8601
         */
        public T addDateTimePropertyWithExceptions(String key, String value) throws IllegalArgumentException {
            if (value != null) {
                checkFormatISO8601(value);
                this.properties.put(key, value);
            }
            return self();
        }

        /**
         * Adding a property to the builder.
         *
         * @param key the key of the property in a string.
         * @param value the JsonNode value of te property.
         * @return the generic builder.
         */
        public T addProperty(String key, JsonNode value) {
            if (AbstractEntity.addProperty(this.properties, key, value)) {
                this.relatedItems.addAll(JsonUtilFunctions.getIdPropertiesFromProperty(value));
            }
            return self();
        }

        /**
         * Adding a property to the builder.
         *
         * @param key the key of the property as a string.
         * @param value the value of the property as a string.
         * @return the generic builder.
         */
        public T addProperty(String key, String value) {
            this.properties.put(key, value);
            return self();
        }

        public T addProperty(String key, int value) {
            this.properties.put(key, value);
            return self();
        }

        public T addProperty(String key, double value) {
            this.properties.put(key, value);
            return self();
        }

        public T addProperty(String key, boolean value) {
            this.properties.put(key, value);
            return self();
        }

        /**
         * ID properties are often used when referencing other entities within
         * the ROCrate. This method adds automatically such one.
         * <p>
         * Instead of {@code "name": "id" }
         * this will add {@code "name" : {"@id": "id"} }
         * 
         * Does nothing if name or id are null.
         *
         * @param name the name of the ID property.
         * @param id the ID.
         * @return the generic builder
         */
        public T addIdProperty(String name, String id) {
            AbstractEntity.mergeIdIntoValue(id, this.properties.get(name))
                    .ifPresent(newValue -> {
                        this.properties.set(name, newValue);
                        this.relatedItems.add(id);
                    });
            return self();
        }

        /**
         * This is another way of adding the ID property, this time the whole
         * other Entity is provided.
         *
         * @param name the name of the property.
         * @param entity the other entity that is referenced.
         * @return the generic builder.
         */
        public T addIdProperty(String name, AbstractEntity entity) {
            if (entity != null) {
                return addIdProperty(name, entity.getId());
            }
            return self();
        }

        /**
         * This adds multiple id entities to a single key.
         *
         * @param name the name of the property.
         * @param entities the Collection containing the multiple entities.
         * @return the generic builder.
         */
        public T addIdFromCollectionOfEntities(String name, Collection<AbstractEntity> entities) {
            if (entities != null) {
                for (var e : entities) {
                    addIdProperty(name, e);
                }
            }
            return self();
        }

        /**
         * Deprecated. Equivalent to {@link #setAllIfValid(ObjectNode)}.
         *
         * @param properties the Json representing all the properties.
         * @return the generic builder, either including all given properties
         *          * or unchanged.
         *
         * @deprecated To enforce the user know what this method does,
         *   we want the user to use one of the more explicitly named
         *   methods {@link #setAllIfValid(ObjectNode)} or
         *   {@link #setAllIfValid(ObjectNode)}.
         * @see #setAllIfValid(ObjectNode)
         */
        @Deprecated(since = "2.1.0", forRemoval = true)
        public T setAll(ObjectNode properties) {
            return setAllIfValid(properties);
        }

        /**
         * This sets everything from a json object to the property,
         * <b>if the result is valid</b>. Otherwise, it will do <b>nothing</b>.
         * <p>
         * Valid means here that the json object needs to be flat as specified
         * in the RO-Crate specification. In principle, this means that
         * primitives and objects referencing an ID are allowed,
         * as well as arrays of these.
         *
         * @param properties the Json representing all the properties.
         * @return the generic builder, either including all given properties
         * or unchanged.
         */
        public T setAllIfValid(ObjectNode properties) {
            if (AbstractEntity.entityValidation.entityValidation(properties)) {
                this.properties = properties;
                this.relatedItems.addAll(JsonUtilFunctions.getIdPropertiesFromJsonNode(properties));
            }
            return self();
        }

        /**
         * This sets everything from a json object to the property. Can be
         * useful when the entity is already available somewhere.
         * <p>
         * Errors on validation are printed, but everything will be added.
         * For more about validation, see {@link #setAllIfValid(ObjectNode)}.
         *
         * @param properties the Json representing all the properties.
         * @return the generic builder with all properties added.
         */
        public T setAllUnsafe(ObjectNode properties) {
            // This will currently only print errors.
            AbstractEntity.entityValidation.entityValidation(properties);
            this.properties = properties;
            this.relatedItems.addAll(JsonUtilFunctions.getIdPropertiesFromJsonNode(properties));
            return self();
        }

        public abstract T self();

        public abstract AbstractEntity build();
    }

}
