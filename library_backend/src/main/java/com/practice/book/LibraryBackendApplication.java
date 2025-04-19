package com.practice.book;

import com.practice.book.role.Role;
import com.practice.book.role.RoleRepository;
import org.apache.catalina.core.ApplicationContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
public class LibraryBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(LibraryBackendApplication.class, args);
	}

//	@Bean
//	public CommandLineRunner commandLineRunner(RoleRepository roleRepository) {
//		return args -> {
//			if (roleRepository.count() == 0) {
//				roleRepository.save(Role.builder().name("USER").build());
//			}
//		};
//	}

}
