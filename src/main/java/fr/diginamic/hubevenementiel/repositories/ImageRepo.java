package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageRepo extends JpaRepository<Image, Long> {

    List<Image> findByEventIdOrderByDisplayOrderAsc(Long eventId);

    long countByEventId(Long eventId);
}
