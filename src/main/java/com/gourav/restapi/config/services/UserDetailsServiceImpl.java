package com.gourav.restapi.config.services;

import com.gourav.restapi.models.User;
import com.gourav.restapi.repositories.UserRepository;
import com.gourav.restapi.security.UserDetails;
import com.gourav.restapi.security.UserDetailsService;
import com.gourav.restapi.security.UsernameNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Inject
    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        return UserDetailsImpl.build(user);
    }
}
