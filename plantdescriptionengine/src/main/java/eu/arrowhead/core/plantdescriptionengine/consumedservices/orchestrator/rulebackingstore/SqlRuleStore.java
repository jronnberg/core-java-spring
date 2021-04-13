package eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.rulebackingstore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Class that reads and writes Orchestration rules to an SQL database.
 */
public class SqlRuleStore implements RuleStore {

    private final String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS pde_rule (id INT);";
    private final String SQL_SELECT_ALL_RULES = "select * from pde_rule;";
    private final String SQL_INSERT_RULE = "INSERT INTO pde_rule(id) VALUES(?);";
    private final String SQL_DELETE_ALL_RULES = "DELETE FROM pde_rule;";
    private final String ID = "id";

    private Connection connection;

    /**
     * Throws an {@code IllegalStateException} if this instance has not been'
     * initialized.
     */
    private void ensureInitialized() {
        if (connection == null) {
            throw new IllegalStateException("SqlRuleStore has not been initialized.");
        }
    }

    /**
     * Initializes the rule store for use by connecting to the database and
     * creating the necessary tables.
     *
     * @param driverClassName The driver class for the mysql database.
     * @param connectionUrl   URL of the database connection.
     * @param username        Username to use when connecting to the database.
     * @param password        Password to use when connecting to the database.
     */
    public void init(
        final String driverClassName,
        final String connectionUrl,
        final String username,
        final String password
    ) throws RuleStoreException {

        Objects.requireNonNull(driverClassName, "Expected database driver name.");
        Objects.requireNonNull(connectionUrl, "Expected connection URL.");
        Objects.requireNonNull(username, "Expected username.");
        Objects.requireNonNull(password, "Expected password.");

        try {
            Class.forName(driverClassName);
            connection = DriverManager.getConnection(connectionUrl, username, password);
            final Statement statement = connection.createStatement();
            statement.execute(SQL_CREATE_TABLE);

        } catch (final ClassNotFoundException | SQLException e) {
            throw new RuleStoreException("Failed to initialize rule store", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Integer> readRules() throws RuleStoreException {
        ensureInitialized();

        try {
            final Set<Integer> result = new HashSet<>();
            final Statement statement = connection.createStatement();
            final ResultSet resultSet = statement.executeQuery(SQL_SELECT_ALL_RULES);

            while (resultSet.next()) {
                result.add(resultSet.getInt(ID));
            }

            return result;

        } catch (final SQLException e) {
            throw new RuleStoreException("Failed to read rules", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRules(final Set<Integer> rules) throws RuleStoreException {
        Objects.requireNonNull(rules, "Expected rules.");
        ensureInitialized();

        try {
            final PreparedStatement statement = connection.prepareStatement(SQL_INSERT_RULE);
            for (final Integer rule : rules) {
                statement.setInt(1, rule);
                statement.executeUpdate();
            }

        } catch (final SQLException e) {
            throw new RuleStoreException("Failed to write orchestration rules to database", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws RuleStoreException
     */
    @Override
    public void removeAll() throws RuleStoreException {
        ensureInitialized();
        try {
            final Statement statement = connection.createStatement();
            statement.execute(SQL_DELETE_ALL_RULES);
        } catch (final SQLException e) {
            throw new RuleStoreException("Failed to delete orchestration rules", e);
        }
    }
}