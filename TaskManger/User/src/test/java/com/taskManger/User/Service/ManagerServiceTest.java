package com.taskManger.User.Service;

import com.taskManger.User.DTO.UserDTO;
import com.taskManger.User.DTO.UserInsertDTO;
import com.taskManger.User.Model.UserEntity;
import com.taskManger.User.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(locations = "classpath:application-test.yml")

public class ManagerServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ManagerService managerService;

    private UserInsertDTO userInsertDTO;
    private UserEntity userEntity;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        userInsertDTO = new UserInsertDTO();
        userInsertDTO.setName("Test User");
        userInsertDTO.setEmail("test@example.com");
        userInsertDTO.setPassword("password123");

        userEntity = new UserEntity();
        userEntity.setId(1);
        userEntity.setName("Test User");
        userEntity.setEmail("test@example.com");
        userEntity.setPassword("password123");
        userEntity.setCreatedAt(LocalDateTime.now());

        userDTO = new UserDTO();
        userDTO.setId(1);
        userDTO.setName("Test User");
        userDTO.setEmail("test@example.com");
    }

    @Test
    void addUser_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        managerService.addUser(userInsertDTO);
        verify(userRepository, times(1)).findByEmail(userInsertDTO.getEmail());
        verify(userRepository, times(1)).save(any(UserEntity.class));
    }

    @Test
    void addUser_ThrowsException_WhenUserAlreadyExists() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(userEntity));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            managerService.addUser(userInsertDTO);
        });
        assertEquals("User already exists", exception.getMessage());
        verify(userRepository, times(1)).findByEmail(userInsertDTO.getEmail());
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void viewAllUsers_ReturnsListOfUsers() {
        List<UserEntity> userEntities = Arrays.asList(userEntity);
        when(userRepository.findAll()).thenReturn(userEntities);
        List<UserDTO> result = managerService.viewAllUsers();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(userDTO.getId(), result.get(0).getId());
        assertEquals(userDTO.getName(), result.get(0).getName());
        assertEquals(userDTO.getEmail(), result.get(0).getEmail());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void viewAllUsers_ReturnsEmptyList_WhenNoUsersExist() {
        when(userRepository.findAll()).thenReturn(Arrays.asList());
        List<UserDTO> result = managerService.viewAllUsers();
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void deleteUser_Success() {
        Integer userId = 1;
        when(userRepository.existsById(userId)).thenReturn(true);
        managerService.deleteUser(userId);
        verify(userRepository, times(1)).existsById(userId);
        verify(userRepository, times(1)).deleteById(userId);
    }

    @Test
    void deleteUser_ThrowsException_WhenUserNotFound() {
        Integer userId = 999;
        when(userRepository.existsById(userId)).thenReturn(false);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            managerService.deleteUser(userId);
        });
        assertEquals("User not found with id: " + userId, exception.getMessage());
        verify(userRepository, times(1)).existsById(userId);
        verify(userRepository, never()).deleteById(anyInt());
    }
}
