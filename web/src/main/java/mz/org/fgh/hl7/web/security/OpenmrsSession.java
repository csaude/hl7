package mz.org.fgh.hl7.web.security;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpenmrsSession {
    private boolean isAuthenticated;
    private OpenmrsUser user;
}
