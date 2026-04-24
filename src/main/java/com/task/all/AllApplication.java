package com.task.all;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(
		info = @Info(
				title = "All Task API",
				version = "1.0",
				description = "API REST para la gestión de tareas, checklist e ítems checkeables."
		)
)
@SpringBootApplication
public class AllApplication {

	public static void main(String[] args) {
		SpringApplication.run(AllApplication.class, args);
	}

}
