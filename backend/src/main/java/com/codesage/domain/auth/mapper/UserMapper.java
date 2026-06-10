package com.codesage.domain.auth.mapper;

import com.codesage.domain.auth.dto.response.UserResponseDto;
import com.codesage.domain.auth.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between {@link User} entity and {@link UserResponseDto}.
 *
 * <p>Uses Spring component model so it can be injected via {@code @Autowired}
 * or constructor injection.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Maps a {@link User} entity to a {@link UserResponseDto}.
     * Exposes only public profile fields — never the encrypted GitHub token.
     */
    @Mapping(target = "id",          source = "id")
    @Mapping(target = "login",       source = "login")
    @Mapping(target = "name",        source = "name")
    @Mapping(target = "email",       source = "email")
    @Mapping(target = "avatarUrl",   source = "avatarUrl")
    @Mapping(target = "role",        source = "role")
    @Mapping(target = "lastLoginAt", source = "lastLoginAt")
    @Mapping(target = "createdAt",   source = "createdAt")
    UserResponseDto toDto(User user);
}
