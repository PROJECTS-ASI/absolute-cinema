package com.absolutecinema.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {
	private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(normalize(email));
    }

    public User register(RegisterForm form) {
        String email = normalize(form.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo");
        }
        User user = new User();
        user.setName(form.getName().trim());
        user.setLastName(form.getLastName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        return userRepository.save(user);
    }

    // Spring Security llama a este método al iniciar sesión
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(normalize(email))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles("CLIENTE")
                .build();
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
