package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.Address;
import fr.diginamic.hubevenementiel.entities.Club;
import fr.diginamic.hubevenementiel.enums.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;

public interface ClubRepo extends JpaRepository<Club, Long> {

    /**
     *
     * @param name name of the club you want to do a search on
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of club with pagination info
     */
    Page<Club> findByName(String name, Pageable pageable);

    /**
     *
     * @param category category of club you want to do a search on
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of club with pagnation info
     */
    Page<Club> findByCategory(Category category, Pageable pageable);

    /**
     *
     * @param dateMin minimal date at which you want to do the search on
     * @param dateMax maximal date at which you want to do the serach on
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of club with pagination info
     */
    Page<Club> findByEndValidiyDateBetween(LocalDate dateMin, LocalDate dateMax, Pageable pageable);

    /**
     *
     * @param address address object to search club possessing it
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of club with pagination info
     */
    Page<Club> findByAddress(Address address, Pageable pageable);
}
