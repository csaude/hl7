package mz.org.fgh.hl7.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import mz.org.fgh.hl7.web.security.OpenmrsAuthenticationProvider;
import mz.org.fgh.hl7.web.security.SessionService;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Value("${app.username}")
    private String userName;

    @Value("${app.password}")
    private String passWord;

    @Bean
    @ConditionalOnProperty(name = "app.openmrs.login", havingValue = "false", matchIfMissing = true)
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails user = User.builder()
                .username(userName)
                .password(passwordEncoder().encode(passWord))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    @ConditionalOnProperty(name = "app.openmrs.login", havingValue = "true")
    public AuthenticationManager openmrsAuthenticationManager(
            SessionService sessionService) throws Exception {
        return new ProviderManager(new OpenmrsAuthenticationProvider(sessionService));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.sessionManagement(management -> management
                .maximumSessions(1)
                .expiredUrl("/login?expired"));

        http.authorizeRequests(requests -> requests
                .antMatchers("/webjars/**", "/login", "/images/**", "/*.ico").permitAll()
                .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(logout -> logout.logoutUrl("/logout"));

        return http.build();
    }
}
