package fr.diginamic.hubevenementiel.validation;

import java.util.regex.Pattern;

public final class PhoneNumberFormat {

    private static final Pattern FRENCH_NUMBER = Pattern
            .compile("^(?:(?:\\+|00)33[ .-]?|0)[1-9](?:[ .-]?\\d{2}){4}$");

    private PhoneNumberFormat() {
    }

    public static boolean isValid(String phone) {
        return phone != null && FRENCH_NUMBER.matcher(phone.trim()).matches();
    }
}