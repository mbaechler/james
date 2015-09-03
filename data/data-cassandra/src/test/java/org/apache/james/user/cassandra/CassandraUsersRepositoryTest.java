package org.apache.james.user.cassandra;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import static com.datastax.driver.core.querybuilder.QueryBuilder.update;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.CassandraDataDataModel;
import org.apache.james.backends.cassandra.CassandraClusterSingleton;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.lib.AbstractUsersRepositoryTest;
import org.junit.After;
import org.junit.Test;

public class CassandraUsersRepositoryTest extends AbstractUsersRepositoryTest {

    private CassandraClusterSingleton cassandra;
    
    @After
    public void tearDown() {
        cassandra.clearAllTables();
    }
    
    
    @Override
    protected UsersRepository getUsersRepository() throws Exception {
        cassandra = CassandraClusterSingleton.create(new CassandraDataDataModel());
        return new CassandraUsersRepository(cassandra.getConf());
    }

    
    @Test
    public void ifExistsShouldAppendConditionAtStatementEnd() {
        String actual = CassandraUsersRepository.ifExists(update("foo").with(set("w", 3)) .where(eq("k", 2)));
        assertThat(actual).isEqualTo("UPDATE foo SET w=3 WHERE k=2 IF EXISTS;");
    }
    
    @Test(expected=NullPointerException.class)
    public void ifExistsShouldThrowNullPointerExceptionOnNull() {
        CassandraUsersRepository.ifExists(null);
    }
    
    @Test
    public void givenStatementContainsSemiColumnIfExistsShouldAppendConditionAtStatementEnd() {
        String actual = CassandraUsersRepository.ifExists(update("foo").with(set("w", "bla;")) .where(eq("k", 2)));
        assertThat(actual).isEqualTo("UPDATE foo SET w='bla;' WHERE k=2 IF EXISTS;");
    }

}
