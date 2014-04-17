package eu.sydisnet.neo4j.rabustel.userstories;

import eu.sydisnet.neo4j.rabustel.control.GraphEntityManager;
import eu.sydisnet.neo4j.rabustel.model.MessageExchange;
import eu.sydisnet.neo4j.rabustel.model.MessageType;
import eu.sydisnet.neo4j.rabustel.model.Person;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.Test;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by shebert on 11/04/14.
 */
public class AllAuthorsAndMessageExchangesUST {

    static final Logger LOG = Logger.getLogger(MethodHandles.lookup().lookupClass().toString());

    static Weld weld;
    static WeldContainer weldContainer;


    @Test
    public void user_story_01_all_authors_with_all_exchanges() throws IOException {
        LOG.info("****************************** AllAuthorsAndMessageExchangesUST::user_story_01_all_authors_with_all_exchanges() ******************************");
        System.setProperty("DB_NAME", "data/graph01.db");

        LOG.info("############################## Weld::init() ##############################");
        weld = new Weld();
        weldContainer = weld.initialize();
        Runtime.getRuntime().addShutdownHook(new Thread(weld::shutdown));

        // Get EntityManager
        GraphEntityManager entityManager = weldContainer.instance().select(GraphEntityManager.class).get();

        // Starts a transaction
        entityManager.beginTransaction();

        LOG.info("MATCH (p:Personne)-[r:EXPED|:DEST|:AUTRE_DEST]-(l:Lettre) return p,r,l,p.nom,l.numero");

        /******************************************************************************************************/
        /*************************************** Insert People ************************************************/
        {
            // Retrieves content of person list
            List<String> lines = Files.readAllLines(Paths.get("src/main/resources", "personnes/list.txt"), Charset.defaultCharset());

            // Perform the parsing of the list
            lines.stream()
                    .filter(line -> (line != null && !line.isEmpty() && !line.startsWith("#")))
                    .forEach(line -> {
                        String[] properties = line.split("\t");
                        entityManager.persist(
                                properties[0].trim(),
                                (properties.length > 1 ? properties[1].trim() : null),
                                (properties.length > 2 && "oui".equalsIgnoreCase(properties[2].trim()))
                        );
                    });
        }
        /******************************************************************************************************/


        /******************************************************************************************************/
        /*************************************** Insert MessageExchanges **************************************/
        {
            // Retrieves content of exchanges
            List<String> lines = Files.readAllLines(Paths.get("src/main/resources", "lettres/list.txt"), Charset.defaultCharset());

            // Perform the parsing of the list
            lines.stream()
                    .filter(line -> (line != null && !line.isEmpty() && !line.startsWith("#")))
                    .forEach(line -> {
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

                        // Process Persons
                        if (personFrom != null) {
                            if (personTo != null) {
                                entityManager.writeTo(personFrom, personTo);
                            }
                            if (personTo2 != null) {
                                entityManager.writeTo(personFrom, personTo2);
                            }
                        }

                    });
        }
        // Commit the transaction
        entityManager.commit();


        LOG.info("############################## Weld::shutdown() ##############################");
        weld.shutdown();
    }


