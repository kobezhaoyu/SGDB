/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html) with the
 * Commons Clause, as found in the associated LICENSE.txt file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * Neo4j object code can be licensed independently from the source
 * under separate terms from the AGPL. Inquiries can be directed to:
 * licensing@neo4j.com
 *
 * More information is also available at:
 * https://neo4j.com/licensing/
 */
package org.neo4j.causalclustering.discovery;

import com.hazelcast.logging.AbstractLogger;
import com.hazelcast.logging.LogEvent;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.neo4j.logging.Log;
import org.neo4j.logging.Logger;
import org.neo4j.logging.NullLogger;

public class HazelcastLogger extends AbstractLogger
{
    private final Log log;
    private final Level minLevel;

    HazelcastLogger( Log log, Level minLevel )
    {
        this.log = log;
        this.minLevel = minLevel;
    }

    @Override
    public void log( Level level, String message )
    {
        getLogger( level ).log( message );
    }

    @Override
    public void log( Level level, String message, Throwable thrown )
    {
        getLogger( level ).log( message, thrown );
    }

    @Override
    public void log( LogEvent logEvent )
    {
        LogRecord logRecord = logEvent.getLogRecord();

        String message = "Member[" + logEvent.getMember() + "] " + logRecord.getMessage();

        Logger logger = getLogger( logRecord.getLevel() );
        Throwable thrown = logRecord.getThrown();

        if ( thrown == null )
        {
            logger.log( message );
        }
        else
        {
            logger.log( message, thrown );
        }
    }

    @Override
    public Level getLevel()
    {
        return minLevel;
    }

    @Override
    public boolean isLoggable( Level level )
    {
        return level.intValue() >= minLevel.intValue();
    }

    private Logger getLogger( Level level )
    {
        int levelValue = level.intValue();

        if ( levelValue < minLevel.intValue() )
        {
            return NullLogger.getInstance();
        }
        else if ( levelValue <= Level.FINE.intValue() )
        {
            return log.debugLogger();
        }
        else if ( levelValue <= Level.INFO.intValue() )
        {
            return log.infoLogger();
        }
        else if ( levelValue <= Level.WARNING.intValue() )
        {
            return log.warnLogger();
        }
        else
        {
            return log.errorLogger();
        }
    }
}
