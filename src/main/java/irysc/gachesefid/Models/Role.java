package irysc.gachesefid.Models;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
    ROLE_ADMIN, ROLE_CLIENT, ROLE_ADVISOR,
    ROLE_CONTENT, ROLE_EDITOR, ROLE_AGENT,
    ROLE_SCHOOL, ROLE_TEACHER, ROLE_SUPER_ADMIN
    ;

    public String getAuthority() {
        return name();
    }

}
