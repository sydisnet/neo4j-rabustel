package eu.sydisnet.neo4j.rabustel.suites;

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

import eu.sydisnet.neo4j.rabustel.control.GraphEntityManagerTest;
import eu.sydisnet.neo4j.rabustel.engine.DatabaseFactoryTest;
import eu.sydisnet.neo4j.rabustel.engine.Neo4jStartStopTest;
import eu.sydisnet.neo4j.rabustel.model.MessageTypesTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Tests Suite definition.
 *
 * Created by shebert on 29/03/14.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        MessageTypesTest.class,
        Neo4jStartStopTest.class,
        DatabaseFactoryTest.class,
        GraphEntityManagerTest.class
})
public class TestsSuiteLauncher {
}
