package eu.sydisnet.neo4j.rabustel.control;

import eu.sydisnet.neo4j.rabustel.model.KnowsType;
import eu.sydisnet.neo4j.rabustel.model.Person;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test User Stories
 *
 * Created by shebert on 29/03/14.
 */
public class GraphEntityManagerTest {

    static final Logger LOG = Logger.getLogger(MethodHandles.lookup().lookupClass().toString());

    static Weld weld;
    static WeldContainer weldContainer;

    @BeforeClass
    public static void init() {
        weld = new Weld();
        weldContainer = weld.initialize();
    }

    @AfterClass
    public static void cleanUp() {
        weld.shutdown();
    }

    @Test
    public void should_be_able_to_create_full_database() throws IOException {
        GraphEntityManager entityManager = getGraphEntityManager();
        assertThat("EntityManager should exists", entityManager, notNullValue());

        // PersonList
        List<Person> persons = new ArrayList<>();
        {
            // Given
            List<String> lines = Files.readAllLines(Paths.get("src/main/resources", "personnes/list.txt"), Charset.defaultCharset());

            // When
            entityManager.beginTransaction();
            int lineCount = 0;
            String[] properties;
            for (String line : lines) {
                if (line != null && !line.isEmpty() && !line.startsWith("#")) {
                    properties = line.split("\t");
                    persons.add(
                            entityManager.persist(
                                    properties[0].trim(),
                                    (properties.length > 1 ? properties[1].trim() : null),
                                    (properties.length > 2 && "oui".equalsIgnoreCase(properties[2].trim()))
                            )
                    );
                    lineCount++;
                }
            }

            entityManager.commit();

            // Expect
            assertThat(String.format("Should have %d entries", lineCount), persons, hasSize(lineCount));
            LOG.info(String.format("Succesfully adding %d instances of \"Person\"", lineCount));


            // Expect
            entityManager.beginTransaction();
            Person p = entityManager.find(Person.class, "Jean Rabustel");
            assertThat("Should be Jean Rabustel", p.getName(), is("Jean Rabustel"));
            entityManager.commit();
        }

        // Relationships between persons
        List<Person> personInColumns = new ArrayList<>();
        List<Person> personInRows = new ArrayList<>();
        {
            // Given
            List<String> lines = Files.readAllLines(Paths.get("src/main/resources", "personnes/rel_knows.txt"), Charset.defaultCharset());

            // When
            entityManager.beginTransaction();
            boolean firstLine = true;
            String[] properties;
            for (String line : lines) {
                if (line != null && !line.isEmpty() && !line.startsWith("#")) {
                    properties = line.split("\t");

                    // Traitement de la première ligne -> constitution de la liste des personnes
                    if (firstLine) {
                        firstLine = false;
                        for (int col = 1; col < properties.length; col++) {
                            Person pCol = entityManager.find(Person.class, properties[col].trim());
                            if (pCol == null) {
                                LOG.severe(String.format("The node: %s does not exist in columns !", properties[col].trim()));
                            } else {
                                personInColumns.add(pCol);
                            }
                        }

                        continue;
                    }

                    // Traitement à partir de la ligne deux
                    {
                        Person pRow = entityManager.find(Person.class, properties[0].trim());
                        if (pRow == null) {
                            LOG.severe(String.format("The node: %s does not exist in rows !", properties[0].trim()));
                        } else {
                            personInRows.add(pRow);

                            {
                                for (int col = 1; col < properties.length; col++) {
                                    Person p1 = personInColumns.get(col - 1);
                                    switch (properties[col].trim()) {
                                        case "1":
                                            entityManager.bidirectionalRelationship(p1, pRow, KnowsType.PROF);
                                            break;
                                        case "2":
                                            entityManager.bidirectionalRelationship(p1, pRow, KnowsType.FAMILY);
                                            break;
                                        case "3":
                                            entityManager.bidirectionalRelationship(p1, pRow, KnowsType.SOCIAL);
                                            break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Check persons which are in persons and not in personInRows
            List<Person> personsInPersonsNotInPersonInRows = new ArrayList<>();
            for (Person person : persons) {
                if (!personInRows.contains(person)) {
                    LOG.info(String.format("%s is not included in rows", person.toString()));
                    personsInPersonsNotInPersonInRows.add(person);
                }
            }

            // Check persons which are in persons and not in personInColumns
            List<Person> personsInPersonsNotInPersonInColumns = new ArrayList<>();
            for (Person person : persons) {
                if (!personInColumns.contains(person)) {
                    LOG.info(String.format("%s is not included in columns", person.toString()));
                    personsInPersonsNotInPersonInColumns.add(person);
                }
            }

            entityManager.commit();
        }

        

    }

    private GraphEntityManager getGraphEntityManager() {
        return weldContainer.instance().select(GraphEntityManager.class).get();
    }

}
