# User Management REST API

Spring Boot 3.x REST API per la gestione utenti con persistenza PostgreSQL, sicurezza OAuth2 JWT tramite Keycloak, OpenAPI/Swagger, Flyway, MapStruct e suite di test unitari/integration.

## Stack tecnico
- Java 17
- Spring Boot 3.3.x
- Spring Data JPA
- PostgreSQL
- Flyway
- Spring Security Resource Server + Keycloak JWT
- MapStruct
- SpringDoc OpenAPI
- JUnit 5, Mockito, MockMvc, Testcontainers

## Avvio locale
1. Copiare `.env.example` in `.env` e valorizzare le variabili.
2. Avviare PostgreSQL:
   ```bash
   docker compose up -d postgres
   ```
3. Avviare l'applicazione:
   ```bash
   mvn spring-boot:run
   ```
4. Swagger UI disponibile su `http://localhost:8080/swagger-ui.html`.

> Il servizio valida i JWT usando l'issuer Keycloak configurato. Per i test locali con token reali serve un realm/client compatibile oppure il realm remoto fornito.

## Variabili d'ambiente
| Variabile | Descrizione |
|---|---|
| `SPRING_DATASOURCE_URL` | JDBC URL PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | Utente database |
| `SPRING_DATASOURCE_PASSWORD` | Password database |
| `KEYCLOAK_ISSUER_URI` | Issuer URI del realm Keycloak |
| `KEYCLOAK_OPENID_CONFIGURATION_URI` | Endpoint well-known OpenID |
| `KEYCLOAK_CLIENT_ID` | Client id `demo-task` |
| `KEYCLOAK_CLIENT_SECRET` | Client secret letto solo da environment, mai hardcoded |
| `SERVER_PORT` | Porta applicativa |

## Decisioni architetturali
1. **Ruoli separati**: i ruoli dominio (`OWNER`, `OPERATOR`, `MAINTAINER`, `DEVELOPER`, `REPORTER`) sono distinti dai ruoli Keycloak (`ADMIN`, `OPERATOR`, `USER`). I primi descrivono responsabilità applicative dell'utente gestito, i secondi autorizzano chi chiama le API.
2. **Delete fisico**: nessun soft delete; `DELETE /users/{id}` rimuove definitivamente il record e le righe figlie dei ruoli.
3. **Paginazione**: `GET /users` supporta `page`, `size`, `sort` con default `sort=createdAt,desc`.
4. **Error format**: gli errori sono esposti come `application/problem+json` secondo RFC 7807.
5. **ID tecnico**: UUID generato dal backend tramite Hibernate.
6. **Evento post-creazione**: dopo il salvataggio viene pubblicato un evento interno Spring (`UserCreatedEvent`) e gestito in modo asincrono con `@Async`. Questa scelta disaccoppia side-effect futuri (audit, notifiche, integrazioni) dalla transazione HTTP principale.
7. **Visibilità dati per ruolo**:
   - `ADMIN`: vede tutti i campi.
   - `OPERATOR`: non vede `taxCode`.
   - `USER`: non vede `taxCode` e `roles`.

## Sicurezza e RBAC Keycloak
| Ruolo Keycloak | Permessi |
|---|---|
| `ADMIN` | read, create, update, delete |
| `OPERATOR` | read, create, update |
| `USER` | read |

L'applicazione legge i ruoli da `realm_access.roles` del JWT e li converte in authority Spring `ROLE_*`.

## Endpoint API
| Metodo | Endpoint | Permessi |
|---|---|---|
| `GET` | `/users` | `ADMIN`, `OPERATOR`, `USER` |
| `GET` | `/users/{id}` | `ADMIN`, `OPERATOR`, `USER` |
| `POST` | `/users` | `ADMIN`, `OPERATOR` |
| `PUT` | `/users/{id}` | `ADMIN`, `OPERATOR` |
| `DELETE` | `/users/{id}` | `ADMIN` |

### Payload creazione
```json
{
  "username": "mrossi",
  "email": "mario.rossi@example.com",
  "taxCode": "RSSMRA80A01H501U",
  "firstName": "Mario",
  "lastName": "Rossi",
  "roles": ["OWNER", "REPORTER"]
}
```

### Payload update
```json
{
  "username": "mrossi",
  "taxCode": "RSSMRA80A01H501U",
  "firstName": "Mario",
  "lastName": "Rossi",
  "roles": ["MAINTAINER"]
}
```

> L'email è immutabile e non compare nel payload di update.

## Testing
```bash
mvn test
```
- `UserServiceTest`: unit test con Mockito.
- `UserControllerIT`: integration test con `@SpringBootTest` + MockMvc + H2.
- `UserRepositoryIT`: integration test con PostgreSQL via Testcontainers.

## Servizi Docker opzionali
- `docker compose up -d postgres`: solo database.
- `docker compose --profile security up -d`: database + Keycloak locale di supporto. Il Keycloak locale richiede configurazione manuale di realm/client coerenti con l'applicazione.
