package com.finsphere.auth.service;

import com.finsphere.common.dto.auth.LoginResponse;

/**
 * Successful login: the token goes into the HttpOnly cookie, the response body to the client.
 *
 * @param token    signed JWT
 * @param response body for the client
 */
public record LoginResult(String token, LoginResponse response) {

    @Override
    public String toString() {
        return "LoginResult[token=****, response=" + this.response + "]";
    }
}
