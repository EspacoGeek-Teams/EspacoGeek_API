# GraphQL API Guide

## Accessing GraphiQL

GraphiQL is an interactive GraphQL IDE that allows you to explore and test the API.

### Local Development
Once the application is running, access GraphiQL at:
```
http://localhost:8080/graphiql
```

### GraphQL Endpoint
The GraphQL API endpoint is:
```
http://localhost:8080/api
```

## Using GraphiQL

### 1. Exploring the Schema
- Click the "Docs" button on the right side of GraphiQL to browse all available queries and mutations
- The schema includes detailed descriptions and examples for each operation
- Use auto-complete (Ctrl+Space) to discover fields and operations

### 2. Example Queries

#### Search for TV Series
```graphql
query {
  tvserie(name: "Breaking Bad") {
    content {
      id
      name
      about
      cover
      genre {
        name
      }
    }
    totalElements
  }
}
```

#### Search for Games
```graphql
query {
  game(name: "The Last of Us", page: 0, size: 10) {
    content {
      id
      name
      about
      cover
      genre {
        name
      }
    }
    totalPages
    totalElements
  }
}
```

#### Get Daily Quote Artwork
```graphql
query {
  dailyQuoteArtwork {
    quote
    author
    urlArtwork
  }
}
```

### 3. Authentication

Most mutations require authentication. First, login to get a JWT token:

```graphql
query {
  login(email: "user@example.com", password: "yourpassword")
}
```

Then, add the token to the HTTP Headers in GraphiQL:
```json
{
  "Authorization": "Bearer YOUR_JWT_TOKEN_HERE"
}
```

### 4. Example Mutations

#### Create a New User (No authentication required)
```graphql
mutation {
  createUser(credentials: {
    username: "johndoe"
    email: "john@example.com"
    password: "securepassword123"
  })
}
```

#### Change Password (Requires authentication)
```graphql
mutation {
  editPassword(
    actualPassword: "currentpassword"
    newPassword: "newsecurepassword"
  )
}
```

#### Update Username (Requires authentication)
```graphql
mutation {
  editUsername(
    password: "yourpassword"
    newUsername: "newusername"
  )
}
```

## Tips

1. **Use Variables**: For cleaner queries, use GraphQL variables:
   ```graphql
   query SearchGame($name: String!, $page: Int, $size: Int) {
     game(name: $name, page: $page, size: $size) {
       content {
         id
         name
       }
     }
   }
   ```
   Variables panel:
   ```json
   {
     "name": "The Last of Us",
     "page": 0,
     "size": 10
   }
   ```

2. **Request Only What You Need**: GraphQL allows you to specify exactly which fields you want, reducing data transfer.

3. **Explore Nested Fields**: Use the Docs panel to discover all available nested fields in types like Media, Genre, etc.

4. **Format Your Queries**: Use the "Prettify" button to auto-format your GraphQL queries.

## Troubleshooting

### GraphiQL is not accessible
- Ensure the application is running
- Check that you're accessing the correct URL: `http://localhost:8080/graphiql`
- Verify the port number matches your configuration

### Authentication errors
- Make sure you've included the JWT token in the Authorization header
- Verify the token is still valid (tokens may expire)
- Check that you're using the format: `Bearer YOUR_TOKEN`

### Database errors
- Ensure MySQL database is running and accessible
- Verify environment variables are set correctly:
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`

## Additional Resources

- [GraphQL Official Documentation](https://graphql.org/learn/)
- [Spring for GraphQL Documentation](https://docs.spring.io/spring-graphql/reference/)
