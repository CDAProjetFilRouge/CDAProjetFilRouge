package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.Address;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface AddressRepo extends JpaRepository<Address, Long> {

    /**
     *
     * @param code Code of the city (e.g. 34110)
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return A list of Address as the result with pagination info
     */
    Page<Address> findByPostalCode(String code, Pageable pageable);

    /**
     *
     * @param city name of the city (e.g. Montpellier)
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return A list of Address as the result with pagination info
     */
    Page<Address> findByCity(String city, Pageable pageable);

    /**
     *
     * @param country name of the country (e.g. France)
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return A list of Address as the result with pagination info
     */
    Page<Address> findByCountry(String country, Pageable pageable);

    Optional<Address> findByStreet1AndPostalCodeAndCity(String street1, String postalCode, String city);
}
