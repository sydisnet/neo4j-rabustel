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

import eu.sydisnet.neo4j.rabustel.model.KnowsType;
import eu.sydisnet.neo4j.rabustel.model.MessageExchange;
import eu.sydisnet.neo4j.rabustel.model.MessageType;
import eu.sydisnet.neo4j.rabustel.model.Person;
import org.hamcrest.MatcherAssert;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;

/**
 * Test User Stories
 * <p>
 * Created by shebert on 29/03/14.
 */
public class GraphEntityManagerUT {

    static final Logger LOG = Logger.getLogger(MethodHandles.lookup().lookupClass().toString());

    static Weld weld;
    static WeldContainer weldContainer;

    @BeforeClass
    public static void init() {
        LOG.info("############################## GraphEntityManagerUT::init() ##############################");
        weld = new Weld();
        weldContainer = weld.initialize();
        Runtime.getRuntime().addShutdownHook(new Thread(weld::shutdown));

    }

    @AfterClass
    public static void cleanUp() {
        LOG.info("############################## GraphEntityManagerUT::cleanUp() ##############################");
        weld.shutdown();
    }

    @Test
    public void should_be_able_to_obtain_database_service() {
        // When
        GraphEntityManager entityManager = getGraphEntityManager();

        // Expect
        MatcherAssert.assertThat("EntityManager should exists", entityManager, notNullValue());
    }