    @Test
    public void user_story_03_all_exchanges_between_paris_and_dijon_sender_recipients() throws IOException {
        LOG.info("****************************** AllAuthorsAndMessageExchangesUST::user_story_03_all_exchanges_between_paris_and_dijon_sender_recipients() ******************************");
        System.setProperty("DB_NAME", "data/graph03.db");

        LOG.info("############################## Weld::init() ##############################");
        weld = new Weld();
        weldContainer = weld.initialize();
        Runtime.getRuntime().addShutdownHook(new Thread(weld::shutdown));

        // Get EntityManager
        GraphEntityManager entityManager = weldContainer.instance().select(GraphEntityManager.class).get();

        // Starts a transaction
        entityManager.beginTransaction();

        LOG.info("MATCH (l:Lettre)-[r:EXPED|:DEST|:AUTRE_DEST]-(p:Personne) \n" +
                "WHERE \n\t(l.lieu_envoi = \"Paris\" AND l.lieu_reception = \"Dijon\") \n" +
                "\tOR (l.lieu_envoi = \"Dijon\" AND l.lieu_reception = \"Paris\") \n" +
                "return l,r,p");

        /******************************************************************************************************/
        /*************************************** Insert People ************************************************/
        {
            // Retrieves content of person list
            List<String> lines = Files.readAllLines(Paths.get("src/main/resources", "personnes/list.txt"), Charset.defaultCharset());

            // Perform the parsing of the list
            lines.stream()
                    .filter(line -> (line != null && !line.isEmpty() && !line.startsWith("#")))
                    .forEach(line -> {
                        String[] properties = line.split("\t");
                        entityManager.persist(
                                properties[0].trim(),
                                (properties.length > 1 ? properties[1].trim() : null),
                                (properties.length > 2 && "oui".equalsIgnoreCase(properties[2].trim()))
                        );
                    });
        }
        /******************************************************************************************************/


        /******************************************************************************************************/
        /*************************************** Insert MessageExchanges **************************************/
        {
            // Retrieves content of exchanges
            List<String> lines = Files.readAllLines(Paths.get("src/main/resources", "lettres/list.txt"), Charset.defaultCharset());

            // Perform the parsing of the list
            lines.stream()
                    .filter(line -> (line != null && !line.isEmpty() && !line.startsWith("#")))
                    .forEach(line -> {
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
                        if (
                                ("Paris".equalsIgnoreCase(sendFrom) && "Dijon".equalsIgnoreCase(sendTo))
                                        ||
                                        ("Dijon".equalsIgnoreCase(sendFrom) && "Paris".equalsIgnoreCase(sendTo))
                                ) {
                            if (!number.contains("449-68") && !number.contains("449-90 bis") && !number.contains("449-91") && !number.contains("449-99") && !number.contains("449-100")) {
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
                            }
                        }

                    });
        }
        // Commit the transaction
        entityManager.commit();


        LOG.info("############################## Weld::shutdown() ##############################");
        weld.shutdown();
    }


    @Test
    public void user_story_04_all_exchanges_between_paris_and_dijon_mentions() throws IOException {
        LOG.info("****************************** AllAuthorsAndMessageExchangesUST::user_story_04_all_exchanges_between_paris_and_dijon_mentions() ******************************");
        System.setProperty("DB_NAME", "data/graph04.db");

        LOG.info("############################## Weld::init() ##############################");
        weld = new Weld();
        weldContainer = weld.initialize();
        Runtime.getRuntime().addShutdownHook(new Thread(weld::shutdown));

        // Get EntityManager
        GraphEntityManager entityManager = weldContainer.instance().select(GraphEntityManager.class).get();

        // Starts a transaction
        entityManager.beginTransaction();

        LOG.info("MATCH (l:Lettre)-[r:MENTION]-(p:Personne) \n" +
                "WHERE \n\t(l.lieu_envoi = \"Paris\" AND l.lieu_reception = \"Dijon\") \n" +
                "\tOR (l.lieu_envoi = \"Dijon\" AND l.lieu_reception = \"Paris\") \n" +
                "return l,r,p");

        /******************************************************************************************************/
        /*************************************** Insert People ************************************************/
        {
            // Retrieves content of person list
            List<String> lines = Files.readAllLines(Paths.get("src/main/resources", "personnes/list.txt"), Charset.defaultCharset());

            // Perform the parsing of the list
            lines.stream()
                    .filter(line -> (line != null && !line.isEmpty() && !line.startsWith("#")))
                    .forEach(line -> {
                        String[] properties = line.split("\t");
                        entityManager.persist(
                                properties[0].trim(),
                                (properties.length > 1 ? properties[1].trim() : null),
                                (properties.length > 2 && "oui".equalsIgnoreCase(properties[2].trim()))
                        );
                    });
        }
        /******************************************************************************************************/


        /******************************************************************************************************/
        /*************************************** Insert MessageExchanges **************************************/
        {
            // Retrieves content of exchanges
            List<String> lines = Files.readAllLines(Paths.get("src/main/resources", "lettres/list.txt"), Charset.defaultCharset());

            // Perform the parsing of the list
            lines.stream()
                    .filter(line -> (line != null && !line.isEmpty() && !line.startsWith("#")))
                    .forEach(line -> {
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
                        if (
                                ("Paris".equalsIgnoreCase(sendFrom) && "Dijon".equalsIgnoreCase(sendTo))
                                        ||
                                        ("Dijon".equalsIgnoreCase(sendFrom) && "Paris".equalsIgnoreCase(sendTo))
                                ) {
                            if (!number.contains("449-68") && !number.contains("449-90 bis") && !number.contains("449-91") && !number.contains("449-99") && !number.contains("449-100")) {
                                MessageExchange messageExchange = entityManager.persist(number, sendDate, sendFrom, sendTo, messageType);
                                mentions.stream().filter(p -> p != null).forEach(p -> entityManager.mention(messageExchange, p));
                            }
                        }

                    });
        }
        // Commit the transaction
        entityManager.commit();


        LOG.info("############################## Weld::shutdown() ##############################");
        weld.shutdown();
    }


