package edu.kit.datamanager.ro_crate.entities.contextual;

/**
 * Enumeration class representing action types.
 * @author sabrinechelbi
 */
public enum ActionType {

    CREATE("CreateAction"),
    UPDATE("UpdateAction");

    private final String name;

    ActionType(String name) {
        this.name = name;
    }
    
    public String getName(){
        return name;
    }
}
