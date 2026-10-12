package fr.diginamic.hubevenementiel.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneNumberFormatTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "0561234567",
            "05 61 23 45 67",
            "05.61.23.45.67",
            "05-61-23-45-67",
            "+33 5 61 23 45 67",
            "+33561234567",
            "0033 5 61 23 45 67",
            "06 12 34 56 78",
            "  05 61 23 45 67  "
    })
    void isValid_frenchNumbers_returnsTrue(String phone) {
        assertThat(PhoneNumberFormat.isValid(phone)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "123456",
            "------",
            "0000000000",
            "00 00 00 00 00",
            "056123456",
            "05612345678",
            "06ABCDEFGH",
            "+44 20 7946 0958",
            "05 61 23 45 6",
            "(05) 61 23 45 67",
            "05 61 23 45 67 89"
    })
    void isValid_otherStrings_returnsFalse(String phone) {
        assertThat(PhoneNumberFormat.isValid(phone)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "   " })
    void isValid_nullOrBlank_returnsFalse(String phone) {
        assertThat(PhoneNumberFormat.isValid(phone)).isFalse();
    }

    @Test
    void isValid_numberWithOnlyDigitsOfAnotherAlphabet_returnsFalse() {
        assertThat(PhoneNumberFormat.isValid("٠٥٦١٢٣٤٥٦٧")).isFalse();
    }
}
