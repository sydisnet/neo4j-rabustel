package eu.sydisnet.neo4j.rabustel.control;

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

import eu.sydisnet.neo4j.rabustel.engine.Indexer;
import eu.sydisnet.neo4j.rabustel.model.KnowsType;
import eu.sydisnet.neo4j.rabustel.model.Person;
import eu.sydisnet.neo4j.rabustel.model.SimpleKnowsType;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service class which manages the GraphDatabase.
 *
 * Created by shebert on 29/03/14.
 */
@Singleton
public class GraphEntityManager {

    static final  Logger LOG = Logger.getLogger(MethodHandles.lookup().lookupClass().toString());

    GraphDatabaseService dbService;

    Transaction currentTx;

    public Person persist(final String name, final String origin, final boolean jurist) {
        // Start transaction or join current one
        Transaction newTx = null;
        if (currentTx == null) {
            newTx = dbService.beginTx();
        }

        // Process
        Person person = new Person(dbService.createNode(DynamicLabel.label("Personne")));
        person.setName(name);
        if (origin != null) { person.setOrigin(origin); }
        person.setJurist(jurist);


        // Commit transaction if a new one have been required
        if (newTx != null) {
            newTx.success();
            newTx.close();
        }

        return person;
    }

    public void bidirectionalRelationship(final Person p1, final Person p2, final KnowsType relType) {
        // Start transaction or join current one
        Transaction newTx = null;
        if (currentTx == null) {
            newTx = dbService.beginTx();
        }

        // Process
        boolean relToBeCreated = true;
        for (Relationship rel : p1.getUnderlyingNode().getRelationships(relType, Direction.BOTH)) {
            if (rel.getOtherNode(p1.getUnderlyingNode()).equals(p2.getUnderlyingNode())) {
                // la relation existe déjà
                relToBeCreated = false;
            }
        }
        for (Relationship rel : p2.getUnderlyingNode().getRelationships(relType, Direction.BOTH)) {
            if (rel.getOtherNode(p2.getUnderlyingNode()).equals(p1.getUnderlyingNode())) {
                // la relation existe déjà
                relToBeCreated = false;
            }
        }
        if (relToBeCreated) {
            Relationship rel1To2 = p1.getUnderlyingNode().createRelationshipTo(p2.getUnderlyingNode(), relType);
//            rel1To2.setProperty("type", relType.getDescription());
//        Relationship rel2To1 = p2.getUnderlyingNode().createRelationshipTo(p1.getUnderlyingNode(), SimpleKnowsType.KNOWS);
        }

        // Commit transaction if a new one have been required
        if (newTx != null) {
            newTx.success();
            newTx.close();
        }
    }

    public <T> T find(final Class<T> entityClass, final Object primaryKey) {
        // Start transaction or join current one
        Transaction newTx = null;
        if (currentTx == null) {
            newTx = dbService.beginTx();
        }

        // Process
        // 1. Getting Label
        Label label = null;
        String key = null;
        T entity = null;
        if (Person.class.equals(entityClass)) {
            label = DynamicLabel.label( Person.LABEL );
            key = Person.NAME ;
            List<Node> nodes = findNodesByLabelAndProperty(label, key, primaryKey);
            if (nodes != null && !nodes.isEmpty() && nodes.size() == 1) {
                entity = (T) new Person(nodes.get(0));
            }
        }


        // Commit transaction if a new one have been required
        if (newTx != null) {
            newTx.success();
            newTx.close();
        }

        return entity;
    }

    private List<Node> findNodesByLabelAndProperty(final Label label, final String key, final Object primaryKey) {
        List<Node> nodeList = new ArrayList<>();
        try ( ResourceIterator<Node> resourceIterator = dbService.findNodesByLabelAndProperty(label, key, primaryKey).iterator() )
        {
            while (resourceIterator.hasNext()) {
                nodeList.add(resourceIterator.next());
            }

            resourceIterator.close();
        }
        return nodeList;
    }


    public void beginTransaction() {
        if (currentTx != null) {
            LOG.info("A new transaction is requested so the current one is marked for rollback.");
            currentTx.failure();
            currentTx.close();
        }

        currentTx = dbService.beginTx();
    }

    public void commit() {
        if (currentTx == null) {
            throw new IllegalStateException("Current Transaction is absent ! You have to start a new one before...");
        }

        currentTx.success();
        currentTx.close();
        currentTx = null;
    }

    public void rollback() {
        if (currentTx == null) {
            throw new IllegalStateException("Current Transaction is absent ! You have to start a new one before...");
        }

        currentTx.failure();
        currentTx.close();
        currentTx = null;
    }



    @PostConstruct
    void startDatabase() {
        LOG.info("GraphEntityManager::startDatabase()");

        // 1. Starting database
        {
            long starting = System.currentTimeMillis();

            Path dbPath = Paths.get("/opt/java/neo4j/neo4j-community-2.0.1", "data/graph.db");

            dbService = new GraphDatabaseFactory()
                    .newEmbeddedDatabase(
                            dbPath.toAbsolutePath().toString()
                    );

            // Registers a shutdown hook for the Neo4j instance so that it
            // shuts down nicely when the VM exits (even if you "Ctrl-C" the
            // running application).
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    dbService.shutdown();
                }
            });

            LOG.info("Checking GraphDB Availability: " + dbService.isAvailable(60000));

            LOG.info(String.format("Starting database in %d ms.", (System.currentTimeMillis() - starting) / (10 ^ 6)));
        }

        // 2. Creating indexes
        {
            cleanUp();

            long starting = System.currentTimeMillis();

            Indexer.index(dbService, "Personne", new String[]{"nom"}, new String[]{"origine", "legiste"});
            Indexer.index(dbService, "Lettre", new String[]{"numero"}, new String[]{"date", "lieu_envoi", "lieu_reception"});

            LOG.info(String.format("Creating indexes in %d ms.", (System.currentTimeMillis() - starting) / (10 ^ 6)));
        }
    }

    private void cleanUp() {
        // First, dropping all objects
        {
            try ( Transaction tx = dbService.beginTx() )
            {
                for (Relationship rel : GlobalGraphOperations.at(dbService).getAllRelationships())
                {
                    rel.delete();
                }

                for (Node node : GlobalGraphOperations.at(dbService).getAllNodes())
                {
                    node.delete();
                }

                tx.success();
            }
        }
    }


    @PreDestroy
    void stopDatabase() {
        LOG.info("GraphEntityManager::stopDatabase()");

        dbService.shutdown();
    }
}
