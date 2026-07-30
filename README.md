# ProgettoDemoForInterview
```
This is a java project for interview simulation.

Implementation of a stateless REST service acting as an OAuth2 Resource Server. Authentication is delegated to Keycloak, which issues the JWT. The backend validates the token, identifies the calling user, and applies RBAC and filtering. It does not directly handle usernames and passwords, nor does it expose its own login endpoint.
```

# CONTEXT
```
The context is in progetto_candidato_context.md, it is not necessary read progetto_candidato.pdf if present
```

# INSTRUCTIONS

```bash
- The candidate must provide a ZIP file containing the .env file, a PDF with instructions, and a Postman collection to facilitate the interviewer.
- Passwords, secrets, and environment variables must be defined in the .env file located in the user-management-api folder.
- Security note: Do not commit or share the .env file through the source code repository. It must be exchanged only through approved secure channels.
- The command below starts a PostgreSQL container and the Java application. The application will connect to a remote Keycloak instance.


# 1. Copiare e valorizzare le variabili
cp .env.example .env

# 2. Avviare Tutti i Container tranne KEYCLOAK locale
docker compose up --build -d

# 3. Stack Token
Use api getToken in folder STACK_ONLINE_TOKEN situated in postman_collection.json sent from candidate. 

    curl --location 'http://READ_PDF/realms/demo-interview/protocol/openid-connect/token' \
        --header 'Content-Type: application/x-www-form-urlencoded' \
        --data-urlencode 'grant_type=password' \
        --data-urlencode 'client_id=demo-task' \
        --data-urlencode 'client_secret=READ_PDF' \
        --data-urlencode 'username=READ_PDF' \
        --data-urlencode 'password=READ_PDF'

# 4. CREATE A USER
    4.a Insert token on Swagger UI: http://localhost:8080/swagger-ui.html or in Bearer token of Postman Collection
    4.b Create User with http://localhost:8080/swagger-ui/index.html#/user-controller/createUser or createUser of Postman Collection

# 5. CHECK DATABASE
    url --> jdbc:postgresql://localhost:5435/user_management
    user --> user_management
    pwd --> user_management

#5. TEST APP BEHAVIOR WITH DIFFERENT TOKEN

    For the demo, all APIs located in the "LOCALE" folder of the Postman collection must be used, with the "exception" of "getToken" which is located in the STACK_ONLINE_TOKEN folder.
   
```


# CUSTOM AGENTS
Use Spring Backend Expert agent for backend tasks.
```
It is not pushed on this repo
```
