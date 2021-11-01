package com.springboot.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springboot.rest.domain.dto.AdminUserDTO;
import com.springboot.rest.domain.dto.UserDTO;
import com.springboot.rest.infrastructure.entity.User;

/**
 * Unit tests for {@link UserMapper}.
 */
class UserMapperTest {

    private static final String DEFAULT_LOGIN = "johndoe";
    private static final Long DEFAULT_ID = 1L;

    private UserMapper userMapper;
    private User user;
    private AdminUserDTO userDto;

    @BeforeEach
    public void init() {
        userMapper = new UserMapper();
        user = new User();
        user.setLogin(DEFAULT_LOGIN);
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        user.setEmail("johndoe@localhost");
        user.setFirstName("john");
        user.setLastName("doe");
        user.setImageUrl("image_url");
        user.setLangKey("en");

        userDto = new AdminUserDTO(user);
    }

    @Test
    void usersToUserDTOsShouldMapOnlyNonNullUsers() {
        List<User> users = new ArrayList<>();
        users.add(user);
        users.add(null);

        List<UserDTO> userDTOS = userMapper.entitiesToDtos(users);
//        List<UserDTO> userDTOS = userMapper.usersToUserDTOs(users);

        assertThat(userDTOS).isNotEmpty().size().isEqualTo(1);
    }

    @Test
    void userDTOsToUsersShouldMapOnlyNonNullUsers() {
        List<AdminUserDTO> usersDto = new ArrayList<>();
        usersDto.add(userDto);
        usersDto.add(null);

        List<User> users = userMapper.adminUserDtosToUserEntities(usersDto);
//        List<User> users = userMapper.userDTOsToUsers(usersDto);

        assertThat(users).isNotEmpty().size().isEqualTo(1);
    }

    @Test
    void userDTOsToUsersWithAuthoritiesStringShouldMapToUsersWithAuthoritiesDomain() {
        Set<String> authoritiesAsString = new HashSet<>();
        authoritiesAsString.add("ADMIN");
        userDto.setAuthorities(authoritiesAsString);

        List<AdminUserDTO> usersDto = new ArrayList<>();
        usersDto.add(userDto);

        List<User> users = userMapper.adminUserDtosToUserEntities(usersDto);
//        List<User> users = userMapper.userDTOsToUsers(usersDto);

        assertThat(users).isNotEmpty().size().isEqualTo(1);
        assertThat(users.get(0).getAuthorities()).isNotNull();
        assertThat(users.get(0).getAuthorities()).isNotEmpty();
        assertThat(users.get(0).getAuthorities().iterator().next().getName()).isEqualTo("ADMIN");
    }

    @Test
    void userDTOsToUsersMapWithNullAuthoritiesStringShouldReturnUserWithEmptyAuthorities() {
        userDto.setAuthorities(null);

        List<AdminUserDTO> usersDto = new ArrayList<>();
        usersDto.add(userDto);

        List<User> users = userMapper.adminUserDtosToUserEntities(usersDto);
//        List<User> users = userMapper.userDTOsToUsers(usersDto);

        assertThat(users).isNotEmpty().size().isEqualTo(1);
        assertThat(users.get(0).getAuthorities()).isNotNull();
        assertThat(users.get(0).getAuthorities()).isEmpty();
    }

    @Test
    void userDTOToUserMapWithAuthoritiesStringShouldReturnUserWithAuthorities() {
        Set<String> authoritiesAsString = new HashSet<>();
        authoritiesAsString.add("ADMIN");
        userDto.setAuthorities(authoritiesAsString);

        User user1 = userMapper.adminUserDtoToUserEntity(userDto);
//        User user = userMapper.userDTOToUser(userDto);
        
        // test with ModelMapper
        assertThat(user1).isNotNull();
        assertThat(user1.getAuthorities()).isNotNull();
        assertThat(user1.getAuthorities()).isNotEmpty();
        assertThat(user1.getAuthorities().iterator().next().getName()).isEqualTo("ADMIN");
    }

    @Test
    void userDTOToUserMapWithNullAuthoritiesStringShouldReturnUserWithEmptyAuthorities() {
        userDto.setAuthorities(null);

        User user = userMapper.adminUserDtoToUserEntity(userDto);
//        User user = userMapper.userDTOToUser(userDto);
        
        assertThat(user).isNotNull();
        assertThat(user.getAuthorities()).isNotNull();
        assertThat(user.getAuthorities()).isEmpty();
    }

    @Test
    void userDTOToUserMapWithNullUserShouldReturnNull() {
    	assertThat(userMapper.adminUserDtoToUserEntity(null)).isNull();
//    	assertThat(userMapper.userDTOToUser(null)).isNull();
    }

    @Test
    void testUserFromId() {
        assertThat(userMapper.userFromId(DEFAULT_ID).getId()).isEqualTo(DEFAULT_ID);
        assertThat(userMapper.userFromId(null)).isNull();
    }
}
