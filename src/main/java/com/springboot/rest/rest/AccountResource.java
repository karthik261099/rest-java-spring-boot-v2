package com.springboot.rest.rest;

import com.springboot.rest.domain.dto.AdminUserDTO;
import com.springboot.rest.domain.dto.PasswordChangeDTO;
import com.springboot.rest.domain.dto.SampleEntityDTO;
import com.springboot.rest.domain.port.api.MailServicePort;
import com.springboot.rest.domain.port.api.UserServicePort;
import com.springboot.rest.infrastructure.entity.User;
import com.springboot.rest.mapper.UserMapper;
import com.springboot.rest.rest.errors.AccountResourceException;
import com.springboot.rest.rest.errors.EmailAlreadyUsedException;
import com.springboot.rest.rest.errors.LoginAlreadyUsedException;
import com.springboot.rest.rest.vm.KeyAndPasswordVM;
import com.springboot.rest.security.SecurityUtils;
import com.springboot.rest.usecase.mail.SendMail;
import com.springboot.rest.usecase.user.CreateUser;
import com.springboot.rest.usecase.user.DeleteUser;
import com.springboot.rest.usecase.user.ReadUser;
import com.springboot.rest.usecase.user.RegisterUser;
import com.springboot.rest.usecase.user.UpdateUser;
import com.springboot.rest.rest.errors.InvalidPasswordException;
import com.springboot.rest.rest.vm.ManagedUserVM;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Optional;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api")
public class AccountResource {

    private final Logger log = LoggerFactory.getLogger(AccountResource.class);

    ////######## Without Using Verb Service Layer ######### ////////
	/*
	 * private final UserServicePort userServicePort; private final MailServicePort
	 * mailServicePort; private final UserMapper userMapper;
	 * 
	 * public AccountResource(UserServicePort userServicePort, MailServicePort
	 * mailServicePort, UserMapper userMapper) { this.userServicePort =
	 * userServicePort; this.mailServicePort = mailServicePort; this.userMapper =
	 * userMapper; }
	 */
    
    //// ######## Using Verb Service Layer ######## ////////
    private final CreateUser createUser;
    private final RegisterUser registerUser;
    private final ReadUser readUser;
    private final UpdateUser updateUser;
    private final DeleteUser deleteUser;
    
    private final SendMail sendEmail;
    
    private final UserMapper userMapper;

    public AccountResource(CreateUser createUser, RegisterUser registerUser, ReadUser readUser, UpdateUser updateUser,
			DeleteUser deleteUser, SendMail sendEmail, UserMapper userMapper) {
		this.createUser = createUser;
		this.registerUser = registerUser;
		this.readUser = readUser;
		this.updateUser = updateUser;
		this.deleteUser = deleteUser;
		this.sendEmail = sendEmail;
		this.userMapper = userMapper;
	}

	/**
     * {@code POST  /register} : register the user.
     *
     * @param managedUserVM
     *            the managed user View Model.
     * @throws InvalidPasswordException
     *             {@code 400 (Bad Request)} if the password is incorrect.
     * @throws EmailAlreadyUsedException
     *             {@code 400 (Bad Request)} if the email is already used.
     * @throws LoginAlreadyUsedException
     *             {@code 400 (Bad Request)} if the login is already used.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerAccount(@Valid @RequestBody ManagedUserVM managedUserVM) {
        if (isPasswordLengthInvalid(managedUserVM.getPassword())) {
            throw new InvalidPasswordException();
        }
        User user = registerUser.registerUser(managedUserVM, managedUserVM.getPassword());
     
    }

    /**
     * {@code GET  /activate} : activate the registered user.
     *
     * @param key
     *            the activation key.
     * @throws RuntimeException
     *             {@code 500 (Internal Server Error)} if the user couldn't be
     *             activated.
     */
    @GetMapping("/activate")
    @Operation(summary = "/account", security = @SecurityRequirement(name = "bearerAuth"))
    public void activateAccount(@RequestParam(value = "key") String key) {
        Optional<User> user = registerUser.activateRegistration(key);
        if (!user.isPresent()) {
            throw new AccountResourceException("No user was found for this activation key");
        }
    }

