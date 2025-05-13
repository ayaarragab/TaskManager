package com.taskManger.User.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInsertDTO {
    private String name;
    private String email;
    private String role;
    private String password;
}
