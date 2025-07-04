package dprsnn.com.paymentsRegisters.repos;

import dprsnn.com.paymentsRegisters.models.UkrPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UkrPostRepo extends JpaRepository<UkrPost, String> {
    Optional<UkrPost> findById(String ttn);

}
