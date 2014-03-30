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

import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for Relationships within a context such as a message exchange.
 *
 * Created by shebert on 15/03/14.
 */
public class MessageTypesTest {

    @Test
    public void should_be_deal_with_private_or_prof_exchanges() {
        // Given
        MessageType priv = MessageType.PRIVATE;
        MessageType prof = MessageType.PROF;


        // Expect
        assertThat("Should be PRIVATE", priv.name(), is("PRIVATE"));
        assertThat("Should be greater than or equals to ZERO", prof.ordinal(), greaterThanOrEqualTo(0));
    }

    @Test
    public void should_be_deal_with_private_prof_exchanges() {
        // Given
        MessageType type = MessageType.PRIVATE_PROF;

        // Expect
        assertThat("Should be PRIVATE_PROF", type.toString(), is("PRIVATE_PROF"));
        assertThat("Should contains \"Privé\" then \"professionnnel\"",
                type.getDescription(),
                stringContainsInOrder(Arrays.asList("Privé", "propos professionnel")));
    }

    @Test
    public void should_be_deal_with_prof_private_exchanges() {
        // Given
        MessageType type = MessageType.PROF_PRIVATE;

        // Expect
        assertThat("Should contains \"Professionnel\" then \"privé\"",
                type.getDescription(),
                stringContainsInOrder(Arrays.asList("Professionnel", "mentions privées")));
    }
}