    @Test
    public void should_be_able_to_create_full_database() throws IOException {
        GraphEntityManager entityManager = getGraphEntityManager();

        // Test A - Creating Persons
        // Given
        List<Person> persons;
        {
            // When
            persons = populateWithPersons(entityManager);

            // Expect
            MatcherAssert.assertThat(String.format("Should have %d entries", 88), persons, hasSize(88));
            LOG.info(String.format("Succesfully adding %d instances of \"Person\"", persons.size()));
        }

        // Test B - Finding Persons
        // Given
        String personName = "Jean Rabustel";
        String otherPersonName = "Michel de Pons";
        {
            entityManager.beginTransaction();

            // When
            Person p1 = entityManager.find(Person.class, personName);
            Person p2 = entityManager.find(Person.class, otherPersonName);

            // Expect Jean Rabustel
            MatcherAssert.assertThat("Should be Jean Rabustel", p1.getName(), is("Jean Rabustel"));
            MatcherAssert.assertThat("Jean Rabustel should come from DIJON City", p1.getOrigin(), is(equalToIgnoringCase("DIJON")));
            MatcherAssert.assertThat("Jean Rabustel should NOT be a JURIST", p1.isJurist(), is(false));

            // Expect Michel de Pons
            MatcherAssert.assertThat("Should be Michel de Pons", p2.getName(), is("Michel de Pons"));
            MatcherAssert.assertThat("Michel de Pons should come from PARIS City", p2.getOrigin(), is(equalToIgnoringCase("PARIS")));
            MatcherAssert.assertThat("Michel de Pons should be a JURIST", p2.isJurist(), is(true));

            entityManager.rollback();
        }


        // Test C - Creating Relationships between persons
        // Given
//        List<Optional<Person>> personInColumns = new ArrayList<>();
//        List<Optional<Person>> personInRows = new ArrayList<>();
//        {
//            // Given
//            populateWithRelationships(entityManager, persons, personInColumns, personInRows);
//
//            entityManager.beginTransaction();
//
//            // Expect Le chancelier de Bourgogne    <-> Jean de Gray    Type SOCIAL
//            {
//                Person p1 = entityManager.find(Person.class, "Le chancelier de Bourgogne");
//                Person p2 = entityManager.find(Person.class, "Jean de Gray");
//                Set<Relationship> relationships = entityManager.getRelationships(p1, p2);
//                int count = relationships.stream().filter(r -> r.isType(KnowsType.SOCIAL))
//                        .collect(Collectors.toSet()).size();
//                MatcherAssert.assertThat(String.format("%s and %s should have %s relationship", p1, p2, KnowsType.SOCIAL), count, is(1));
//            }
//
//            // Expect Isabelle de Portugal          <-> Jean Rabustel   Type PROF
//            {
//                Person p1 = entityManager.find(Person.class, "Isabelle de Portugal");
//                Person p2 = entityManager.find(Person.class, "Jean Rabustel");
//                Set<Relationship> relationships = entityManager.getRelationships(p1, p2);
//                int count = relationships.stream().filter(r -> r.isType(KnowsType.PROF))
//                        .collect(Collectors.toSet()).size();
//                MatcherAssert.assertThat(String.format("%s and %s should have %s relationship", p1, p2, KnowsType.PROF), count, is(1));
//            }
//
//            // Expect La femme de Michel Pons       <-> Michel de Pons  Type FAMILY
//            {
//                Person p1 = entityManager.find(Person.class, "La femme de Michel Pons");
//                Person p2 = entityManager.find(Person.class, "Michel de Pons");
//                Set<Relationship> relationships = entityManager.getRelationships(p1, p2);
//                int count = relationships.stream().filter(r -> r.isType(KnowsType.FAMILY))
//                        .collect(Collectors.toSet()).size();
//                int countPro = relationships.stream().filter(r -> r.isType(KnowsType.PROF))
//                        .collect(Collectors.toSet()).size();
//                MatcherAssert.assertThat(String.format("%s and %s should have %s relationship", p1, p2, KnowsType.FAMILY), count, is(1));
//                MatcherAssert.assertThat(String.format("%s and %s should have NO %s relationship", p1, p2, KnowsType.PROF), countPro, is(0));
//            }
//
//            // Expect La femme de Michel Pons       <-> Antoine Girard  <Aucune>
//            {
//                Person p1 = entityManager.find(Person.class, "La femme de Michel Pons");
//                Person p2 = entityManager.find(Person.class, "Antoine Girard");
//                Set<Relationship> relationships = entityManager.getRelationships(p1, p2);
//                MatcherAssert.assertThat(String.format("%s and %s should have NO relationship", p1, p2), relationships, empty());
//            }
//
//            entityManager.rollback();
//        }

        // Test D - Creating MessageExchange
        // Given
        List<MessageExchange> messages;
        {
            // When
            messages = populateWithMessageExchanges(entityManager);

            // Expect
            MatcherAssert.assertThat(String.format("Should have %d entries", 70), messages, hasSize(70));
            LOG.info(String.format("Succesfully adding %d instances of \"MessageExchange\"", messages.size()));
        }
    }


    private List<Person> populateWithPersons(final GraphEntityManager entityManager) throws IOException {
        // Retrieves content of person list
        List<String> lines = Files.readAllLines(Paths.get("src/main/resources", "personnes/list.txt"), Charset.defaultCharset());

        // Starts a transaction
        entityManager.beginTransaction();

        // Perform the parsing of the list
        List<Person> result =
                lines.stream()
                        .filter(line -> (line != null && !line.isEmpty() && !line.startsWith("#")))
                        .map(line -> {
                            String[] properties = line.split("\t");
                            return entityManager.persist(
                                    properties[0].trim(),
                                    (properties.length > 1 ? properties[1].trim() : null),
                                    (properties.length > 2 && "oui".equalsIgnoreCase(properties[2].trim()))
                            );
                        })
                        .collect(Collectors.toList());

        // Commit the transaction
        entityManager.commit();

        // Returns the result
        return result;
    }

