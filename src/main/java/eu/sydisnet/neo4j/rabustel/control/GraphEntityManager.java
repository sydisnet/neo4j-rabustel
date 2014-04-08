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
import eu.sydisnet.neo4j.rabustel.model.*;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Service class which manages the GraphDatabase.
 * <p>
 * Created by shebert on 29/03/14.
 */
@Singleton
public class GraphEntityManager {

    static final Logger LOG = Logger.getLogger(MethodHandles.lookup().lookupClass().toString());

    /**
     * The database
     */
    GraphDatabaseService dbService;

    /**
     * The current transaction
     */
    Transaction currentTx;

    /**
     * Retrieve a node.
     *
     * @param entityClass the corresponding model class node to retrieve
     * @param primaryKey  the primary key of the model class node
     * @param <T>         the type of the model class node
     * @return the corresponding node
     *
     */
    public <T> T find(final Class<T> entityClass, final Object primaryKey) {
        // Start transaction or join current one
        Transaction newTx = null;
        if (currentTx == null) {
            newTx = dbService.beginTx();
        }

        // Process
        T entity = null;
        if (Person.class.equals(entityClass)) {
            List<Node> nodes = findNodesByLabelAndProperty(DynamicLabel.label(Person.LABEL), Person.NAME, primaryKey);
            if (nodes != null && !nodes.isEmpty() && nodes.size() == 1) {
                entity = entityClass.cast(new Person(nodes.get(0)));
            }
        } else if (MessageExchange.class.equals(entityClass)) {
            List<Node> nodes = findNodesByLabelAndProperty(DynamicLabel.label(MessageExchange.LABEL), MessageExchange.NUMBER, primaryKey);
            if (nodes != null && !nodes.isEmpty() && nodes.size() == 1) {
                entity = entityClass.cast(new MessageExchange(nodes.get(0)));
            }
        }


        // Commit transaction if a new one have been required
        if (newTx != null) {
            newTx.success();
            newTx.close();
        }

        return entity;
    }

    /**
     * Retrieve all the relationships between two instances of {@link eu.sydisnet.neo4j.rabustel.model.Person}
     *
     * @param p1 the first person
     * @param p2 the other person
     * @return the relationships between {@code p1} and {@code p2}
     */
    public Set<Relationship> getRelationships(final Person p1, final Person p2) {
        // Start transaction or join current one
        Transaction newTx = null;
        if (currentTx == null) {
            newTx = dbService.beginTx();
        }

        // Process
        Set<Relationship> relationships = new HashSet<>();
        for (Relationship rel : p1.getUnderlyingNode().getRelationships(Direction.BOTH)) {
            if (rel.getOtherNode(p1.getUnderlyingNode()).equals(p2.getUnderlyingNode())) {
                relationships.add(rel);
            }
        }

        // Commit transaction if a new one have been required
        if (newTx != null) {
            newTx.success();
            newTx.close();
        }

        return relationships;
    }

    /**
     * Persists a new Person model class node.
     *
     * @param name   the name of the Person
     * @param origin the origin of the Person
     * @param jurist if the Person is a jurist or not
     * @return the corresponding model class node instance
     *
     */
    public Person persist(final String name, final String origin, final boolean jurist) {
        // Start transaction or join current one
        Transaction newTx = null;
        if (currentTx == null) {
            newTx = dbService.beginTx();
        }

        // Process
        Person person = new Person(dbService.createNode(DynamicLabel.label(Person.LABEL)));
        person.setName(name);
        person.setOrigin(origin);
        person.setJurist(jurist);

        // Commit transaction if a new one have been required
        if (newTx != null) {
            newTx.success();
            newTx.close();
        }

        return person;
    }

