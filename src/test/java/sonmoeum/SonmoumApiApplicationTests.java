package sonmoeum;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SonmoumApiApplicationTests {
	private static final Logger log = LoggerFactory.getLogger(SonmoumApiApplicationTests.class);

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void contextLoads() {
		log.info("Password `admin1234` encoded: {}", passwordEncoder.encode("admin1234"));
	}

}
