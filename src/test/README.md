# GraphQL Test Suite

## Overview
This test suite provides comprehensive coverage for all GraphQL queries and mutations in the EspacoGeek API.

## Test Structure
Tests are organized by query/mutation type and entity:

```
src/test/java/com/espacogeek/geek/
├── query/
│   ├── user/
│   │   ├── FindUserQueryTest.java       (4 tests)
│   │   └── LoginQueryTest.java          (3 tests)
│   ├── media/
│   │   ├── MediaQueryTest.java          (2 tests)
│   │   ├── TvSerieQueryTest.java        (3 tests)
│   │   ├── GameQueryTest.java           (2 tests)
│   │   └── VisualNovelQueryTest.java    (2 tests)
│   └── quote/
│       └── QuoteQueryTest.java          (2 tests)
└── mutation/
    └── user/
        ├── CreateUserMutationTest.java  (4 tests)
        ├── EditPasswordMutationTest.java (3 tests)
        ├── EditUsernameMutationTest.java (2 tests)
        ├── EditEmailMutationTest.java    (2 tests)
        └── DeleteUserMutationTest.java   (2 tests)
```

**Total: 31 tests**

## Running Tests

### Run all tests
```bash
./gradlew test
```

### Run specific test class
```bash
./gradlew test --tests "com.espacogeek.geek.query.user.FindUserQueryTest"
```

### Run tests with detailed output
```bash
./gradlew test --info
```

### View test report
After running tests, open:
```
build/reports/tests/test/index.html
```

## Technologies Used
- **JUnit 5**: Test framework
- **Mockito**: Mocking framework
- **Spring GraphQL Test**: GraphQL testing support
- **Spring Security Test**: Authentication testing with `@WithMockUser`
- **AssertJ**: Fluent assertions
- **H2 Database**: In-memory database for tests

## Test Patterns

### Query Tests
```java
@GraphQlTest(UserController.class)
@ActiveProfiles("test")
class FindUserQueryTest {
    
    @Autowired
    private GraphQlTester graphQlTester;
    
    @MockBean
    private UserService userService;
    
    @Test
    void findUserById_ShouldReturnUser() {
        // Given
        when(userService.findByIdOrUsernameContainsOrEmail(...))
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
                });
    }
}
```

### Mutation Tests with Authentication
```java
@Test
@WithMockUser(authorities = {"ROLE_user", "ID_1"})
void editPassword_ValidPassword_ShouldReturnOkStatus() {
    // Test implementation
}
```

## Adding New Tests

### 1. Create test class in appropriate directory
- For queries: `src/test/java/com/espacogeek/geek/query/{entity}/`
- For mutations: `src/test/java/com/espacogeek/geek/mutation/{entity}/`

### 2. Use `@GraphQlTest` annotation
```java
@GraphQlTest(YourController.class)
@ActiveProfiles("test")
class YourTest {
    // Test implementation
}
```

### 3. Mock required beans
```java
@MockBean
private YourService yourService;
```

### 4. For authenticated endpoints
Use `@WithMockUser` with proper authorities:
```java
@Test
@WithMockUser(authorities = {"ROLE_user", "ID_1"})
void yourTest() {
    // Test implementation
}
```

## Password Requirements
Tests use passwords that meet the application's validation rules:
- At least 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one number
- At least one special character (!*@#$%^&+=)

Example valid password: `ValidPassword123!`

## Notes
- AOT (Ahead of Time) processing is disabled for tests due to Mockito incompatibility
- Tests use H2 in-memory database instead of MySQL
- Flyway migrations are disabled in test profile
- All tests mock external dependencies (services, APIs, etc.)

## Coverage
- ✅ All User queries and mutations
- ✅ All Media queries (media, tvserie, game, vn)
- ✅ Quote query
- ✅ Both success and error scenarios
- ✅ Authentication and authorization checks
