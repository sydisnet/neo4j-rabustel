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

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Utility class which create indexes and constraints on schema.
 * <p>
 * Created by shebert on 15/03/14.
 */
public class Indexer {

    /**
     * Static Logger.
     */
    static final Logger LOG = Logger.getLogger(MethodHandles.lookup().lookupClass().toString());

    /**
     * <p>
     * Create some constraints and other indexes based on property keys on nodes for a given label.
     * </p>
     *
     * @param graphDb      the neo4j service wrapper.
     * @param label        the label to constraint. This parameter is mandatory to avoid IllegalArgumentException.
     * @param constraints  is an array of unique constraints.
     * @param propertyKeys is an array of property keys.
     */
    public static void index(final GraphDatabaseService graphDb, final String label,
                             final String[] constraints,
                             final String[] propertyKeys) {
        // Check parameters
        if (label == null) {
            throw new IllegalArgumentException("Label is mandatory and must not be null !");
        }

        try (Transaction tx = graphDb.beginTx()) {
            // 1. Drop constraints and indexes if existing yet
            {
                // Dropping constraints
                graphDb.schema()
                        .getConstraints(DynamicLabel.label(label))
                        .forEach(cd -> {
                            LOG.info(String.format("Dropping unique constraint definition: %s.%s",
                                    cd.getLabel(),
                                    cd.getPropertyKeys().toString()));
                            cd.drop();
                        });

                // Dropping indexes
                graphDb
                        .schema()
                        .getIndexes(DynamicLabel.label(label))
                        .forEach(id -> {
                            LOG.info(String.format("Dropping index definition: %s.%s",
                                    id.getLabel(),
                                    id.getPropertyKeys().toString()));
                            id.drop();
                        });
            }

            // 2. Create constraints
            Set<String> propertyKeysYetIndexed = new HashSet<>();
            {
                if (constraints != null) {
                    Arrays.asList(constraints)
                            .stream()
                            .filter(c -> c != null)
                            .map(c ->
                            {
                                graphDb.schema().constraintFor(DynamicLabel.label(label))
                                        .assertPropertyIsUnique(c)
                                        .create();
                                LOG.info(String.format("Adding Constraint: %s.%s", label, c));
                                return c;

                            })
                            .forEach(propertyKeysYetIndexed::add);
                }
            }

            // 3. Create indexes
            {
                if (propertyKeys != null) {
                    Arrays.asList(propertyKeys)
                            .stream()
                            .filter(p -> p != null && !propertyKeysYetIndexed.contains(p))
                            .forEach(p ->
                            {
                                graphDb.schema().indexFor(DynamicLabel.label(label))
                                        .on(p)
                                        .create();

                                LOG.info(String.format("Adding Index: %s.%s", label, p));
                            });
                }
            }

            // Expect
            tx.success();
        } catch (RuntimeException ex) {
            LOG.severe(String.format("Unable to index label %s because of the underlying error: %s\n%s",
                    label, ex.getMessage(), ex.toString()));

            throw ex;
        }

        // Waiting for indexes on label
        try (Transaction ignored = graphDb.beginTx()) {
            graphDb.schema().awaitIndexesOnline(10, TimeUnit.SECONDS);
        }
    }
}
