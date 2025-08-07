package edu.kit.datamanager.ro_crate.entities.contextual;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

/**
 * This class represents a detailing provenance of entities.
 * 
 * Relevant specification link: <a href=
 * "https://www.researchobject.org/ro-crate/specification/1.1/provenance.html">Provenance
 * of entities</a>
 * 
 * @author sabrinechelbi
 */
public class ActionEntity extends ContextualEntity {

    public ActionEntity(AbstractActionEntityBuilder<?> entityBuilder) {
        super(entityBuilder);
        this.addType(entityBuilder.type.getName());
        this.addProperty("name", entityBuilder.name);
        this.addProperty("description", entityBuilder.description);

        this.addDateTimePropertyWithExceptions("startTime", entityBuilder.startTime);
        this.addDateTimePropertyWithExceptions("endTime", entityBuilder.endTime);

        entityBuilder.status.ifPresent(status -> this.addIdProperty("actionStatus", status.getId()));
        this.addIdProperty("agent", entityBuilder.agent);
        this.addIdListProperties("instrument", entityBuilder.instruments);
        this.addIdListProperties("result", entityBuilder.results);
        this.addIdListProperties("object", entityBuilder.objects);
    }

    abstract static class AbstractActionEntityBuilder<T extends AbstractActionEntityBuilder<T>>
            extends AbstractContextualEntityBuilder<T> {

        protected ActionType type;
        protected String description;
        protected String name;
        protected String startTime;
        protected String endTime;
        protected String agent;
        protected Optional<ActionStatus> status = Optional.empty();

        protected Collection<String> objects = new HashSet<>();
        protected Collection<String> results = new HashSet<>();
        protected Collection<String> instruments = new HashSet<>();

        protected AbstractActionEntityBuilder(ActionType type) {
            this.type = type;
        }

        public T setDescription(String description) {
            this.description = description;
            return self();
        }

        public T setName(String name) {
            this.name = name;
            return self();
        }

        public T setStartTime(String startTime) {
            this.startTime = startTime;
            return self();
        }

        public T setEndTime(String endTime) {
            this.endTime = endTime;
            return self();
        }

        public T setAgent(String agent) {
            this.agent = agent;
            return self();
        }

        public T setStatus(ActionStatus status) {
            this.status = Optional.ofNullable(status);
            return self();
        }

        /**
         * Same as calling {@link #addObject(String)} with each element of the
         * collection.
         * 
         * From the specification: "A curation Action MUST have at least one object
         * which associates it with either the root data entity Dataset or one of its
         * components."
         * 
         * @param objects see {@link #addObject(String)}
         * @return this builder
         */
        public T addObjects(Collection<String> objects) {
            this.objects.addAll(objects);
            return self();
        }

        /**
         * Adds a object to the collection of objects of this ActionEntity.
         * 
         * From the specification: "A curation Action MUST have at least one object
         * which associates it with either the root data entity Dataset or one of its
         * components."
         * 
         * @param object the object to add to this ActionEntity. Duplicates will be
         *               ignored/removed. "The object upon which the action is carried
         *               out, whose state is kept intact or changed. Also known as the
         *               semantic roles patient, affected or undergoer (which change
         *               their state) or theme (which doesn't). E.g. John read a book."
         *               (Schema.org definition)
         * @return this builder
         */
        public T addObject(String object) {
            this.objects.add(object);
            return self();
        }

        /**
         * Same as calling {@link #addResult(String)} with each element of the
         * collection.
         * 
         * @param results see {@link #addResult(String)}
         * @return this builder
         */
        public T addResults(Collection<String> results) {
            this.results.addAll(results);
            return self();
        }

        /**
         * Adds a result to the collection of results of this ActionEntity.
         * 
         * @param result the result to add to this ActionEntity. Duplicates will be
         *               ignored/removed. "The result produced in the action. E.g. John
         *               wrote a book." (Schema.org definition)
         * @return this builder
         */
        public T addResult(String result) {
            this.results.add(result);
            return self();
        }

        /**
         * Same as calling {@link #addInstrument(String)} with each element of the
         * collection.
         * 
         * @param instruments see {@link #addInstrument(String)}
         * @return this builder
         */
        public T addInstruments(Collection<String> instruments) {
            this.instruments.addAll(instruments);
            return self();
        }

        /**
         * Adds a instrument to the collection of instruments of this ActionEntity.
         * 
         * @param instrument the instrument to add to this ActionEntity. Duplicates will
         *                   be ignored/removed. "The object that helped the agent
         *                   perform the action. E.g. John wrote a book with a pen."
         *                   (Schema.org definition)
         * @return this builder
         */
        public T addInstrument(String instrument) {
            this.instruments.add(instrument);
            return self();
        }

        @Override
        public abstract ActionEntity build();
    }

    public static final class ActionEntityBuilder
            extends AbstractActionEntityBuilder<ActionEntityBuilder> {

        public ActionEntityBuilder(ActionType type) {
            super(type);
        }

        @Override
        public ActionEntityBuilder self() {
            return this;
        }

        @Override
        public ActionEntity build() {
            return new ActionEntity(this);
        }
    }
}
