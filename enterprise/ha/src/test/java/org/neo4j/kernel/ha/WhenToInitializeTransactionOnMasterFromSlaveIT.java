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
package org.neo4j.kernel.ha;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.ha.com.master.MasterImpl;
import org.neo4j.kernel.impl.ha.ClusterManager;
import org.neo4j.kernel.monitoring.Monitors;
import org.neo4j.test.ha.ClusterRule;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.neo4j.helpers.collection.Iterables.count;

/**
 * Determines when slaves should initialize a transaction on the master. This is particularly relevant for read operations
 * where we want slaves to be fast, and preferably not go to the master at all.
 */
public class WhenToInitializeTransactionOnMasterFromSlaveIT
{
    @Rule
    public ClusterRule clusterRule = new ClusterRule();

    private GraphDatabaseService slave;
    private ClusterManager.ManagedCluster cluster;

    private MasterImpl.Monitor masterMonitor = mock(MasterImpl.Monitor.class);

    @Before
    public void setUp()
    {
        cluster = clusterRule.startCluster();
        slave = cluster.getAnySlave();

        // Create some basic data
        try ( Transaction tx = slave.beginTx() )
        {
            Node node = slave.createNode( Label.label( "Person" ) );
            node.setProperty( "name", "Bob" );
            node.createRelationshipTo( slave.createNode(), RelationshipType.withName( "KNOWS" ) );

            tx.success();
        }

        // And now monitor the master for incoming calls
        cluster.getMaster().getDependencyResolver().resolveDependency( Monitors.class ).addMonitorListener( masterMonitor );
    }

    @Test
    public void shouldNotInitializeTxOnReadOnlyOpsOnNeoXaDS()
    {
        long nodeId = 0L;

        try ( Transaction transaction = slave.beginTx() )
        {
            // When
            Node node = slave.getNodeById( nodeId );

            // Then
            assertDidntStartMasterTx();

            // When
            count( node.getLabels() );

            // Then
            assertDidntStartMasterTx();

            // When
            readAllRels( node );

            // Then
            assertDidntStartMasterTx();

            // When
            readEachProperty( node );

            // Then
            assertDidntStartMasterTx();

            transaction.success();
        }

        // Finally
        assertDidntStartMasterTx();
    }

    private void assertDidntStartMasterTx()
    {
        verifyNoMoreInteractions( masterMonitor );
    }

    private void readAllRels( Node node )
    {
        for ( Relationship relationship : node.getRelationships() )
        {
            readEachProperty( relationship );
        }
    }

    private void readEachProperty( PropertyContainer entity )
    {
        for ( String k : entity.getPropertyKeys() )
        {
            entity.getProperty( k );
        }
    }

}