    @Test
    public void user_story_05_all_exchanges_between_paris_and_dijon_sender_recipients_mentions() throws IOException {
        LOG.info("****************************** AllAuthorsAndMessageExchangesUST::user_story_05_all_exchanges_between_paris_and_dijon_mentions() ******************************");
        System.setProperty("DB_NAME", "data/graph05.db");

        LOG.info("############################## Weld::init() ##############################");
        weld = new Weld();
        weldContainer = weld.initialize();
        Runtime.getRuntime().addShutdownHook(new Thread(weld::shutdown));

        // Get EntityManager
        GraphEntityManager entityManager = weldContainer.instance().select(GraphEntityManager.class).get();

        // Starts a transaction
        entityManager.beginTransaction();

        LOG.info("MATCH (l:Lettre)-[r:EXPED|:DEST|:AUTRE_DEST|:MENTION]-(p:Personne) \n" +
                "WHERE \n\t(l.lieu_envoi = \"Paris\" AND l.lieu_reception = \"Dijon\") \n" +
                "\tOR (l.lieu_envoi = \"Dijon\" AND l.lieu_reception = \"Paris\") \n" +
                "return l,r,p");

        /******************************************************************************************************/
        /*************************************** Insert People ************************************************/
        {
            // Retrieves content of person list
            List<String> lines = Files.readAllLines(Paths.get("src/main/resources", "personnes/list.txt"), Charset.defaultCharset());

            // Perform the parsing of the list
            lines.stream()
                    .filter(line -> (line != null && !line.isEmpty() && !line.startsWith("#")))
                    .forEach(line -> {
                        String[] properties = line.split("\t");
                        entityManager.persist(
                                properties[0].trim(),
                                (properties.length > 1 ? properties[1].trim() : null),
                                (properties.length > 2 && "oui".equalsIgnoreCase(properties[2].trim()))
                        );
                    });
        }
        /******************************************************************************************************/


        /******************************************************************************************************/
        /*************************************** Insert MessageExchanges **************************************/
        {
            // Retrieves content of exchanges
            List<String> lines = Files.readAllLines(Paths.get("src/main/resources", "lettres/list.txt"), Charset.defaultCharset());

            // Perform the parsing of the list
            lines.stream()
                    .filter(line -> (line != null && !line.isEmpty() && !line.startsWith("#")))
                    .forEach(line -> {
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
                        if (
                                ("Paris".equalsIgnoreCase(sendFrom) && "Dijon".equalsIgnoreCase(sendTo))
                                        ||
                                        ("Dijon".equalsIgnoreCase(sendFrom) && "Paris".equalsIgnoreCase(sendTo))
                                ) {
                            if (!number.contains("449-68") && !number.contains("449-90 bis") && !number.contains("449-91") && !number.contains("449-99") && !number.contains("449-100")) {
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
                                mentions.stream().filter(p -> p != null).forEach(p -> entityManager.mention(messageExchange, p));
                            }
                        }

                    });
        }
        // Commit the transaction
        entityManager.commit();


        LOG.info("############################## Weld::shutdown() ##############################");
        weld.shutdown();
    }


