package com.espacogeek.geek.mutation.userstatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.espacogeek.geek.controllers.UserStatusController;
import com.espacogeek.geek.exception.NotFoundException;
import com.espacogeek.geek.models.UserCustomStatusModel;
import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.services.UserCustomStatusService;

@GraphQlTest(UserStatusController.class)
@ActiveProfiles("test")
class UserStatusMutationTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private UserCustomStatusService userCustomStatusService;

    private UserCustomStatusModel stubStatus(Integer id, String name) {
        UserModel user = new UserModel();
        user.setId(1);
        user.setUsername("testuser");
        UserCustomStatusModel status = new UserCustomStatusModel();
        status.setId(id);
        status.setName(name);
        status.setUser(user);
        return status;
    }

    // ---- createUserStatus tests ----

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void createUserStatus_Success_ShouldReturnNewStatus() {
        UserCustomStatusModel created = stubStatus(1, "Re-watching");
        when(userCustomStatusService.create(eq(1), eq("Re-watching"))).thenReturn(created);

        graphQlTester.document("""
                mutation {
                    createUserStatus(name: "Re-watching") {
                        id
                        name
                    }
                }
                """)
                .execute()
                .errors().verify()
                .path("createUserStatus.name").entity(String.class).isEqualTo("Re-watching")
                .path("createUserStatus.id").hasValue();

        verify(userCustomStatusService).create(1, "Re-watching");
    }

    @Test
    void createUserStatus_Unauthenticated_ShouldReturnError() {
        graphQlTester.document("""
                mutation {
                    createUserStatus(name: "Re-watching") {
                        id
                        name
                    }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors).isNotEmpty());
    }

    // ---- updateUserStatus tests ----

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void updateUserStatus_Success_ShouldReturnUpdatedStatus() {
        UserCustomStatusModel updated = stubStatus(1, "Collecting");
        when(userCustomStatusService.update(eq(1), eq(1), eq("Collecting"))).thenReturn(updated);

        graphQlTester.document("""
                mutation {
                    updateUserStatus(id: 1, name: "Collecting") {
                        id
                        name
                    }
                }
                """)
                .execute()
                .errors().verify()
                .path("updateUserStatus.name").entity(String.class).isEqualTo("Collecting");

        verify(userCustomStatusService).update(1, 1, "Collecting");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void updateUserStatus_NotFound_ShouldReturnError() {
        when(userCustomStatusService.update(anyInt(), anyInt(), anyString()))
                .thenThrow(new NotFoundException("Status not found"));

        graphQlTester.document("""
                mutation {
                    updateUserStatus(id: 999, name: "Collecting") {
                        id
                        name
                    }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).isNotEmpty();
                    assertThat(errors.get(0).getExtensions().get("errorCode")).isEqualTo(3404);
                });
    }

    @Test
    void updateUserStatus_Unauthenticated_ShouldReturnError() {
        graphQlTester.document("""
                mutation {
                    updateUserStatus(id: 1, name: "Collecting") {
                        id
                    }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors).isNotEmpty());
    }

    // ---- deleteUserStatus tests ----

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void deleteUserStatus_Existing_ShouldReturnTrue() {
        when(userCustomStatusService.delete(eq(1), eq(1))).thenReturn(true);

        graphQlTester.document("""
                mutation {
                    deleteUserStatus(id: 1)
                }
                """)
                .execute()
                .errors().verify()
                .path("deleteUserStatus").entity(Boolean.class).isEqualTo(true);

        verify(userCustomStatusService).delete(1, 1);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void deleteUserStatus_NotFound_ShouldReturnFalse() {
        when(userCustomStatusService.delete(anyInt(), anyInt())).thenReturn(false);

        graphQlTester.document("""
                mutation {
                    deleteUserStatus(id: 999)
                }
                """)
                .execute()
                .errors().verify()
                .path("deleteUserStatus").entity(Boolean.class).isEqualTo(false);
    }

    @Test
    void deleteUserStatus_Unauthenticated_ShouldReturnError() {
        graphQlTester.document("""
                mutation {
                    deleteUserStatus(id: 1)
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors).isNotEmpty());
    }
}
