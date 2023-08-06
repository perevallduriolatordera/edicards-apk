package net.ifeu.edicards.Services;

public class WSCredentials {

	private String User;
	private String Password;

	public WSCredentials(String user, String password) {
		this.User = user;
		this.Password = password;
	}

	public String getUser() {
		return User;
	}

	public String getPassword() {
		return Password;
	}
}
