package com.taskManger.User.Service;

import com.taskManger.User.DTO.UserAuth;
import com.taskManger.User.DTO.UserDTO;
import com.taskManger.User.Model.UserEntity;
import com.taskManger.User.Repository.UserRepository;
import com.taskManger.User.security.JwtUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    private UserEntity user;
    private UserAuth userAuth;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(1);
        user.setEmail("test@example.com");
        user.setPassword("password123");
        user.setRole("admin");

        userAuth = new UserAuth();
        userAuth.setEmail("test@example.com");
        userAuth.setPassword("password123");
    }

    @Test
    void login_ReturnsToken_WhenCredentialsAreValid() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(1, "test@example.com", "admin")).thenReturn("mocked-token");

        String token = userService.Login(userAuth);

        assertThat(token).isEqualTo("mocked-token");
        verify(jwtUtil).generateToken(1, "test@example.com", "admin");
    }

    @Test
    void login_ThrowsException_WhenUserNotFound() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.Login(userAuth))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");
    }

    @Test
    void login_ThrowsException_WhenPasswordIsWrong() {
        userAuth.setPassword("wrongpassword");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.Login(userAuth))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid password");
    }

    @Test
    void updatePassword_UpdatesPassword_WhenUserExists() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        userService.updatePassword(1, "newpass");

        assertThat(user.getPassword()).isEqualTo("newpass");
        assertThat(user.getUpdateAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void updatePassword_ThrowsException_WhenUserNotFound() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updatePassword(1, "newpass"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found with id: 1");
    }

    @Test
    void viewUserByiId_ReturnsUserDTO_WhenUserExists() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        UserDTO dto = userService.viewUserByiId(1);

        assertThat(dto.getEmail()).isEqualTo("test@example.com");
        assertThat(dto.getRole()).isEqualTo("admin");
    }

    @Test
    void viewUserByiId_ThrowsException_WhenUserNotFound() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.viewUserByiId(1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found with id: 1");
    }
}
