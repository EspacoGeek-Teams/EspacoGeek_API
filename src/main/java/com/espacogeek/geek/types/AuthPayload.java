package com.espacogeek.geek.types;

import com.espacogeek.geek.models.UserModel;

/**
 * GraphQL response payload returned after a successful login or token refresh.
 * Contains only the short-lived access token; the refresh token is delivered
 * via an HttpOnly cookie named {@code refreshToken}.
 */
public record AuthPayload(String accessToken, UserModel user) {}
