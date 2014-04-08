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
    static final String SENDING_FROM = "lieu_envoi";
    static final String SENDING_TO = "lieu_reception";
    static final String MESSAGE_TYPE = "type_message";

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

    public Integer getSendingDate() {
        Integer sendingDate = null;
        if (underlyingNode.hasProperty(SENDING_DATE)) {
            Object strValue = underlyingNode.getProperty(SENDING_DATE, null);
            if (strValue != null) {
                sendingDate = (Integer) strValue;
            }
        }
        return sendingDate;
    }

    public void setSendingDate(final Integer sendingDate) {
        if (sendingDate != null) {
            underlyingNode.setProperty(SENDING_DATE, sendingDate.intValue());
        } else {
            underlyingNode.removeProperty(SENDING_DATE);
        }
    }

    public String getSendingFromLocation() {
        return (underlyingNode.hasProperty(SENDING_FROM) && underlyingNode.getProperty(SENDING_FROM, null) != null ? ((String) underlyingNode.getProperty(SENDING_FROM)) : null);
    }

    public void setSendingFromLocation(final String sendingFromLocation) {
        if (sendingFromLocation != null) {
            underlyingNode.setProperty(SENDING_FROM, sendingFromLocation);
        } else {
            underlyingNode.removeProperty(SENDING_FROM);
        }
    }

    public String getSendingToLocation() {
        return (underlyingNode.hasProperty(SENDING_TO) && underlyingNode.getProperty(SENDING_TO, null) != null ? ((String) underlyingNode.getProperty(SENDING_TO)) : null);
    }

    public void setSendingToLocation(final String sendingToLocation) {
        if (sendingToLocation != null) {
            underlyingNode.setProperty(SENDING_TO, sendingToLocation);
        } else {
            underlyingNode.removeProperty(SENDING_TO);
        }
    }

    public MessageType getMessageType() {
        return (underlyingNode.hasProperty(MESSAGE_TYPE) && underlyingNode.getProperty(MESSAGE_TYPE, null) != null ? (MessageType.valueOf((String) underlyingNode.getProperty(MESSAGE_TYPE))) : null);
    }

    public void setMessageType(final MessageType messageType) {
        if (messageType != null) {
            underlyingNode.setProperty(MESSAGE_TYPE, messageType.name());
        } else {
            underlyingNode.removeProperty(MESSAGE_TYPE);
        }
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
