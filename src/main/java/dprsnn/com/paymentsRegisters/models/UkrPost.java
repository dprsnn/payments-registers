package dprsnn.com.paymentsRegisters.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class UkrPost {
    @Id
    @Column(nullable = false, unique = true)
    private String ttn;

    private Long crmId;

    public String getTtn() {
        return ttn;
    }

    public void setTtn(String ttn) {
        this.ttn = ttn;
    }

    public Long getCrmId() {
        return crmId;
    }

    public void setCrmId(Long crmId) {
        this.crmId = crmId;
    }
}
