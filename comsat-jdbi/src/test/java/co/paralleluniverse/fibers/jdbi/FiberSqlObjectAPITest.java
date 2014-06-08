/*
 * COMSAT
 * Copyright (C) 2014, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
package co.paralleluniverse.fibers.jdbi;

import co.paralleluniverse.embedded.db.H2JdbcDatasource;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import javax.sql.DataSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

@RunWith(Parameterized.class)
public class FiberSqlObjectAPITest {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {H2JdbcDatasource.class},});
    }
    private final Class<? extends DataSource> cls;
    private MyDAO dao;

    public FiberSqlObjectAPITest(Class<? extends DataSource> cls) {
        this.cls = cls;
    }

    @Before
    public void setUp() throws Exception {
        this.dao = new FiberDBI(cls.newInstance()).onDemand(MyDAO.class);
    }

    @Test
    public void testDao() throws IOException, InterruptedException, Exception {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                dao.createSomethingTable();
                for (int i = 0; i < 100; i++)
                    dao.insert(i, "name" + i);
                Assert.assertEquals("name37", dao.findNameById(37));
                dao.dropSomethingTable();
            }
        }).start().join();
    }

    @Suspendable
    public interface MyDAO {
        @SqlUpdate("create table if not exists something (id int primary key, name varchar(100))")
        void createSomethingTable();

        @SqlUpdate("drop table something")
        void dropSomethingTable();

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind("id") int id);
    }

}