package eu.arrowhead.core.plantdescriptionengine.consumedservices.orchestrator.rulebackingstore.sql;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "pde_rule")
public class PdeRule {
    
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

    public int getId() {
        return id;
    }
 
    public void setId(int id) {
        this.id = id;
    }
}