    /**
     * {@code GET  /authenticate} : check if the user is authenticated, and
     * return its login.
     *
     * @param request
     *            the HTTP request.
     * @return the login if the user is authenticated.
     */
    @GetMapping("/authenticate")
    public String isAuthenticated(HttpServletRequest request) {
        log.debug("REST request to check if the current user is authenticated");
        return request.getRemoteUser();
    }

    /**
     * {@code GET  /account} : get the current user.
     *
     * @return the current user.
     * @throws RuntimeException
     *             {@code 500 (Internal Server Error)} if the user couldn't be
     *             returned.
     */
    @GetMapping("/account")
    @Operation(summary = "/account", security = @SecurityRequirement(name = "bearerAuth"))
    public AdminUserDTO getAccount() {
        return readUser.getUserWithAuthorities().map(AdminUserDTO::new).orElseThrow(() -> new AccountResourceException("User could not be found"));
    }

    /**
     * {@code POST  /account} : update the current user information.
     *
     * @param userDTO
     *            the current user information.
     * @throws EmailAlreadyUsedException
     *             {@code 400 (Bad Request)} if the email is already used.
     * @throws RuntimeException
     *             {@code 500 (Internal Server Error)} if the user login wasn't
     *             found.
     */
    @PostMapping("/account")
    @Operation(summary = "/account", security = @SecurityRequirement(name = "bearerAuth"))
    public void saveAccount(@Valid @RequestBody AdminUserDTO userDTO) {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new AccountResourceException("Current user login not found"));       
        createUser.saveAccount(userDTO, userLogin);
    }

    /**
     * {@code POST  /account/change-password} : changes the current user's
     * password.
     *
     * @param passwordChangeDto
     *            current and new password.
     * @throws InvalidPasswordException
     *             {@code 400 (Bad Request)} if the new password is incorrect.
     */
    @PostMapping(path = "/account/change-password")
    @Operation(summary = "/account/change-password", security = @SecurityRequirement(name = "bearerAuth"))
    public void changePassword(@RequestBody PasswordChangeDTO passwordChangeDto) {
        if (isPasswordLengthInvalid(passwordChangeDto.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        updateUser.changePassword(passwordChangeDto.getCurrentPassword(), passwordChangeDto.getNewPassword());
    }

    /**
     * {@code POST   /account/reset-password/init} : Send an email to reset the
     * password of the user.
     *
     * @param mail
     *            the mail of the user.
     */
    @PostMapping(path = "/account/reset-password/init")
    @Operation(summary = "/account", security = @SecurityRequirement(name = "bearerAuth"))
    public void requestPasswordReset(@RequestBody String mail) {
        Optional<User> user = updateUser.requestPasswordReset(mail);
        if (user.isPresent()) {
        	  log.warn("Password reset requested for existing mail");
        } else {
            log.warn("Password reset requested for non existing mail");
        }
    }

    /**
     * {@code POST   /account/reset-password/finish} : Finish to reset the
     * password of the user.
     *
     * @param keyAndPassword
     *            the generated key and the new password.
     * @throws InvalidPasswordException
     *             {@code 400 (Bad Request)} if the password is incorrect.
     * @throws RuntimeException
     *             {@code 500 (Internal Server Error)} if the password could not
     *             be reset.
     */
    @PostMapping(path = "/account/reset-password/finish")
    @Operation(summary = "/account", security = @SecurityRequirement(name = "bearerAuth"))
    public void finishPasswordReset(@RequestBody KeyAndPasswordVM keyAndPassword) {
        if (isPasswordLengthInvalid(keyAndPassword.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        Optional<User> user = updateUser.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey());

        if (!user.isPresent()) {
            throw new AccountResourceException("No user was found for this reset key");
        }
    }

    private static boolean isPasswordLengthInvalid(String password) {
        return (StringUtils.isEmpty(password) || password.length() < ManagedUserVM.PASSWORD_MIN_LENGTH || password.length() > ManagedUserVM.PASSWORD_MAX_LENGTH);
    }
}
