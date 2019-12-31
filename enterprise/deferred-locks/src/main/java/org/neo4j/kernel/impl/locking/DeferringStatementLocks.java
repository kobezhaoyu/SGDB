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
package org.neo4j.kernel.impl.locking;

import java.util.stream.Stream;

/**
 * A {@link StatementLocks} implementation that defers {@link #optimistic() optimistic}
 * locks using {@link DeferringLockClient}.
 */
public class DeferringStatementLocks implements StatementLocks
{
    private final Locks.Client explicit;
    private final DeferringLockClient implicit;

    public DeferringStatementLocks( Locks.Client explicit )
    {
        this.explicit = explicit;
        this.implicit = new DeferringLockClient( this.explicit );
    }

    @Override
    public Locks.Client pessimistic()
    {
        return explicit;
    }

    @Override
    public Locks.Client optimistic()
    {
        return implicit;
    }

    @Override
    public void prepareForCommit( LockTracer lockTracer )
    {
        implicit.acquireDeferredLocks( lockTracer );
        explicit.prepare();
    }

    @Override
    public void stop()
    {
        implicit.stop();
    }

    @Override
    public void close()
    {
        implicit.close();
    }

    @Override
    public Stream<ActiveLock> activeLocks()
    {
        return Stream.concat( explicit.activeLocks(), implicit.activeLocks() );
    }

    @Override
    public long activeLockCount()
    {
        return explicit.activeLockCount() + implicit.activeLockCount();
    }
}