    /**
     * Persists a new relationship between two instances of {@link eu.sydisnet.neo4j.rabustel.model.Person}, i.e.
     * {@code p1} and {@code p2} of type {@link eu.sydisnet.neo4j.rabustel.model.KnowsType}
     *
     * @param p1 the first person
     * @param p2 the other person
     * @param relType the type of the relationship
     *
     */
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
            p1.getUnderlyingNode().createRelationshipTo(p2.getUnderlyingNode(), relType);
        }

        // Commit transaction if a new one have been required
        if (newTx != null) {
            newTx.success();
            newTx.close();
        }
    }

    /**
     * Persists a new MessageExchange model class node.
     *
     * @param number              the number of the mail
     * @param sendingDate         the date if known of the exchange
     * @param sendingFromLocation the location from the exchange has been sent
     * @param sendingToLocation   the location where the exchange has been received
     * @param messageType         the type of the message
     * @return the corresponding model class node instance
     */
    public MessageExchange persist(final String number,
                                   final Integer sendingDate,
                                   final String sendingFromLocation,
                                   final String sendingToLocation,
                                   final MessageType messageType) {
        // Start transaction or join current one
        Transaction newTx = null;
        if (currentTx == null) {
            newTx = dbService.beginTx();
        }

        // Process
        MessageExchange messageExchange = new MessageExchange(dbService.createNode(DynamicLabel.label(MessageExchange.LABEL)));
        messageExchange.setNumber(number);
        messageExchange.setSendingDate(sendingDate);
        messageExchange.setSendingFromLocation(sendingFromLocation);
        messageExchange.setSendingToLocation(sendingToLocation);
        messageExchange.setMessageType(messageType);

        // Commit transaction if a new one have been required
        if (newTx != null) {
            newTx.success();
            newTx.close();
        }

        return messageExchange;
    }

    /**
     * Creates a relationship from the {@code sender} to the {@code messageExchange}
     * of type {@link eu.sydisnet.neo4j.rabustel.model.MessageDirection#EXPED}.
     *
     * @param messageExchange the message exchange
     * @param sender          the {@link eu.sydisnet.neo4j.rabustel.model.Person} instance which has sent the message
     */
    public void setSender(final MessageExchange messageExchange, final Person sender) {
        // Start transaction or join current one
        Transaction newTx = null;
        if (currentTx == null) {
            newTx = dbService.beginTx();
        }

        // Process
        sender.getUnderlyingNode().createRelationshipTo(messageExchange.getUnderlyingNode(), MessageDirection.EXPED);

        // Commit transaction if a new one have been required
        if (newTx != null) {
            newTx.success();
            newTx.close();
        }
    }

    /**
     * Creates a relationship from the {@code messageExchange} to the {@code recipient}
     * of type {@link eu.sydisnet.neo4j.rabustel.model.MessageDirection#DEST}.
     *
     * @param messageExchange the message exchange
     * @param recipient       the {@link eu.sydisnet.neo4j.rabustel.model.Person} instance which has received the message
     */
    public void setRecipient(final MessageExchange messageExchange, final Person recipient) {
        // Start transaction or join current one
        Transaction newTx = null;
        if (currentTx == null) {
            newTx = dbService.beginTx();
        }

        // Process
        messageExchange.getUnderlyingNode().createRelationshipTo(recipient.getUnderlyingNode(), MessageDirection.DEST);

        // Commit transaction if a new one have been required
        if (newTx != null) {
            newTx.success();
            newTx.close();
        }
    }

    /**
     * Creates a relationship from the {@code messageExchange} to the {@code recipient}
     * of type {@link eu.sydisnet.neo4j.rabustel.model.MessageDirection#DEST} and then creates another
     * relationship from the {@code messageExchange} to the {@code otherRecipient}
     * of type {@link eu.sydisnet.neo4j.rabustel.model.MessageDirection#AUTRE_DEST}.
     *
     * @param messageExchange the message exchange
     * @param recipient       the {@link eu.sydisnet.neo4j.rabustel.model.Person} instance which has received the message
     * @param otherRecipient  the second {@link eu.sydisnet.neo4j.rabustel.model.Person} instance which has benn concerned by this message
     */
    public void setRecipients(final MessageExchange messageExchange, final Person recipient, final Person otherRecipient) {
        // Start transaction or join current one
        Transaction newTx = null;
        if (currentTx == null) {
            newTx = dbService.beginTx();
        }

        // Process
        messageExchange.getUnderlyingNode().createRelationshipTo(recipient.getUnderlyingNode(), MessageDirection.DEST);
        messageExchange.getUnderlyingNode().createRelationshipTo(otherRecipient.getUnderlyingNode(), MessageDirection.AUTRE_DEST);

        // Commit transaction if a new one have been required
        if (newTx != null) {
            newTx.success();
            newTx.close();
        }
    }

    /**
     * Creates a relationship from the {@code messageExchange} to the {@code mentioned}
     * of type {@link eu.sydisnet.neo4j.rabustel.model.MessageDirection#MENTION}.
     *
     * @param messageExchange the message exchange
     * @param mentioned       the {@link eu.sydisnet.neo4j.rabustel.model.Person} instance which has received the message
     */
    public void mention(final MessageExchange messageExchange, final Person mentioned) {
        // Start transaction or join current one
        Transaction newTx = null;
        if (currentTx == null) {
            newTx = dbService.beginTx();
        }

        // Process
        messageExchange.getUnderlyingNode().createRelationshipTo(mentioned.getUnderlyingNode(), MessageDirection.MENTION);

        // Commit transaction if a new one have been required
        if (newTx != null) {
            newTx.success();
            newTx.close();
        }
    }


    /**
     * Helper method which retrieve a list of nodes based on {@code label}, {@code key} and {@code value}.
     *
     * @param label the labelized-nodes to seek for
     * @param key   the key to be includes in search
     * @param value the value of the key to search
     * @return the matching list nodes
     */
    private List<Node> findNodesByLabelAndProperty(final Label label, final String key, final Object value) {
        List<Node> nodeList = new ArrayList<>();

        try (ResourceIterator<Node> resourceIterator = dbService.findNodesByLabelAndProperty(label, key, value).iterator()) {
            resourceIterator.forEachRemaining(nodeList::add);
            resourceIterator.close();
        }
        return nodeList;
    }


    /**
     * Starts a new global transaction.
     */
    public void beginTransaction() {
        if (currentTx != null) {
            LOG.info("A new transaction is requested so the current one is marked for rollback.");
            currentTx.failure();
            currentTx.close();
        }

        currentTx = dbService.beginTx();
    }

    /**
     * Commits and then closes the global transaction.
     *
     * @throws IllegalStateException in the case of the global transaction does not exist.
     */
    public void commit() {
        if (currentTx == null) {
            throw new IllegalStateException("Current Transaction is absent ! You have to start a new one before...");
        }

        currentTx.success();
        currentTx.close();
        currentTx = null;
    }

    /**
     * Rollbacks and then closes the global transaction.
     *
     * @throws IllegalStateException in the case of the global transaction does not exist.
     */
    public void rollback() {
        if (currentTx == null) {
            throw new IllegalStateException("Current Transaction is absent ! You have to start a new one before...");
        }

        currentTx.failure();
        currentTx.close();
        currentTx = null;
    }


    /**
     * Starts the embedded database.
     */
    @PostConstruct
    private void startDatabase() {
        LOG.info("GraphEntityManager::startDatabase()");

        // 1. Starting database
        {
            long starting = System.nanoTime();
            Path dbPath = Paths.get("/opt/java/neo4j/neo4j-community-2.0.1", "data/graph.db");
            dbService = new GraphDatabaseFactory()
                    .newEmbeddedDatabase(
                            dbPath.toAbsolutePath().toString()
                    );
            Runtime.getRuntime().addShutdownHook(new Thread(dbService::shutdown));
            dbService.isAvailable(60000);
            LOG.info(String.format("Starting database in %s ms.", (System.nanoTime() - starting) / (Math.pow(10, 6))));
        }

        // 2. Cleanup the database
        {
            cleanUp();
        }

        // 3. Creating indexes
        {
            long starting = System.nanoTime();

            Indexer.index(dbService, "Personne", new String[]{"nom"}, new String[]{"origine", "legiste"});
            Indexer.index(dbService, "Lettre", new String[]{"numero"}, new String[]{"date_envoi", "lieu_envoi", "lieu_reception"});

            LOG.info(String.format("Creating indexes in %s ms.", (System.nanoTime() - starting) / (Math.pow(10, 6))));
        }
    }

    /**
     * Cleaning up all the database.
     */
    private void cleanUp() {
        {
            try (Transaction tx = dbService.beginTx()) {
                GlobalGraphOperations.at(dbService)
                        .getAllRelationships()
                        .forEach(Relationship::delete);

                GlobalGraphOperations.at(dbService)
                        .getAllNodes()
                        .forEach(Node::delete);

                tx.success();
            }
        }
    }

    /**
     * Stops the database.
     */
    @PreDestroy
    private void stopDatabase() {
        LOG.info("GraphEntityManager::stopDatabase()");

        dbService.shutdown();
    }
}
