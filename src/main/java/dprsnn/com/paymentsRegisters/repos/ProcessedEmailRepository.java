package dprsnn.com.paymentsRegisters.repos;

import dprsnn.com.paymentsRegisters.models.ProcessedEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcessedEmailRepository extends JpaRepository<ProcessedEmail, Long> {

    Optional<ProcessedEmail> findByMessageId(String messageId);

    boolean existsByMessageId(String messageId);
}

