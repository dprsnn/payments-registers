package dprsnn.com.paymentsRegisters.repos;

import dprsnn.com.paymentsRegisters.models.CrmCredentialsModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CrmCredentialsRepo extends JpaRepository<CrmCredentialsModel, Long> {
    CrmCredentialsModel findByParamName(String paramName);
}
