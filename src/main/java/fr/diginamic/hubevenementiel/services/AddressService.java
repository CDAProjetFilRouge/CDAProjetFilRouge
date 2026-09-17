package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.Address;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.AddressRepo;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AddressService {

    private final AddressRepo addressRepository;
    private final UserRepo userRepo;

    public AddressService(AddressRepo addressRepository, UserRepo userRepo) {
        this.addressRepository = addressRepository;
        this.userRepo = userRepo;
    }

    /**
     *
     * @param page page at which you want to start
     * @param size number of entries per page
     * @return a list of address
     */
    public List<Address> findAllAddress(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return addressRepository.findAll(pageable).getContent();
    }

    /**
     *
     * @param addressId id of the address you want to find
     * @return an object of address
     * @throws HttpException
     */
    public Address findById(Long addressId) throws HttpException {
        Optional<Address> optionalAddress = addressRepository.findById(addressId);

        if (optionalAddress.isEmpty()) {
            throw new NotFoundException("Aucune adresse trouvée avec cet identifiant");
        }

        return optionalAddress.get();
    }

    /**
     *
     * @param address object of type address to add in the DB
     * @return the object address created
     * @throws HttpException
     */
    @Transactional
    public Address createAddress(Address address) throws HttpException {

        normalize(address);
        addressChecker(address);

        return addressRepository.findByStreet1AndPostalCodeAndCity(
                address.getStreet1(),
                address.getPostalCode(),
                address.getCity()
        ).orElseGet(() -> addressRepository.save(address));
    }

    /**
     *
     * @param id id of the address to updated
     * @param addressDetails objecting containing the update info
     * @return the updated object
     * @throws HttpException
     */
    @Transactional
    public Address updateAddress(Long id, Address addressDetails) throws HttpException {

        Address existing = addressRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Adresse introuvable avec l'id " + id));

        normalize(addressDetails);
        addressChecker(addressDetails);

        Optional<Address> duplicate = addressRepository.findByStreet1AndPostalCodeAndCity(
                addressDetails.getStreet1(),
                addressDetails.getPostalCode(),
                addressDetails.getCity()
        );

        if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
            return duplicate.get();
        }

        existing.setStreet1(addressDetails.getStreet1());
        existing.setStreet2(addressDetails.getStreet2());
        existing.setPostalCode(addressDetails.getPostalCode());
        existing.setCity(addressDetails.getCity());
        existing.setCountry(addressDetails.getCountry());

        return addressRepository.save(existing);
    }

    /**
     *
     * @param id id of the address to delete
     * @throws HttpException
     */
    @Transactional
    public void deleteAddress(Long id) throws HttpException {

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Adresse introuvable avec l'id " + id));

        addressRepository.delete(address);
    }

    /**
     *
     * @param idAddress id of the address to find
     * @param idUser id of the appUser to dissociate the address from
     * @throws HttpException
     */
    @Transactional
    public void anonymizeAddress(Long idAddress, Long idUser) throws HttpException {
        Address address = addressRepository.findById(idAddress).orElseThrow(() -> new NotFoundException("No address found with id: "+idAddress));
        AppUser appUser = userRepo.findById(idUser).orElseThrow(() -> new NotFoundException("No app user found with id: "+idUser));

        boolean removed = address.getUsers().removeIf(u -> u.getId().equals(appUser.getId()));

        if(!removed){
            throw new NotFoundException("AppUser associated with this address");
        }

        addressRepository.save(address);
    }

    /**
     *
     * @param address object of type address to check
     * @return true if all check passed else false
     * @throws HttpException
     */
    public boolean addressChecker(Address address) throws HttpException {

        if (address == null) {
            throw new BadRequestException("L'adresse ne peut pas être nulle.");
        }

        String street = address.getStreet1();
        if (street == null || street.isBlank()) {
            throw new BadRequestException("L'adresse ne peut pas être vide.");
        }

        boolean containsDigit = street.matches(".*\\d+.*");
        boolean containsLetter = street.matches(".*[a-zA-ZÀ-ÿ]+.*");
        if (!containsDigit || !containsLetter) {
            throw new BadRequestException("L'adresse doit contenir un numéro et un nom de rue.");
        }

        String postalCode = address.getPostalCode();
        if (postalCode == null || postalCode.isBlank() || !postalCode.matches("\\d{4,5}")) {
            throw new BadRequestException("Le code postal doit être composé de 4 ou 5 chiffres.");
        }

        String city = address.getCity();
        if (city == null || city.isBlank()) {
            throw new BadRequestException("Vous devez renseigner une ville.");
        }

        String country = address.getCountry();
        if (country == null || country.isBlank()) {
            throw new BadRequestException("Vous devez renseigner un pays.");
        }

        return true;
    }

    /**
     *
     * @param address object address to normalize
     */
    private void normalize(Address address) {
        if (address.getStreet1() != null) address.setStreet1(address.getStreet1().trim());
        if (address.getStreet2() != null) address.setStreet2(address.getStreet2().trim());
        if (address.getPostalCode() != null) address.setPostalCode(address.getPostalCode().trim());
        if (address.getCity() != null) address.setCity(address.getCity().trim());
        if (address.getCountry() != null) address.setCountry(address.getCountry().trim());
    }






}
