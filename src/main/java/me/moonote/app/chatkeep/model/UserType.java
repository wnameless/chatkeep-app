package me.moonote.app.chatkeep.model;

/**
 * Represents the type of user in the system.
 *
 * ANONYMOUS: Unregistered user identified by browser-generated UUID AUTHENTICATED: Registered user
 * authenticated via OAuth2 provider
 */
public enum UserType {
  ANONYMOUS, AUTHENTICATED
}
