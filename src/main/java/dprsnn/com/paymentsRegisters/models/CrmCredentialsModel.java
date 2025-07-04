package dprsnn.com.paymentsRegisters.models;

import jakarta.persistence.*;
import lombok.Data;


@Entity
@Table(name = "crm_credentials")
public class CrmCredentialsModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "param_name")
    private String paramName;
    @Column(name = "param_value")
    private String paramValue;

    public CrmCredentialsModel(){

    }
    public CrmCredentialsModel(String paramName, String paramValue) {

        this.paramName = paramName;

        this.paramValue = paramValue;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public String getParamValue() {
        return paramValue;
    }

    public void setParamValue(String paramValue) {
        this.paramValue = paramValue;
    }

}
