package com.bazunia.vps.config;

import com.bazunia.vps.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.util.unit.DataSize;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.filter.ForwardedHeaderFilter;
// --- ⭐️ DODAJ IMPORTY DLA 2FA ⭐️ ---
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserRepository userRepository;

    // Ten Bean uczy Springa, jak znajdować usera po emailu
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    // Ten Bean definiuje, jakiego hashera używamy (BCrypt)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Ten Bean jest potrzebny do poprawnego działania AuthService
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder()); // Używa BCryptPasswordEncoder
        return authProvider;
    }

    // --- ⭐️ DODANY BEAN DLA OBSŁUGI NAGŁÓWKÓW PROXY (ROZWIĄZANIE PROBLEMU Z 127.0.0.1) ⭐️ ---
    @Bean
    public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
        // Ten filter nakazuje Springowi, aby odczytywał adres IP klienta
        // z nagłówków X-Forwarded-For i X-Real-IP, zamiast z adresu proxy (127.0.0.1).
        FilterRegistrationBean<ForwardedHeaderFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new ForwardedHeaderFilter());
        return bean;
    }

    // --- ⭐️ DODANE BEANY DLA 2FA, KTÓRYCH BRAKOWAŁO ⭐️ ---

    /**
     * Bean, który tworzy `SecretGenerator` (do tworzenia sekretów 2FA)
     */
    @Bean
    public SecretGenerator secretGenerator() {
        return new DefaultSecretGenerator();
    }

    /**
     * Bean, który tworzy `CodeVerifier` (do sprawdzania kodów 2FA)
     */
    @Bean
    public CodeVerifier codeVerifier() {
        TimeProvider timeProvider = new SystemTimeProvider();
        DefaultCodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1);
        return new DefaultCodeVerifier(codeGenerator, timeProvider);
    }
    // ⭐️ DODANY BEAN DLA NAPRAWY BŁĘDU 413 (Maksymalny Rozmiar Pliku) ⭐️
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        // Ustawienie limitu na 100 MB (w bajtach)
        long size = 100 * 1024 * 1024;

        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofBytes(size));
        factory.setMaxRequestSize(DataSize.ofBytes(size));

        return factory.createMultipartConfig();
    }
}
