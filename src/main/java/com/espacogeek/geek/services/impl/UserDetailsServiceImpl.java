package com.espacogeek.geek.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.espacogeek.geek.exception.GenericException;
import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.repositories.UserRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Loads user details from database for Spring Security.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserModel user = userRepository.findUserByEmail(email).orElseThrow(() -> new GenericException(HttpStatus.UNAUTHORIZED.toString()));

        // Build roles list from user.userRole (comma separated) and ensure ID_ claim is included
        List<String> rolesList = new ArrayList<>();
        String raw = user.getUserRole();
        if (raw != null && !raw.isBlank()) {
            String[] parts = raw.replaceAll("\\s", "").split(",");
            // Normalize roles: if a role doesn't start with ROLE_ or ID_, prefix with ROLE_
            rolesList.addAll(Arrays.stream(parts)
                .map(s -> s == null ? null : s.trim())
                .filter(s -> s != null && !s.isBlank())
                .map(s -> {
                    if (s.startsWith("ROLE_") || s.startsWith("ID_")) return s;
                    return "ROLE_" + s;
                })
                .toList());
        }
        // Ensure at least ROLE_user is present as a fallback
        if (rolesList.isEmpty()) {
            rolesList.add("ROLE_user");
        }
        // Add device/user identifier role (keep ID_ as-is)
        rolesList.add("ID_" + user.getId());

        return User.builder()
                .username(user.getEmail())
                .password(new String(user.getPassword()))
                .authorities(rolesList.toArray(new String[0]))
                .build();
    }
}
