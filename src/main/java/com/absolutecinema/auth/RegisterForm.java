package com.absolutecinema.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class RegisterForm {
	@NotBlank(message = "Ingresa tu nombre")
    private String name;

    @NotBlank(message = "Ingresa tus apellidos")
    private String lastName;

    @NotBlank(message = "Ingresa tu correo")
    @Email(message = "El correo no es válido")
    private String email;

    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
             message = "Mínimo 8 caracteres, una mayúscula y un número")
    private String password;

    @NotBlank(message = "Confirma tu contraseña")
    private String confirmPassword;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getConfirmPassword() {
		return confirmPassword;
	}

	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}
}