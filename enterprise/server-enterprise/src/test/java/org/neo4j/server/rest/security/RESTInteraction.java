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
package org.neo4j.server.rest.security;

import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import javax.ws.rs.core.HttpHeaders;

import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.test.server.HTTP;

import static org.neo4j.test.server.HTTP.RawPayload.quotedJson;

class RESTInteraction extends AbstractRESTInteraction
{

    RESTInteraction( Map<String,String> config ) throws IOException
    {
        super( config );
    }

    @Override
    String commitPath()
    {
        return "db/data/transaction/commit";
    }

    @Override
    HTTP.RawPayload constructQuery( String query )
    {
        return quotedJson( "{'statements':[{'statement':'" +
                           query.replace( "'", "\\'" ).replace( "\"", "\\\"" )
                           + "'}]}" );
    }

    @Override
    void consume( Consumer<ResourceIterator<Map<String,Object>>> resultConsumer, JsonNode data )
    {
        if ( data.has( "results" ) && data.get( "results" ).has( 0 ) )
        {
            resultConsumer.accept( new RESTResult( data.get( "results" ).get( 0 ) ) );
        }
    }

    @Override
    protected HTTP.Response authenticate( String principalCredentials )
    {
        return HTTP.withHeaders( HttpHeaders.AUTHORIZATION, principalCredentials ).request( POST, commitURL() );
    }

    private class RESTResult extends AbstractRESTResult
    {
        RESTResult( JsonNode fullResult )
        {
            super( fullResult );
        }

        @Override
        protected JsonNode getRow( JsonNode data, int i )
        {
            return data.get( i ).get( "row" );
        }
    }
}
