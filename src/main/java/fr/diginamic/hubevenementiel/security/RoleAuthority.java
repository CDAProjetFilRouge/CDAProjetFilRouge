package fr.diginamic.hubevenementiel.security;

import org.springframework.security.core.GrantedAuthority;

public class RoleAuthority implements GrantedAuthority {

    private final String name;

    public RoleAuthority(String name) {
        this.name = name;
    }

    @Override
    public String getAuthority() {
        return name;
    }
}
