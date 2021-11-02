package com.springboot.rest.usecase.user;

import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.springboot.rest.domain.dto.AdminUserDTO;
import com.springboot.rest.domain.port.api.UserServicePort;
import com.springboot.rest.domain.service.UserService;
import com.springboot.rest.infrastructure.entity.User;
import com.springboot.rest.mapper.UserMapper;

public class CreateUserTest {
	
	private static final String DEFAULT_LOGIN = "johndoe";
	
    private UserMapper userMapper;
    private User user;
    private AdminUserDTO userDto;
    private CreateUser createUser;
    
    private UserService userService;
    
    
//    public CreateUserTest(UserServicePort userService) {
//		this.userService = userService;
//	}

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
        createUser = new CreateUser(userService);
    }
    
    @Test
    void saveAdminUserDTOasUser() {
    	User createdUser = createUser.createUser(userDto);
    	assertTrue((User.class.isInstance(createdUser)));
    }
    
    @Test
    void saveUserAccountWithGivenLoginID() {
    	createUser.saveAccount(userDto, DEFAULT_LOGIN);
    }
    
}
