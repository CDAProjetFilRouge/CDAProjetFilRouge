package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageRepo extends JpaRepository<Image, Long> {

    /**
     *
     * @param eventId id of the event you want to find all the images of
     * @return an ordered list of image
     */
    List<Image> findByEventIdOrderByDisplayOrderAsc(Long eventId);

    /**
     *
     * @param eventId id of the event you want to search images of
     * @return numbers of images for this event
     */
    long countByEventId(Long eventId);
}
