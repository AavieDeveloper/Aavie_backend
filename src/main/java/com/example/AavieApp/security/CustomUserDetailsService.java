package com.example.AavieApp.security;

import com.example.AavieApp.model.UserProfile;
import com.example.AavieApp.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserProfile userProfile = userProfileRepository.findByEmail(email.toLowerCase())
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        return new User(
            userProfile.getEmail(),
            userProfile.getPasswordHash(),
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}