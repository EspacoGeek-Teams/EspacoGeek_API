package com.espacogeek.geek.query.userstatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.espacogeek.geek.controllers.UserStatusController;
import com.espacogeek.geek.models.UserCustomStatusModel;
import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.services.UserCustomStatusService;

@GraphQlTest(UserStatusController.class)
@ActiveProfiles("test")
class UserStatusQueryTest {

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

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void getUserStatuses_ShouldReturnAllUserStatuses() {
        UserCustomStatusModel s1 = stubStatus(1, "Re-watching");
        UserCustomStatusModel s2 = stubStatus(2, "Collecting");
        when(userCustomStatusService.findByUserId(eq(1))).thenReturn(List.of(s1, s2));

        graphQlTester.document("""
                query {
                    getUserStatuses {
                        id
                        name
                    }
                }
                """)
                .execute()
                .errors().verify()
                .path("getUserStatuses").entityList(UserCustomStatusModel.class)
                .satisfies(results -> {
                    assertThat(results).hasSize(2);
                    assertThat(results).extracting("name").containsExactlyInAnyOrder("Re-watching", "Collecting");
                });

        verify(userCustomStatusService).findByUserId(1);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void getUserStatuses_WhenEmpty_ShouldReturnEmptyList() {
        when(userCustomStatusService.findByUserId(eq(1))).thenReturn(List.of());

        graphQlTester.document("""
                query {
                    getUserStatuses {
                        id
                        name
                    }
                }
                """)
                .execute()
                .errors().verify()
                .path("getUserStatuses").entityList(UserCustomStatusModel.class)
                .satisfies(results -> assertThat(results).isEmpty());

        verify(userCustomStatusService).findByUserId(1);
    }

    @Test
    void getUserStatuses_Unauthenticated_ShouldReturnError() {
        graphQlTester.document("""
                query {
                    getUserStatuses {
                        id
                        name
                    }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors).isNotEmpty());
    }
}
