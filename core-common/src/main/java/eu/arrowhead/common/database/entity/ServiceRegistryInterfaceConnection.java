package eu.arrowhead.common.database.entity;

import java.time.ZonedDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table (uniqueConstraints = @UniqueConstraint(columnNames = {"serviceRegistryId", "interfaceId"}))
public class ServiceRegistryInterfaceConnection {
	
	@Id
	@GeneratedValue (strategy = GenerationType.IDENTITY)
	private long id;
	
	@ManyToOne (fetch = FetchType.EAGER)
	@JoinColumn (name = "serviceRegistryId", referencedColumnName = "id", nullable = false)
	private ServiceRegistry serviceRegistryEntry;
	
	@ManyToOne (fetch = FetchType.EAGER)
	@JoinColumn (name = "interfaceId", referencedColumnName = "id", nullable = false)
	private ServiceInterface serviceInterface;
	
	@Column (nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	private ZonedDateTime createdAt;
	
	@Column (nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
	private ZonedDateTime updatedAt;

	public ServiceRegistryInterfaceConnection() {
	
	}

	public ServiceRegistryInterfaceConnection(final ServiceRegistry serviceRegistryEntry, final ServiceInterface serviceInterface) {
		this.serviceRegistryEntry = serviceRegistryEntry;
		this.serviceInterface = serviceInterface;
	}

	public long getId() {
		return id;
	}

	public void setId(final long id) {
		this.id = id;
	}

	public ServiceRegistry getServiceRegistryEntry() {
		return serviceRegistryEntry;
	}

	public void setServiceRegistryEntry(final ServiceRegistry serviceRegistryEntry) {
		this.serviceRegistryEntry = serviceRegistryEntry;
	}

	public ServiceInterface getServiceInterface() {
		return serviceInterface;
	}

	public void setServiceInterface(final ServiceInterface serviceInterface) {
		this.serviceInterface = serviceInterface;
	}

	public ZonedDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(final ZonedDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public ZonedDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(final ZonedDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	@Override
	public String toString() {
		return "ServiceRegistryInterfaceConnection [id=" + id + ", serviceRegistryEntry=" + serviceRegistryEntry
				+ ", serviceInterface=" + serviceInterface + "]";
	}
	
}