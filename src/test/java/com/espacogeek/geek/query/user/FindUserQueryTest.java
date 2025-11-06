package com.espacogeek.geek.query.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

import com.espacogeek.geek.config.JwtConfig;
import com.espacogeek.geek.controllers.UserController;
import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.services.JwtTokenService;
import com.espacogeek.geek.services.UserService;

@GraphQlTest(UserController.class)
@ActiveProfiles("test")
class FindUserQueryTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtConfig jwtConfig;

    @MockBean
    private JwtTokenService jwtTokenService;

    @Test
    void findUserById_ShouldReturnUser() {
        // Given
        UserModel user = new UserModel();
        user.setId(1);
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        when(userService.findByIdOrUsernameContainsOrEmail(anyInt(), any(), any()))
                .thenReturn(Arrays.asList(user));

        // When & Then
        graphQlTester.document("""
                query {
                    findUser(id: 1) {
                        id
                        username
                        email
                    }
                }
                """)
                .execute()
                .path("findUser")
                .entityList(UserModel.class)
                .satisfies(users -> {
                    assertThat(users).hasSize(1);
                    assertThat(users.get(0).getId()).isEqualTo(1);
                    assertThat(users.get(0).getUsername()).isEqualTo("testuser");
                    assertThat(users.get(0).getEmail()).isEqualTo("test@example.com");
                });
    }

    @Test
    void findUserByUsername_ShouldReturnUser() {
        // Given
        UserModel user = new UserModel();
        user.setId(2);
        user.setUsername("johndoe");
        user.setEmail("john@example.com");

        when(userService.findByIdOrUsernameContainsOrEmail(any(), anyString(), any()))
                .thenReturn(Arrays.asList(user));

        // When & Then
        graphQlTester.document("""
                query {
                    findUser(username: "johndoe") {
                        id
                        username
                        email
                    }
                }
                """)
                .execute()
                .path("findUser")
                .entityList(UserModel.class)
                .satisfies(users -> {
                    assertThat(users).hasSize(1);
                    assertThat(users.get(0).getUsername()).isEqualTo("johndoe");
                });
    }

    @Test
    void findUserByEmail_ShouldReturnUser() {
        // Given
        UserModel user = new UserModel();
        user.setId(3);
        user.setUsername("jane");
        user.setEmail("jane@example.com");

        when(userService.findByIdOrUsernameContainsOrEmail(any(), any(), anyString()))
                .thenReturn(Arrays.asList(user));

        // When & Then
        graphQlTester.document("""
                query {
                    findUser(email: "jane@example.com") {
                        id
                        username
                        email
                    }
                }
                """)
                .execute()
                .path("findUser")
                .entityList(UserModel.class)
                .satisfies(users -> {
                    assertThat(users).hasSize(1);
                    assertThat(users.get(0).getEmail()).isEqualTo("jane@example.com");
                });
    }

    @Test
    void findUser_NoParameters_ShouldReturnEmptyList() {
        // Given
        when(userService.findByIdOrUsernameContainsOrEmail(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // When & Then
        graphQlTester.document("""
                query {
                    findUser {
                        id
                        username
                        email
                    }
                }
                """)
                .execute()
                .path("findUser")
                .entityList(UserModel.class)
                .hasSize(0);
    }
}
