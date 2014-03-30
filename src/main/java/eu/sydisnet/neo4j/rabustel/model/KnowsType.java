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

import org.neo4j.graphdb.RelationshipType;

/**
 * Relationships types.
 *
 * Created by shebert on 23/03/14.
 */
public enum KnowsType implements RelationshipType {

    PROF("Professionnel"), FAMILY("Parenté"), SOCIAL("Sociabilité");

    private String description = "N/A";

    /**
     * Constructor
     *
     * @param description the human-readable meaning of this particular relationship type.
     */
    KnowsType(final String description) {
        this.description = description;
    }

    /**
     * @return the human-readable meaning of this particular relationship type.
     */
    public String getDescription() {
        return description;
    }
}
