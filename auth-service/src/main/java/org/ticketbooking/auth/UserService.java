package org.ticketbooking.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.ticketbooking.common.model.CommonException;
import org.ticketbooking.common.model.Role;
import org.ticketbooking.common.model.User;
import org.ticketbooking.common.repository.RoleRepository;
import org.ticketbooking.common.repository.UserRepository;

@Service
public class UserService{
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public User register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        Role userRole = roleRepository.findByName("CUSTOMER").orElseThrow(() -> new RuntimeException("Role not found"));
        user.setRole(userRole);
        return userRepository.save(user);
    }

    public String authenticate(String username, String password) throws CommonException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (passwordEncoder.matches(password, user.getPassword())) {
            return jwtUtil.generateToken(username);
        } else {
            throw new CommonException("Invalid credentials","400");
        }
    }

    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
