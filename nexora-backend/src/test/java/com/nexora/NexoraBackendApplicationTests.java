package com.nexora;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class NexoraBackendApplicationTests {

	@Test
	void contextLoads() {
		// Valida que o contexto Spring sobe sem erros
	}

}
