package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.Address;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AddressRepo extends CrudRepository<Address, Long> {

    /**
     *
     * @param code Code of the city (e.g. 34110)
     * @return A list of Address as the result
     */
    Page<Address> findByPostalCode(String code, Pageable pageable);

    /**
     *
     * @param city name of the city (e.g. Montpellier)
     * @return A list of Address as the result
     */
    Page<Address> findByCity(String city, Pageable pageable);

    /**
     *
     * @param country name of the country (e.g. France)
     * @return A list of Address as the result
     */
    Page<Address> findByCountry(String country, Pageable pageable);
}
