package com.marin.mas_que_amigos.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserCreateRequestDTO {

    @NotBlank(message = "El usuario es obligatorio.")
    @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$",
            message = "El usuario debe tener entre 3 y 50 caracteres: letras, números, punto, guion y guion bajo.")
    private String username;

    // 72 is BCrypt's input limit: anything longer would be silently truncated.
    @NotBlank(message = "La contraseña es obligatoria.")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres.")
    @Size(max = 72, message = "La contraseña no puede superar 72 caracteres.")
    private String password;

    // Optional: ADMIN or DT (default DT). A String so an unknown value is answered
    // with our own Spanish 400 instead of a JSON deserialization error.
    private String role;

    // Optional, DT accounts only.
    private Long teamId;
}