    @Test
    public void user_story_06_some_exchanges_sender_recipients_mentions() throws IOException {
        LOG.info("****************************** AllAuthorsAndMessageExchangesUST::user_story_06_some_exchanges_sender_recipients_mentions() ******************************");
        System.setProperty("DB_NAME", "data/graph06.db");

        LOG.info("############################## Weld::init() ##############################");
        weld = new Weld();
        weldContainer = weld.initialize();
        Runtime.getRuntime().addShutdownHook(new Thread(weld::shutdown));

        // Get EntityManager
        GraphEntityManager entityManager = weldContainer.instance().select(GraphEntityManager.class).get();

        // Starts a transaction
        entityManager.beginTransaction();

        LOG.info("MATCH (l:Lettre)-[r:EXPED|:DEST|:AUTRE_DEST|:MENTION]-(p:Personne) \n" +
                "WHERE \n\tl.numero IN ['B 449-68', 'B 449-90 bis', 'B 449-91', 'B 449-99', 'B 449-100', 'B 449-126', 'B 451-26', 'B 481-25', 'B 449-113', 'B 449-96'] \n" +
                "return l,r,p");

        /******************************************************************************************************/
        /*************************************** Insert People ************************************************/
        {
            // Retrieves content of person list
            List<String> lines = Files.readAllLines(Paths.get("src/main/resources", "personnes/list.txt"), Charset.defaultCharset());

            // Perform the parsing of the list
            lines.stream()
                    .filter(line -> (line != null && !line.isEmpty() && !line.startsWith("#")))
                    .forEach(line -> {
                        String[] properties = line.split("\t");
                        entityManager.persist(
                                properties[0].trim(),
                                (properties.length > 1 ? properties[1].trim() : null),
                                (properties.length > 2 && "oui".equalsIgnoreCase(properties[2].trim()))
                        );
                    });
        }
        /******************************************************************************************************/


        /******************************************************************************************************/
        /*************************************** Insert MessageExchanges **************************************/
        {
            // Retrieves content of exchanges
            List<String> lines = Files.readAllLines(Paths.get("src/main/resources", "lettres/list.txt"), Charset.defaultCharset());

            // Perform the parsing of the list
            lines.stream()
                    .filter(line -> (line != null && !line.isEmpty() && !line.startsWith("#")))
                    .forEach(line -> {
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
                        if (number.contains("449-68") || number.contains("449-90 bis") || number.contains("449-91") || number.contains("449-99") || number.contains("449-100") ||
                                number.contains("449-126") || number.contains("451-26") || number.contains("481-25") || number.contains("449-113") || number.contains("449-96")) {
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
                            mentions.stream().filter(p -> p != null).forEach(p -> entityManager.mention(messageExchange, p));
                        }

                    });
        }
        // Commit the transaction
        entityManager.commit();


        LOG.info("############################## Weld::shutdown() ##############################");
        weld.shutdown();
    }


    @Test
    public void user_story_07_all_exchanges_sender_recipients_mentions_private() throws IOException {
        LOG.info("****************************** AllAuthorsAndMessageExchangesUST::user_story_07_all_exchanges_sender_recipients_mentions_private() ******************************");
        System.setProperty("DB_NAME", "data/graph07.db");

        LOG.info("############################## Weld::init() ##############################");
        weld = new Weld();
        weldContainer = weld.initialize();
        Runtime.getRuntime().addShutdownHook(new Thread(weld::shutdown));

        // Get EntityManager
        GraphEntityManager entityManager = weldContainer.instance().select(GraphEntityManager.class).get();

        // Starts a transaction
        entityManager.beginTransaction();

        LOG.info("MATCH (l:Lettre)-[r:EXPED|:DEST|:AUTRE_DEST|:MENTION]-(p:Personne) \n" +
                "WHERE \n\t(NOT l.numero IN ['B 449-126', 'B 451-26', 'B 481-25']) \n" +
                "\tAND (l.type_message IN ['PRIVATE', 'PRIVATE_PROF']) \n" +
                "return l,r,p");

        /******************************************************************************************************/
        /*************************************** Insert People ************************************************/
        {
            // Retrieves content of person list
            List<String> lines = Files.readAllLines(Paths.get("src/main/resources", "personnes/list.txt"), Charset.defaultCharset());

            // Perform the parsing of the list
            lines.stream()
                    .filter(line -> (line != null && !line.isEmpty() && !line.startsWith("#")))
                    .forEach(line -> {
                        String[] properties = line.split("\t");
                        entityManager.persist(
                                properties[0].trim(),
                                (properties.length > 1 ? properties[1].trim() : null),
                                (properties.length > 2 && "oui".equalsIgnoreCase(properties[2].trim()))
                        );
                    });
        }
        /******************************************************************************************************/


        /******************************************************************************************************/
        /*************************************** Insert MessageExchanges **************************************/
        {
            // Retrieves content of exchanges
            List<String> lines = Files.readAllLines(Paths.get("src/main/resources", "lettres/list.txt"), Charset.defaultCharset());

            // Perform the parsing of the list
            lines.stream()
                    .filter(line -> (line != null && !line.isEmpty() && !line.startsWith("#")))
                    .forEach(line -> {
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
                        if (messageType.equals(MessageType.PRIVATE) || messageType.equals(MessageType.PRIVATE_PROF)) {
                            if (!number.contains("449-126") && !number.contains("451-26") && !number.contains("481-25")) {
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
                                mentions.stream().filter(p -> p != null).forEach(p -> entityManager.mention(messageExchange, p));
                            }
                        }
                    });
        }
        // Commit the transaction
        entityManager.commit();


        LOG.info("############################## Weld::shutdown() ##############################");
        weld.shutdown();
    }
}
