package com.github.phoswald.rstm.security;

public class PasswordUtility {

	public static void main(String[] args) {
		char[] password = System.console().readPassword("Enter password: ");
		System.out.println("bcrypt hash: " + JdbcIdentityProvider.hashPassword(password));
	}
}
