package dprsnn.com.paymentsRegisters.repos;

import dprsnn.com.paymentsRegisters.models.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogRepo extends JpaRepository<Log, Long> {
}
