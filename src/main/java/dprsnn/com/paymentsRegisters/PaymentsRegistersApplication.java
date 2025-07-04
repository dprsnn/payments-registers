package dprsnn.com.paymentsRegisters;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PaymentsRegistersApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentsRegistersApplication.class, args);
	}

}
