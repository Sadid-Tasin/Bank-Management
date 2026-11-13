package com.bank.bankmanagement.auth.service;

import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.user.repository.BankUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class BankUserDetailsService implements UserDetailsService {

    private final BankUserRepository userRepository;

    public BankUserDetailsService(BankUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        BankUser bankUser = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Invalid email or password"));

        return User.withUsername(bankUser.getEmail())
                .password(bankUser.getPasswordHash())
                .roles(bankUser.getRole().name())
                .disabled(!bankUser.isEnabled())
                .build();
    }
}
