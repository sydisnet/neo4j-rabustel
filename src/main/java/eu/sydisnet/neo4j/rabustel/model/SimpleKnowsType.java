package eu.sydisnet.neo4j.rabustel.model;

import org.neo4j.graphdb.RelationshipType;

/**
 * Created by shebert on 30/03/14.
 */
public enum SimpleKnowsType implements RelationshipType {
    KNOWS("Connait");

    private String display;

    SimpleKnowsType(final String display) {
        this.display = display;
    }


    @Override
    public String toString() {
        return display;
    }
}
