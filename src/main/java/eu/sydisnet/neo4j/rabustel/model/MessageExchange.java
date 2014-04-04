package eu.sydisnet.neo4j.rabustel.model;

import org.neo4j.graphdb.Node;

import javax.json.Json;
import javax.json.JsonWriter;
import java.io.StringWriter;

/**
 * Model: MessageExchange.
 * <p>
 * Created by shebert on 04/04/14.
 */
public class MessageExchange {

    public static final String LABEL = "Lettre";

    public static final String NUMBER = "numero";
    static final String SENDING_DATE = "date_envoi";
    static final String SENDIND_FROM = "lieu_envoi";
    static final String SENDIND_TO = "lieu_reception";

    private final Node underlyingNode;

    public MessageExchange(final Node underlyingNode) {
        this.underlyingNode = underlyingNode;
    }

    public Node getUnderlyingNode() {
        return underlyingNode;
    }

    public String getNumber() {
        return (String) underlyingNode.getProperty(NUMBER);
    }

    public void setNumber(final String number) {
        underlyingNode.setProperty(NUMBER, number);
    }


    @Override
    public boolean equals(final Object o) {
        return o instanceof MessageExchange &&
                underlyingNode.equals(((MessageExchange) o).getUnderlyingNode());
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
                            .add(NUMBER, getNumber())
                            .build()
            );
        }

        return String.format("MessageExchange %s", stWriter.toString());
    }
}
