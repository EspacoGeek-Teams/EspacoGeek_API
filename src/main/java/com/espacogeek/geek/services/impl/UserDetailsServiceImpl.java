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

        return User.builder()
                .username(user.getEmail())
                .password(new String(user.getPassword()))
                .authorities("ID_"+user.getId(), "ROLE_user")
                .build();
    }
}
