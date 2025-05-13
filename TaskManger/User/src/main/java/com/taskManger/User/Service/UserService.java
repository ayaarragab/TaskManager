package com.taskManger.User.Service;

import com.taskManger.User.DTO.UserAuth;
import com.taskManger.User.DTO.UserDTO;
import com.taskManger.User.Mapper.UserMapper;
import com.taskManger.User.Model.UserEntity;
import com.taskManger.User.Repository.UserRepository;
import com.taskManger.User.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    public String Login(UserAuth userAuth)
    {
        UserEntity user = userRepository.findByEmail(userAuth.getEmail())
                .orElseThrow(() -> new IllegalArgumentException ("User not found"));
        if (!user.getPassword().equals(userAuth.getPassword())) {
            throw new IllegalArgumentException ("Invalid password");
        }

        return jwtUtil.generateToken(user.getId(),user.getEmail(), user.getRole());
    }

    public void updatePassword(Integer id,String newPassword)
    {
        Optional<UserEntity> optionalUser = userRepository.findById(id);
        if(!optionalUser.isPresent())
        {
            throw new IllegalArgumentException("User not found with id: " + id);
        }
        UserEntity user = optionalUser.get();
        user.setUpdateAt(LocalDateTime.now());
        user.setPassword(newPassword);
        userRepository.save(user);
    }

    public UserDTO viewUserByiId(Integer id)
    {
        return userRepository.findById(id)
                .map(UserMapper::toDto)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
    }
}
