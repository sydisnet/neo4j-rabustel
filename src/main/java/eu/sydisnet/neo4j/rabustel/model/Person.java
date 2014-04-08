package eu.sydisnet.neo4j.rabustel.model;

/*
 * #%L
 * Rabustel
 * %%
 * Copyright (C) 2014 Sébastien Hébert - Twitter: @sydisnet
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.neo4j.graphdb.Node;

import javax.json.Json;
import javax.json.JsonWriter;
import java.io.StringWriter;

/**
 * Model: Person.
 * <p>
 * Created by shebert on 16/03/14.
 */
public class Person {

    public static final String LABEL = "Personne";

    public static final String NAME = "nom";
    static final String ORIGIN = "origine";
    static final String JURIST = "legiste";

    private final Node underlyingNode;

    public Person(final Node underlyingNode) {
        this.underlyingNode = underlyingNode;
    }

    public Node getUnderlyingNode() {
        return underlyingNode;
    }

    public String getName() {
        return (String) underlyingNode.getProperty(NAME);
    }

    public void setName(final String name) {
        underlyingNode.setProperty(NAME, name);
    }

    public String getOrigin() {
        return (underlyingNode.hasProperty(ORIGIN) && underlyingNode.getProperty(ORIGIN, null) != null ? ((String) underlyingNode.getProperty(ORIGIN)) : null);
    }

    public void setOrigin(final String origin) {
        if (origin != null) {
            underlyingNode.setProperty(ORIGIN, origin);
        } else {
            underlyingNode.removeProperty(ORIGIN);
        }
    }

    public boolean isJurist() {
        return
                (
                        underlyingNode.hasProperty(JURIST) && underlyingNode.getProperty(JURIST) != null && "Oui".equalsIgnoreCase((String) underlyingNode.getProperty(JURIST))
                );
    }

    public void setJurist(final boolean jurist) {
        if (jurist) {
            underlyingNode.setProperty(JURIST, "Oui");
        } else {
            underlyingNode.removeProperty(JURIST);
        }
    }


    @Override
    public boolean equals(final Object o) {
        return o instanceof Person &&
                underlyingNode.equals(((Person) o).getUnderlyingNode());
    }

    @Override
    public int hashCode() {
        return underlyingNode.hashCode();
    }

    @Override
    public String toString() {
        StringWriter stWriter = new StringWriter();

        try (JsonWriter jsonWriter = Json.createWriter(stWriter)) {
            jsonWriter.writeObject(
                    Json.createObjectBuilder()
                            .add(NAME, getName())
                            .build()
            );
        }

        return String.format("Person %s", stWriter.toString());
    }
}
