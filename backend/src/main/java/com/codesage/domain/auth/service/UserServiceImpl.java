package com.codesage.domain.auth.service;

import com.codesage.domain.auth.dto.response.UserResponseDto;
import com.codesage.domain.auth.mapper.UserMapper;
import com.codesage.domain.auth.model.User;
import com.codesage.domain.auth.repository.UserRepository;
import com.codesage.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of {@link UserService}.
 */
@Log4j2
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper     userMapper;

    @Override
    public UserResponseDto findById(UUID userId) {
        log.debug("Fetching user profile for id: {}", userId);
        User user = findEntityById(userId);
        return userMapper.toDto(user);
    }

    @Override
    public User findEntityById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