    private void populateWithRelationships(
            final GraphEntityManager entityManager,
            final List<Person> persons,
            final List<Optional<Person>> personInColumns,
            final List<Optional<Person>> personInRows) throws IOException {
        // Retrieve content of relationships
        List<String> lines = Files.readAllLines(Paths.get("src/main/resources", "personnes/rel_knows.txt"), Charset.defaultCharset());

        // Starts a transaction
        entityManager.beginTransaction();

        // Perform the parsing of the first line
        {
            List<String> personNames =
                    Arrays.asList(
                            lines.stream()
                                    .filter(line -> (line != null && !line.isEmpty() && !line.startsWith("#")))
                                    .findFirst()
                                    .get()
                                    .split("\t")
                    );
            // Perform the filter / map /collect
            personInColumns.addAll(
                    personNames.stream()
                            .filter(personName -> (!personName.equalsIgnoreCase("1 : professionnel 2 : parenté 3 : sociabilité")))
                            .peek(personName -> {
                                if (entityManager.find(Person.class, personName) == null)
                                    LOG.info(String.format("The node: %s does not exist in columns !", personName));
                            })
                            .map(personName -> Optional.ofNullable(entityManager.find(Person.class, personName.trim())))
                            .collect(Collectors.toList())
            );
        }

        // Perform the parsing of the second line
        {
            // On passe directement à la deuxième ligne
            List<String> data = lines.stream()
                    .filter(
                            line -> (line != null
                                    && !line.isEmpty()
                                    && !line.startsWith("#")
                                    && !line.contains("1 : professionnel 2 : parenté 3 : sociabilité")
                            )
                    )
                    .collect(Collectors.toList());
            // Perform the filter / map / collect
            personInRows.addAll(
                    data.stream()
                            .filter(line -> (line != null && !line.isEmpty() && !line.startsWith("#")))
                            .map(line -> {
                                String[] currentRow = line.split("\t");
                                String personName = currentRow[0].trim();
                                Person personFrom = entityManager.find(Person.class, personName);
                                if (personFrom == null) {
                                    LOG.info(String.format("The node: %s does not exist in rows !", personName));
                                } else {
                                    List<String> relTypes = Arrays.asList(currentRow)
                                            .stream()
                                            .map(String::trim)
                                            .collect(Collectors.toList());
                                    for (int i = 1; i < relTypes.size(); i++) {
                                        Optional<Person> personTo = personInColumns.get((i - 1));
                                        if (!personTo.isPresent()) {
                                            LOG.info(
                                                    String.format(
                                                            "The node: %s wants to create a relationship to person in column %d but this last one dos not exist !",
                                                            personName, i)
                                            );
                                        } else {
                                            Person personToActual = personTo.get();
                                            switch (relTypes.get(i)) {
                                                case "1":
                                                    entityManager.bidirectionalRelationship(personFrom, personToActual, KnowsType.PROF);
                                                    break;
                                                case "2":
                                                    entityManager.bidirectionalRelationship(personFrom, personToActual, KnowsType.FAMILY);
                                                    break;
                                                case "3":
                                                    entityManager.bidirectionalRelationship(personFrom, personToActual, KnowsType.SOCIAL);
                                                    break;
                                            }
                                        }
                                    }
                                }

                                return Optional.ofNullable(personFrom);
                            })
                            .collect(Collectors.toList())
            );
        }

        // Check persons which are in persons and not in personInColumns
        persons.forEach(p -> {
            List<Person> personColumnList = personInColumns.stream()
                    .filter(pCol -> (pCol.isPresent()))
                    .map(Optional::get)
                    .collect(Collectors.toList());

            if (!personColumnList.contains(p)) {
                LOG.info(String.format("%s is not included in columns", p.toString()));
            }
        });

        // Check persons which are in persons and not in personInRows
        persons.forEach(p -> {
            List<Person> personRowList = personInRows.stream()
                    .filter(pRow -> (pRow.isPresent()))
                    .map(Optional::get)
                    .collect(Collectors.toList());

            if (!personRowList.contains(p)) {
                LOG.info(String.format("%s is not included in rows", p.toString()));
            }
        });

        // Commit the transaction
        entityManager.commit();
    }

