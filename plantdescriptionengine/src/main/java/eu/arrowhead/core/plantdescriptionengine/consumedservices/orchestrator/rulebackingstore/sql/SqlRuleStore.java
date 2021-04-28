package eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.rulebackingstore.sql;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.rulebackingstore.RuleStore;
import eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.rulebackingstore.RuleStoreException;

/**
 * Class that reads and writes Orchestration rules to an SQL database.
 */
public class SqlRuleStore implements RuleStore {

    protected SessionFactory sessionFactory = null; // TODO: Reuse the same session factory for all SQL

    /**
     * Throws an {@code IllegalStateException} if this instance has not been'
     * initialized.
     */
    private void ensureInitialized() {
        if (sessionFactory == null) {
            throw new IllegalStateException("SqlRuleStore has not been initialized.");
        }
    }

    /**
     * Initializes the rule store for use by connecting to the database.
     */
    public void init() throws RuleStoreException {

        final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
            .configure() // configures settings from hibernate.cfg.xml
            .build();
        try {
            sessionFactory = new MetadataSources(registry).buildMetadata().buildSessionFactory();
        } catch (Exception e) {
            e.printStackTrace();
            StandardServiceRegistryBuilder.destroy(registry);
            throw new RuleStoreException("Failed to initialize database connection.", e);
        }
    }

    // TODO: Call this at some point.
    public void exit() {
        sessionFactory.close();
    }

    private List<PdeRule> queryAllRules(Session session) {

        CriteriaQuery<PdeRule> query = session.getCriteriaBuilder().createQuery(PdeRule.class);
        Root<PdeRule> rootEntry = query.from(PdeRule.class);
        CriteriaQuery<PdeRule> all = query.select(rootEntry);
        List<PdeRule> rules = session.createQuery(all).getResultList();

        // Or, we could simply do this:
        // List<PdeRule> rules = session.createQuery("from PdeRule rule", PdeRule.class).list();

        return rules;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Integer> readRules() throws RuleStoreException {
        ensureInitialized();
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        Set<Integer> rules = queryAllRules(session).stream().map(rule -> rule.getId()).collect(Collectors.toSet());
        session.close();
        return rules;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRules(final Set<Integer> rules) throws RuleStoreException {
        Objects.requireNonNull(rules, "Expected rules.");
        ensureInitialized();

        Session session = sessionFactory.openSession();
        session.beginTransaction();

        for (final Integer ruleId : rules) {
            
            PdeRule rule = new PdeRule();
            rule.setId(ruleId);
            session.save(rule);

        }

        session.getTransaction().commit();
        session.close();
    }

    /**
     * {@inheritDoc}
     *
     * @throws RuleStoreException
     */
    @Override
    public void removeAll() throws RuleStoreException {
        ensureInitialized();
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        final List<PdeRule> rules = queryAllRules(session);
        for (Object obj : rules) {
            session.delete(obj);
        }
        session.getTransaction().commit();
        session.close();
    }
}