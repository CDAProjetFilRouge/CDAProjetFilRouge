package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.Address;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.repositories.AddressRepo;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepo addressRepository;
    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private AddressService addressService;

    private Address validAddress;

    @BeforeEach
    void setUp() {
        validAddress = new Address();
        validAddress.setStreet1("12 rue de la Paix");
        validAddress.setPostalCode("75002");
        validAddress.setCity("Paris");
        validAddress.setCountry("France");
    }

    // ---------------------------------------------------------------
    // addressChecker
    // ---------------------------------------------------------------

    @Test
    void addressChecker_streetWithDigitButNoLetter_throwsBadRequest() {
        validAddress.setStreet1("12345");
        assertThrows(BadRequestException.class, () -> addressService.addressChecker(validAddress));
    }

    @Test
    void addressChecker_streetWithLetterButNoDigit_throwsBadRequest() {
        validAddress.setStreet1("rue de la Paix");
        assertThrows(BadRequestException.class, () -> addressService.addressChecker(validAddress));
    }

    @Test
    void addressChecker_postalCodeNotFourOrFiveDigits_throwsBadRequest() {
        validAddress.setPostalCode("7A002");
        assertThrows(BadRequestException.class, () -> addressService.addressChecker(validAddress));
    }

    @Test
    void addressChecker_postalCodeFourDigits_passes() throws HttpException {
        validAddress.setPostalCode("1000");
        assertThat(addressService.addressChecker(validAddress)).isTrue();
    }

    @Test
    void addressChecker_blankCity_throwsBadRequest() {
        validAddress.setCity(" ");
        assertThrows(BadRequestException.class, () -> addressService.addressChecker(validAddress));
    }

    @Test
    void addressChecker_blankCountry_throwsBadRequest() {
        validAddress.setCountry(" ");
        assertThrows(BadRequestException.class, () -> addressService.addressChecker(validAddress));
    }

    // ---------------------------------------------------------------
    // createAddress
    // ---------------------------------------------------------------

    @Test
    void createAddress_identicalExisting_returnsExistingWithoutDuplicating() throws HttpException {
        Address existing = new Address();
        existing.setId(5L);
        when(addressRepository.findByStreet1AndPostalCodeAndCity(
                validAddress.getStreet1(), validAddress.getPostalCode(), validAddress.getCity()))
                .thenReturn(Optional.of(existing));

        Address result = addressService.createAddress(validAddress);

        assertThat(result).isEqualTo(existing);
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    void createAddress_newAddress_savesIt() throws HttpException {
        when(addressRepository.findByStreet1AndPostalCodeAndCity(
                validAddress.getStreet1(), validAddress.getPostalCode(), validAddress.getCity()))
                .thenReturn(Optional.empty());
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Address result = addressService.createAddress(validAddress);

        assertThat(result).isEqualTo(validAddress);
        verify(addressRepository).save(validAddress);
    }

    // ---------------------------------------------------------------
    // updateAddress
    // ---------------------------------------------------------------

    @Test
    void updateAddress_collidesWithAnotherExistingAddress_returnsThatOtherAddress() throws HttpException {
        Address existing = new Address();
        existing.setId(1L);
        Address otherAddress = new Address();
        otherAddress.setId(2L);
        when(addressRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(addressRepository.findByStreet1AndPostalCodeAndCity(
                validAddress.getStreet1(), validAddress.getPostalCode(), validAddress.getCity()))
                .thenReturn(Optional.of(otherAddress));

        Address result = addressService.updateAddress(1L, validAddress);

        assertThat(result).isEqualTo(otherAddress);
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    void updateAddress_noCollision_updatesExistingAddress() throws HttpException {
        Address existing = new Address();
        existing.setId(1L);
        when(addressRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(addressRepository.findByStreet1AndPostalCodeAndCity(
                validAddress.getStreet1(), validAddress.getPostalCode(), validAddress.getCity()))
                .thenReturn(Optional.empty());
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Address result = addressService.updateAddress(1L, validAddress);

        assertThat(result.getStreet1()).isEqualTo(validAddress.getStreet1());
        verify(addressRepository).save(existing);
    }
}