    private List<MessageExchange> populateWithMessageExchanges(final GraphEntityManager entityManager) throws IOException {
        // Retrieves content of exchanges
        List<String> lines = Files.readAllLines(Paths.get("src/main/resources", "lettres/list.txt"), Charset.defaultCharset());

        // Starts a transaction
        entityManager.beginTransaction();

        // Perform the parsing of the list
        List<MessageExchange> result;
        result = lines.stream()
                .filter(line -> (line != null && !line.isEmpty() && !line.startsWith("#")))
                .map(line -> {
                    // Input: line
                    String[] lineArray = line.split("\t");

                    // Parsing
                    String number = lineArray[0].trim();
                    Integer sendDate = (lineArray[1] == null || lineArray[1].trim().isEmpty() || lineArray[1].trim().equalsIgnoreCase("s.d.") ? null : Integer.valueOf(lineArray[1].trim()));
                    String sendFrom = (lineArray[2] == null || lineArray[2].trim().isEmpty() || lineArray[2].trim().equalsIgnoreCase("s.l.") ? null : lineArray[2].trim());
                    Person personFrom = entityManager.find(Person.class, lineArray[3].trim());
                    if (personFrom == null) {
                        LOG.info(String.format("The sending man: %s does not exist !", lineArray[3]));
                    }
                    Person personTo = entityManager.find(Person.class, lineArray[4].trim());
                    if (personTo == null) {
                        LOG.info(String.format("The receiver: %s does not exist !", lineArray[4]));
                    }
                    String sendTo = (lineArray[5] == null || lineArray[5].trim().isEmpty() || lineArray[5].trim().equalsIgnoreCase("s.l.") ? null : lineArray[5].trim());
                    Person personTo2 = null;
                    if (lineArray[6] != null && !lineArray[6].trim().isEmpty()) {
                        personTo2 = entityManager.find(Person.class, lineArray[6].trim());
                        if (personTo2 == null) {
                            LOG.info(String.format("The other receiver: %s does not exist !", lineArray[6].trim()));
                        }
                    }
                    MessageType messageType;
                    switch (lineArray[7].trim()) {
                        case "Professionnel":
                            messageType = MessageType.PROF;
                            break;
                        case "Privé":
                            messageType = MessageType.PRIVATE;
                            break;
                        case "Professionnel avec mentions privées":
                            messageType = MessageType.PROF_PRIVATE;
                            break;
                        case "Privé avec propos professionnels":
                            messageType = MessageType.PRIVATE_PROF;
                            break;
                        default:
                            throw new RuntimeException(String.format("%s does not exist !", lineArray[7].trim()));
                    }
                    List<Person> mentions = new ArrayList<>();
                    for (int i = 8; i < lineArray.length; i++) {
                        if (lineArray[i] != null && !lineArray[i].trim().isEmpty()) {
                            Person pMent = entityManager.find(Person.class, lineArray[i].trim());
                            if (pMent == null) {
                                LOG.info(String.format("The person %s appears in letter %s but this person does not exist !",
                                        lineArray[i].trim(), number));
                            } else {
                                mentions.add(pMent);
                            }
                        }
                    }

                    // Process Letter
                    MessageExchange messageExchange = entityManager.persist(number, sendDate, sendFrom, sendTo, messageType);
                    if (personFrom != null) {
                        entityManager.setSender(messageExchange, personFrom);
                    }
                    if (personTo != null) {
                        if (personTo2 != null) {
                            entityManager.setRecipients(messageExchange, personTo, personTo2);
                        } else {
                            entityManager.setRecipient(messageExchange, personTo);
                        }
                    }
//                    mentions.stream().filter(p -> p != null).forEach(p -> entityManager.mention(messageExchange, p));

                    // Return the result
                    return messageExchange;
                })
                .collect(Collectors.toList());

        // Commit the transaction
        entityManager.commit();

        // Returns the result
        return result;
    }

    private GraphEntityManager getGraphEntityManager() {
        return weldContainer.instance().select(GraphEntityManager.class).get();
    }

}
