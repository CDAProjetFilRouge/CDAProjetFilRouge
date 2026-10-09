package fr.diginamic.hubevenementiel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HubEvenementielApplication {

    public static void main(String[] args) {
        SpringApplication.run(HubEvenementielApplication.class, args);
    }
}
