package com.nexusapp.back_end;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NexusApplication {

	public static void main(String[] args) {
        loadDotenvFrom(".");
        loadDotenvFrom("./nexusapp");
		SpringApplication.run(NexusApplication.class, args);
	}

    private static void loadDotenvFrom(String directory) {
        Dotenv dotenv = Dotenv.configure()
                .directory(directory)
                .ignoreIfMissing()
                .load();
        dotenv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));
    }

}
