package mz.org.fgh.hl7.web.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

public class OpenmrsAuthenticationProvider implements AuthenticationProvider {

    private SessionService sessionService;

    public OpenmrsAuthenticationProvider(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        return authenticateViaOpenmrs(authentication.getName(), authentication.getCredentials().toString());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }

    private Authentication authenticateViaOpenmrs(String name, String password) {

        OpenmrsSession session = sessionService.getSession(name, password);

        if (!session.isAuthenticated()) {
            throw new BadCredentialsException("Authentication failed");
        }

        List<GrantedAuthority> grantedAuths = new ArrayList<>();
        grantedAuths.add(new SimpleGrantedAuthority("USER"));
        UserDetails principal = new User(name, password, grantedAuths);
        return new UsernamePasswordAuthenticationToken(principal, password, grantedAuths);
    }
}
