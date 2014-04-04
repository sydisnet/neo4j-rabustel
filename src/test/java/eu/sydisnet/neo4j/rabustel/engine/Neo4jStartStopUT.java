package eu.sydisnet.neo4j.rabustel.engine;

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

import eu.sydisnet.neo4j.rabustel.model.MessageType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Starts and stops neo4j Database.
 * <p>
 * Created by shebert on 15/03/14.
 */
public class Neo4jStartStopUT {

    static final Logger LOG = Logger.getLogger(MethodHandles.lookup().lookupClass().toString());

    static GraphDatabaseService DB_SERVICE;

    @BeforeClass
    public static void start() {
        LOG.info("############################## Neo4jStartStopUT::start() ##############################");

        // Given
        Path dbPath = Paths.get("/opt/java/neo4j/neo4j-community-2.0.1",
                "data/graph.db");

        // When
        DB_SERVICE = new GraphDatabaseFactory()
                .newEmbeddedDatabase(
                        dbPath.toAbsolutePath().toString()
                );

        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook(new Thread(DB_SERVICE::shutdown));

        // Expect
        assertThat("GraphDB should not be null", DB_SERVICE, notNullValue());

        LOG.info("Checking GraphDB Availability: " + DB_SERVICE.isAvailable(60000));
    }

    @AfterClass
    public static void stop() {
        LOG.info("############################## Neo4jStartStopUT::stop() ##############################");

        DB_SERVICE.shutdown();
    }

    @Test
    public void should_be_able_to_create_a_relationship_between_two_persons() {
        LOG.info("****************************** should_be_able_to_create_a_relationship_between_two_persons() ******************************");

        try (Transaction tx = DB_SERVICE.beginTx()) {
            // Given
            Label person = DynamicLabel.label("Personne");

            Node p1 = DB_SERVICE.createNode(person);
            p1.setProperty("nom", "Cécile Becchia");

            Node p2 = DB_SERVICE.createNode(person);
            p2.setProperty("nom", "Sébastien Hébert");

            // When
            p1.createRelationshipTo(p2, MessageType.PRIVATE);
            p1.getSingleRelationship(MessageType.PRIVATE, Direction.OUTGOING).delete();
            p1.delete();
            p2.delete();

            // Expect
            tx.success();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void exception_without_label() {
        LOG.info("****************************** exception_without_label() ******************************");

        // When
        Indexer.index(DB_SERVICE, null, new String[]{"nom"}, new String[]{"origine", "legiste"});
    }

    @Test
    public void should_be_able_to_deal_with_indexes() {
        LOG.info("****************************** should_be_able_to_deal_with_indexes() ******************************");

        // When
        Indexer.index(DB_SERVICE, "Personne", new String[]{"nom"}, new String[]{"origine", "legiste"});

        // Expect no exception

        // Other expectations
        Set<String> constraints = new HashSet<>();
        Set<String> indexes = new HashSet<>();
        retrieveConstraintsAndIndexes(constraints, indexes);

        assertThat("Should have one constraint",
                constraints.size(),
                is(1));
        assertThat("Should contains \"nom\"",
                constraints,
                hasItem("nom"));


        assertThat("Should have three indexes (two indexes plus the unique constraint)",
                indexes.size(),
                is(3));
        assertThat("Should contains the two indexes plus the unique constraint",
                indexes,
                hasItems("origine", "legiste", "nom"));
    }

    /**
     * Helper Method
     *
     * @param constraints the set of constraints to populate
     * @param indexes     the set of indexes to populate
     */
    private void retrieveConstraintsAndIndexes(final Set<String> constraints,
                                               final Set<String> indexes) {
        try (Transaction ignored = DB_SERVICE.beginTx()) {
            DB_SERVICE.schema()
                    .getConstraints(DynamicLabel.label("Personne"))
                    .forEach(cd -> cd.getPropertyKeys().forEach(constraints::add)
                    );

            DB_SERVICE.schema()
                    .getIndexes(DynamicLabel.label("Personne"))
                    .forEach(id -> id.getPropertyKeys().forEach(indexes::add)
                    );
        }
    }
}
