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
 * <p>
 * The PDE needs to keep track of which Orchestration rules it has created, and
 * to which Plant Description Entry each rule belongs. This information is
 * stored in memory, but it also needs to be persisted to permanent storage in
 * case the PDE is restarted. This class provides that functionality, writing
 * rules and their relationship to Plant Descriptions to file.
 */
public class SqlRuleStore implements RuleStore {

    private final String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS pde_rule (id INT);";
    private final String SQL_SELECT_ALL_RULES = "select * from pde_rule;";
    private final String SQL_INSERT_RULE = "INSERT INTO pde_rule(id) VALUES(?);";
    private final String SQL_DELETE_ALL_RULES = "DELETE FROM pde_rule;";

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

    public void init(
        final String driverName,
        final String connectionUrl,
        final String username,
        final String password
    ) throws RuleStoreException {

        Objects.requireNonNull(driverName, "Expected database driver name.");
        Objects.requireNonNull(connectionUrl, "Expected connection URL.");
        Objects.requireNonNull(username, "Expected username.");
        Objects.requireNonNull(password, "Expected password.");

        try {
            Class.forName(driverName);
            connection = DriverManager.getConnection(connectionUrl, username, password);
            final Statement statement = connection.createStatement();
            statement.execute(SQL_CREATE_TABLE);

        } catch (final ClassNotFoundException | SQLException e) {
            System.out.println(e.getMessage());
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
                result.add(resultSet.getInt("id"));
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