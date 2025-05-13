package com.tasks.tasks.Service;

import com.tasks.tasks.Models.Entity.Task;
import com.tasks.tasks.Models.TaskStatus;
import com.tasks.tasks.Models.dto.TaskDto;
import com.tasks.tasks.Repositeries.TaskRepository;
import com.tasks.tasks.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.tasks.tasks.Services.TaskService;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(locations = "classpath:application-test.yml")
public class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private TaskService taskService;

    private TaskDto taskDto;
    private Task Task;
    private String validToken;
    private final Long userId = 1L;
    private final String managerRole = "manager";
    private final String employeeRole = "employee";

    @BeforeEach
    void setUp() {
        taskDto = new TaskDto();
        taskDto.setId(1L);
        taskDto.setTitle("Test Task");
        taskDto.setDescription("Test Description");
        taskDto.setStatus(TaskStatus.IN_PROGRESS);
        taskDto.setAssignedTo(userId);
        taskDto.setDeadline(LocalDateTime.now().plusDays(7));

        Task = new Task();
        Task.setId(1L);
        Task.setTitle("Test Task");
        Task.setDescription("Test Description");
        Task.setStatus(TaskStatus.IN_PROGRESS);
        Task.setAssignedTo(userId);
        Task.setDeadline(LocalDateTime.now().plusDays(7));
        Task.setCreatedAt(LocalDateTime.now());

        validToken = "Bearer valid_token";

        lenient().when(jwtUtil.extractUserId("valid_token")).thenReturn(userId.intValue());
        lenient().when(jwtUtil.extractUserRole("valid_token")).thenReturn(managerRole);
    }

    @Test
    void getUserId_ExtractsUserIdFromToken() {
        Long result = taskService.getUserId(validToken);
        assertEquals(userId, result);
        verify(jwtUtil, times(1)).extractUserId("valid_token");
    }

    @Test
    void getRoleFromToken_ExtractsRoleFromToken() {
        String result = taskService.getRoleFromToken(validToken);
        assertEquals(managerRole, result);
        verify(jwtUtil, times(1)).extractUserRole("valid_token");
    }

    @Test
    void getAllTasks_ReturnsListOfTasks() {
        List<Task> taskEntities = Arrays.asList(Task);
        when(taskRepository.findAll()).thenReturn(taskEntities);
        List<TaskDto> result = taskService.getAllTasks();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(taskDto.getId(), result.get(0).getId());
        assertEquals(taskDto.getTitle(), result.get(0).getTitle());
        verify(taskRepository, times(1)).findAll();
    }

    @Test
    void getAllTasks_ThrowsException_WhenRepositoryFails() {
        when(taskRepository.findAll()).thenThrow(new RuntimeException("Database error"));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            taskService.getAllTasks();
        });
        assertTrue(exception.getMessage().contains("Failed to retrieve tasks"));
        verify(taskRepository, times(1)).findAll();
    }

    @Test
    void createTask_Success() {
        when(taskRepository.save(any(Task.class))).thenReturn(Task);
        TaskDto result = taskService.createTask(taskDto);
        assertNotNull(result);
        assertEquals(taskDto.getTitle(), result.getTitle());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void createTask_ThrowsException_WhenTaskDtoIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            taskService.createTask(null);
        });
        assertEquals("Task data cannot be null", exception.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void createTask_ThrowsException_WhenTitleIsBlank() {
        taskDto.setTitle("");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            taskService.createTask(taskDto);
        });
        assertEquals("Task title is required", exception.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void createTask_ThrowsException_WhenRepositorySaveFails() {
        when(taskRepository.save(any(Task.class))).thenThrow(new RuntimeException("Database error"));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            taskService.createTask(taskDto);
        });
        assertTrue(exception.getMessage().contains("Failed to create task"));
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void deleteTask_Success() {
        Long taskId = 1L;
        when(taskRepository.existsById(taskId)).thenReturn(true);
        taskService.deleteTask(taskId);
        verify(taskRepository, times(1)).existsById(taskId);
        verify(taskRepository, times(1)).deleteById(taskId);
    }

    @Test
    void deleteTask_ThrowsException_WhenTaskNotFound() {
        Long taskId = 999L;
        when(taskRepository.existsById(taskId)).thenReturn(false);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            taskService.deleteTask(taskId);
        });
        assertEquals("Task not found with id: " + taskId, exception.getMessage());
        verify(taskRepository, times(1)).existsById(taskId);
        verify(taskRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteTask_ThrowsException_WhenRepositoryDeleteFails() {
        Long taskId = 1L;
        when(taskRepository.existsById(taskId)).thenReturn(true);
        doThrow(new RuntimeException("Database error")).when(taskRepository).deleteById(taskId);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            taskService.deleteTask(taskId);
        });
        assertTrue(exception.getMessage().contains("Failed to delete task"));
        verify(taskRepository, times(1)).existsById(taskId);
        verify(taskRepository, times(1)).deleteById(taskId);
    }

    @Test
    void getTaskById_AsManager_ReturnsTask() {
        Long taskId = 1L;
        when(jwtUtil.extractUserRole("valid_token")).thenReturn(managerRole);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(Task));
        Optional<TaskDto> result = taskService.getTaskById(taskId, validToken);
        assertTrue(result.isPresent());
        assertEquals(taskDto.getId(), result.get().getId());
        verify(taskRepository, times(1)).findById(taskId);
        verify(taskRepository, never()).findByIdAndAssignedTo(anyLong(), anyLong());
    }

    @Test
    void getTaskById_AsEmployee_ReturnsOwnTask() {
        Long taskId = 1L;
        when(jwtUtil.extractUserRole("valid_token")).thenReturn(employeeRole);
        when(taskRepository.findByIdAndAssignedTo(taskId, userId)).thenReturn(Optional.of(Task));
        Optional<TaskDto> result = taskService.getTaskById(taskId, validToken);
        assertTrue(result.isPresent());
        assertEquals(taskDto.getId(), result.get().getId());
        verify(taskRepository, never()).findById(anyLong());
        verify(taskRepository, times(1)).findByIdAndAssignedTo(taskId, userId);
    }

    @Test
    void getTaskById_AsEmployee_ReturnsEmpty_WhenTaskNotAssigned() {
        Long taskId = 1L;
        when(jwtUtil.extractUserRole("valid_token")).thenReturn(employeeRole);
        when(taskRepository.findByIdAndAssignedTo(taskId, userId)).thenReturn(Optional.empty());
        Optional<TaskDto> result = taskService.getTaskById(taskId, validToken);
        assertFalse(result.isPresent());
        verify(taskRepository, never()).findById(anyLong());
        verify(taskRepository, times(1)).findByIdAndAssignedTo(taskId, userId);
    }

    @Test
    void getUserTasks_ReturnsUserTasks() {
        List<Task> taskEntities = Arrays.asList(Task);
        when(taskRepository.findByAssignedTo(userId)).thenReturn(taskEntities);
        List<TaskDto> result = taskService.getUserTasks(validToken);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(taskDto.getId(), result.get(0).getId());
        verify(taskRepository, times(1)).findByAssignedTo(userId);
    }

    @Test
    void getUserTasks_ReturnsEmptyList_WhenNoTasksExist() {
        when(taskRepository.findByAssignedTo(userId)).thenReturn(Collections.emptyList());
        List<TaskDto> result = taskService.getUserTasks(validToken);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(taskRepository, times(1)).findByAssignedTo(userId);
    }

    @Test
    void getTasksByEmployeeAndStatus_ReturnsFilteredTasks() {
        Long employeeId = 2L;
        List<Task> taskEntities = Arrays.asList(Task);
        when(taskRepository.findByAssignedToAndStatus(employeeId, TaskStatus.IN_PROGRESS)).thenReturn(taskEntities);
        List<TaskDto> result = taskService.getTasksByEmployeeAndStatus(employeeId, TaskStatus.IN_PROGRESS, userId);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(taskDto.getId(), result.get(0).getId());
        verify(taskRepository, times(1)).findByAssignedToAndStatus(employeeId, TaskStatus.IN_PROGRESS);
    }

    @Test
    void getTasksByEmployeeAndStatus_ThrowsException_WhenRepositoryFails() {
        Long employeeId = 2L;
        when(taskRepository.findByAssignedToAndStatus(employeeId, TaskStatus.IN_PROGRESS))
                .thenThrow(new RuntimeException("Database error"));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            taskService.getTasksByEmployeeAndStatus(employeeId, TaskStatus.IN_PROGRESS, userId);
        });
        assertTrue(exception.getMessage().contains("Failed to retrieve tasks for employee"));
        verify(taskRepository, times(1)).findByAssignedToAndStatus(employeeId, TaskStatus.IN_PROGRESS);
    }

    @Test
    void getTasksByEmployeeId_ReturnsEmployeeTasks() {
        Long employeeId = 2L;
        List<Task> taskEntities = Arrays.asList(Task);
        when(taskRepository.findByAssignedTo(employeeId)).thenReturn(taskEntities);
        List<TaskDto> result = taskService.getTasksByEmployeeId(employeeId, userId);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(taskDto.getId(), result.get(0).getId());
        verify(taskRepository, times(1)).findByAssignedTo(employeeId);
    }

    @Test
    void getTasksByEmployeeId_ThrowsException_WhenRepositoryFails() {
        Long employeeId = 2L;
        when(taskRepository.findByAssignedTo(employeeId)).thenThrow(new RuntimeException("Database error"));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            taskService.getTasksByEmployeeId(employeeId, userId);
        });
        assertTrue(exception.getMessage().contains("Failed to retrieve tasks for employee"));
        verify(taskRepository, times(1)).findByAssignedTo(employeeId);
    }

    @Test
    void getTasksByStatus_ReturnsTasksWithStatus() {
        List<Task> taskEntities = Arrays.asList(Task);
        when(taskRepository.findByStatus(TaskStatus.IN_PROGRESS)).thenReturn(taskEntities);
        List<TaskDto> result = taskService.getTasksByStatus(TaskStatus.IN_PROGRESS, userId);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(taskDto.getId(), result.get(0).getId());
        verify(taskRepository, times(1)).findByStatus(TaskStatus.IN_PROGRESS);
    }

    @Test
    void getTasksByStatus_ThrowsException_WhenRepositoryFails() {
        when(taskRepository.findByStatus(TaskStatus.IN_PROGRESS)).thenThrow(new RuntimeException("Database error"));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            taskService.getTasksByStatus(TaskStatus.IN_PROGRESS, userId);
        });
        assertTrue(exception.getMessage().contains("Failed to retrieve tasks with status"));
        verify(taskRepository, times(1)).findByStatus(TaskStatus.IN_PROGRESS);
    }

    @Test
    void updateTask_Success() {
        Long taskId = 1L;
        TaskDto updatedTaskDto = new TaskDto();
        updatedTaskDto.setTitle("Updated Title");
        updatedTaskDto.setDescription("Updated Description");
        updatedTaskDto.setStatus(TaskStatus.IN_PROGRESS);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(Task));
        when(taskRepository.save(any(Task.class))).thenReturn(Task);
        Optional<TaskDto> result = taskService.updateTask(taskId, updatedTaskDto, userId);
        assertTrue(result.isPresent());
        assertEquals(updatedTaskDto.getTitle(), Task.getTitle());
        assertEquals(updatedTaskDto.getDescription(), Task.getDescription());
        assertEquals(updatedTaskDto.getStatus(), Task.getStatus());
        verify(taskRepository, times(1)).findById(taskId);
        verify(taskRepository, times(1)).save(Task);
    }

    @Test
    void updateTask_ThrowsException_WhenTaskDtoIsNull() {
        Long taskId = 1L;
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            taskService.updateTask(taskId, null, userId);
        });
        assertEquals("Updated task data cannot be null", exception.getMessage());
        verify(taskRepository, never()).findById(anyLong());
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void updateTask_ReturnsEmpty_WhenTaskNotFound() {
        Long taskId = 999L;
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());
        Optional<TaskDto> result = taskService.updateTask(taskId, taskDto, userId);
        assertFalse(result.isPresent());
        verify(taskRepository, times(1)).findById(taskId);
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void updateTask_ThrowsException_WhenRepositoryUpdateFails() {
        Long taskId = 1L;
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(Task));
        when(taskRepository.save(any(Task.class))).thenThrow(new RuntimeException("Database error"));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            taskService.updateTask(taskId, taskDto, userId);
        });
        assertTrue(exception.getMessage().contains("Failed to update task"));
        verify(taskRepository, times(1)).findById(taskId);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void updateTask_PartialUpdate_Success() {
        Long taskId = 1L;
        TaskDto partialUpdateDto = new TaskDto();
        partialUpdateDto.setTitle("Updated Title");
        Task originalEntity = new Task();
        originalEntity.setId(taskId);
        originalEntity.setTitle("Original Title");
        originalEntity.setDescription("Original Description");
        originalEntity.setStatus(TaskStatus.IN_PROGRESS);
        originalEntity.setAssignedTo(userId);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(originalEntity));
        when(taskRepository.save(any(Task.class))).thenReturn(originalEntity);
        Optional<TaskDto> result = taskService.updateTask(taskId, partialUpdateDto, userId);
        assertTrue(result.isPresent());
        assertEquals("Updated Title", originalEntity.getTitle());
        assertEquals("Original Description", originalEntity.getDescription());
        assertEquals(TaskStatus.IN_PROGRESS, originalEntity.getStatus());
        verify(taskRepository, times(1)).findById(taskId);
        verify(taskRepository, times(1)).save(originalEntity);
    }
}
