package eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.sql;

import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStore;
import eu.arrowhead.core.plantdescriptionengine.pdtracker.backingstore.PdStoreException;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntryBuilder;
import eu.arrowhead.core.plantdescriptionengine.providedservices.pde_mgmt.dto.PlantDescriptionEntryDto;
import se.arkalix.dto.DtoReadException;
import se.arkalix.dto.DtoWriteException;
import se.arkalix.dto.binary.ByteArrayReader;
import se.arkalix.dto.binary.ByteArrayWriter;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;


/**
 * Class that reads and writes Plant Description Entries to an SQL database.
 */
public class SqlPdStoreHib implements PdStore {

    protected SessionFactory sessionFactory = null; // TODO: Reuse the same session factory for all SQL

    /**
     * Throws an {@code IllegalStateException} if this instance has not been
     * initialized.
     */
    private void ensureInitialized() {
        if (sessionFactory == null) {
            throw new IllegalStateException("SqlPdStore has not been initialized.");
        }
    }

    /**
     * Retrieves all Plant Descriptions from the database.
     * @param session Object used to interact with the database.
     * @return A list of all Plant Descriptions in the database.
     */
    private List<PlantDescription> queryAllPlantDescriptions(Session session) {

        CriteriaQuery<PlantDescription> query = session.getCriteriaBuilder().createQuery(PlantDescription.class);
        Root<PlantDescription> rootEntry = query.from(PlantDescription.class);
        CriteriaQuery<PlantDescription> all = query.select(rootEntry);
        List<PlantDescription> plantDescriptions = session.createQuery(all).getResultList();

        // Or, we could simply do this:
        // List<PdeRule> rules = session.createQuery("from PdeRule rule", PdeRule.class).list();

        return plantDescriptions;
    }

    /**
     * Initializes the Plant Description store for use by connecting to the
     * database.
     */
    public void init() throws PdStoreException {
        final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
            .configure() // configures settings from hibernate.cfg.xml
            .build();
        try {
            sessionFactory = new MetadataSources(registry).buildMetadata().buildSessionFactory();
        } catch (Exception e) {
            StandardServiceRegistryBuilder.destroy(registry);
            throw new PdStoreException("Failed to initialize PD store", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PlantDescriptionEntryDto> readEntries() {
        ensureInitialized();
        List<PlantDescriptionEntryDto> result = new ArrayList<>();
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        List<PlantDescription> descriptions = queryAllPlantDescriptions(session);
        session.close();

        for (PlantDescription description : descriptions) {
            System.out.println(description);
            result.add(new PlantDescriptionEntryBuilder()
                .plantDescription(description.getPlantDescription())
                .build());
        }
        // return new PlantDescriptionEntryBuilder().build();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final PlantDescriptionEntryDto entry) {
        ensureInitialized();
        Objects.requireNonNull(entry, "Expected entry.");

        ensureInitialized();

        Session session = sessionFactory.openSession();
        session.beginTransaction();

        PlantDescription description = new PlantDescription();
        description.setId(entry.id());
        description.setPlantDescription(entry.plantDescription());
        session.save(description);

        session.getTransaction().commit();
        session.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(final int id) throws PdStoreException {
        ensureInitialized();
        try {
            final PreparedStatement statement = connection.prepareStatement(SQL_DELETE_ONE);
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (final SQLException e) {
            throw new PdStoreException("Failed to delete Plant Description entry", e);
        }
    }

    /**
     * Removes all Plant Description Entries from the store.
     */
    public void removeAll() throws PdStoreException {
        ensureInitialized();
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        final List<PlantDescription> descriptions = queryAllPlantDescriptions(session);
        for (PlantDescription description : descriptions) {
            session.delete(description);
        }
        session.getTransaction().commit();
        session.close();
    }
}