package com.oracle.demo.timg.iot.resthandler.controllers;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.AuthenticationFailureReason;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.provider.HttpRequestAuthenticationProvider;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Log
// create with the context so we can confirm the credentials at startup
@Context
public class AuthenticationProviderUserPassword<B> implements HttpRequestAuthenticationProvider<B> {
	@Property(name = RESTServerProperties.USERNAME, defaultValue = "username")
	private String username;
	@Property(name = RESTServerProperties.PASSWORD, defaultValue = "password")
	private String password;

	@Override
	public AuthenticationResponse authenticate(@Nullable HttpRequest<B> httpRequest,
			@NonNull AuthenticationRequest<String, String> authenticationRequest) {
		String incommingUsername = authenticationRequest.getIdentity();
		String incommingPassword = authenticationRequest.getSecret();
		log.info("Checking incomming credentials of " + incommingUsername + "/" + incommingPassword
				+ " against required credentials of " + username + "/" + password);
		return incommingUsername.equals(username) && incommingPassword.equals(password)
				? AuthenticationResponse.success(authenticationRequest.getIdentity())
				: AuthenticationResponse.failure(AuthenticationFailureReason.CREDENTIALS_DO_NOT_MATCH);
	}

	@PostConstruct
	public void postConstruct() {
		log.info("Will look for credentials=" + username + "/" + password);
	}
}