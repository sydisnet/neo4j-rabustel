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

import eu.sydisnet.neo4j.rabustel.model.KnowsType;
import eu.sydisnet.neo4j.rabustel.model.Person;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Deal with CSV files in {@code src/main/resources} to create a graph database.
 *
 * Created by shebert on 23/03/14.
 */
public class DatabaseFactoryTest {

    static final Logger LOG = Logger.getLogger(MethodHandles.lookup().lookupClass().toString());

    static GraphDatabaseService DB_SERVICE;

    @BeforeClass
    public static void start() {
        LOG.info("############################## DatabaseFactoryTest::start() ##############################");

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
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                DB_SERVICE.shutdown();
            }
        });


        LOG.info("Checking GraphDB Availability: " + DB_SERVICE.isAvailable(60000));
    }

    @Before
    public void initialize() {
        LOG.info("############################## DatabaseFactoryTest::initialize() ##############################");

        // First, dropping all objects
        {
            try ( Transaction tx = DB_SERVICE.beginTx() )
            {
                for (Relationship rel : GlobalGraphOperations.at(DB_SERVICE).getAllRelationships())
                {
                    rel.delete();
                }

                for (Node node : GlobalGraphOperations.at(DB_SERVICE).getAllNodes())
                {
                    node.delete();
                }

                tx.success();
            }
        }


        // Second, setting constraints and indexes
        {
            Indexer.index(DB_SERVICE,
                    "Personne", new String[]{"nom"},
                    new String[]{"origine", "legiste"});
            Indexer.index(DB_SERVICE,
                    "Lettre", new String[]{"numero"},
                    new String[]{"date", "lieu_envoi", "lieu_reception"});
        }
    }

    @AfterClass
    public static void stop() {
        LOG.info("############################## DatabaseFactoryTest::stop() ##############################");

        DB_SERVICE.shutdown();
    }

    @Test
    public void should_be_able_to_populate_with_persons() throws IOException {
        LOG.info("****************************** should_be_able_to_populate_with_persons() ******************************");

        // First, we retrieve and insert the list of persons
        {
            List<String> lines = Files.readAllLines(Paths.get("src/main/resources", "personnes/list.txt"), Charset.defaultCharset());

            LOG.info("Lines: " + lines.toString());

            try ( Transaction tx = DB_SERVICE.beginTx() )
            {
                Person person;
                String[] properties;
                for (String line : lines)
                {
                    if (line != null && !line.isEmpty() && !line.startsWith("#"))
                    {
                        properties = line.split("\t");

                        try ( ResourceIterator<Node> personResourceIterator = DB_SERVICE
                                .findNodesByLabelAndProperty( DynamicLabel.label("Personne"), "nom", properties[0] )
                                .iterator() )
                        {
                            Node personNode;
                            if ( personResourceIterator.hasNext() )
                            {
                                person = new Person(personResourceIterator.next());
                            }
                            else {
                                person = new Person(DB_SERVICE.createNode(DynamicLabel.label("Personne")));
                            }
                            personResourceIterator.close();
                        }
                        person.setName(properties[0]);
                        if (properties.length > 1) person.setOrigin(properties[1]);
                        person.setJurist(properties.length > 2 && "oui".equalsIgnoreCase(properties[2]));

                        LOG.info(String.format("Current Node: %s", person));
                    }
                }

                // commit
                tx.success();
            }
        }

        // Secondly, we add relationships
        {
            List<String> lines = Files.readAllLines(Paths.get("src/main/resources", "personnes/rel_knows.txt"), Charset.defaultCharset());

            try ( Transaction tx = DB_SERVICE.beginTx() )
            {
                String[] properties;
                boolean firstLine = true;
                List<Person> personListCol = null;
                List<Person> personListRow = null;
                for (String line : lines)
                {
                    if (line != null && !line.isEmpty() && !line.startsWith("#"))
                    {
                        properties = line.split("\t");

                        // Traitement de la première ligne -> constitution de la liste des personnes
                        if (firstLine)
                        {
                            firstLine = false;
                            personListCol = new ArrayList<>();

                            for (int col = 1 ; col < properties.length ; col++)
                            {
                                try ( ResourceIterator<Node> personResourceIterator = DB_SERVICE
                                        .findNodesByLabelAndProperty( DynamicLabel.label("Personne"), "nom", properties[col] )
                                        .iterator() )
                                {
                                    if ( personResourceIterator.hasNext() )
                                    {
                                        personListCol.add(new Person(personResourceIterator.next()));
                                    }
                                    else {
                                        throw new IllegalArgumentException(String.format("En colonne, /%s/ n'existe pas dans la liste des personnes !", properties[col]));
                                    }
                                    personResourceIterator.close();
                                }
                            }

                            continue;
                        }

                        // Traitement à partir de la ligne deux
                        try ( ResourceIterator<Node> personResourceIterator = DB_SERVICE
                                .findNodesByLabelAndProperty( DynamicLabel.label("Personne"), "nom", properties[0] )
                                .iterator() )
                        {

                            if (!line.contains("\t")) {
                                LOG.info(String.format("La personne /%s/ n'a pas de relation sur sa ligne...", properties[0]));
                            }

                            if ( personResourceIterator.hasNext() )
                            {
                                Person person = new Person(personResourceIterator.next());
                                if (!personListCol.contains(person))
                                {
                                    throw new IllegalArgumentException(String.format("La ligne /%s/ ne fait pas partie de la liste des personnes en colonne !", properties[0]));
                                }
                                else
                                {
                                    if (personListRow == null) {
                                        personListRow = new ArrayList<>();
                                    }
                                    personListRow.add(person);

                                    // TODO: Création des relations
                                    {
                                        for (int col = 1 ; col < properties.length ; col++)
                                        {
                                            Node p1 = personListCol.get(col - 1).getUnderlyingNode();
                                            Node p2 = person.getUnderlyingNode();
                                            switch (properties[col])
                                            {
                                                case "1":
                                                    p1.createRelationshipTo(p2, KnowsType.PROF);
                                                    break;
                                                case "2":
                                                    p1.createRelationshipTo(p2, KnowsType.FAMILY);
                                                    break;
                                                case "3":
                                                    p1.createRelationshipTo(p2, KnowsType.SOCIAL);
                                                    break;
                                            }
                                        }
                                    }
                                }
                            }
                            else {
                                throw new IllegalArgumentException(String.format("En ligne, /%s/ n'existe pas dans la liste des personnes !", properties[0]));
                            }
                            personResourceIterator.close();
                        }
                    }
                }

                LOG.info(String.format("PersonListCol::size(): %d", personListCol.size()));
                LOG.info(String.format("PersonListRow::size(): %d", personListRow.size()));
                LOG.info(String.format("PersonListRow::toString(): %s", personListRow));



                // commit
                tx.success();
            }


        }

    }
}
