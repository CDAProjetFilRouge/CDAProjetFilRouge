package fr.diginamic.hubevenementiel;

import java.util.UUID;

public final class TestIds {

    private TestIds() {
    }

    public static UUID uuid(long n) {
        return new UUID(0L, n);
    }
}
