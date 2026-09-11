package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.Image;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepo extends JpaRepository<Image, Long> {

}
