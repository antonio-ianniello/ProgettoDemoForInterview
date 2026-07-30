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
| `KEYCLOAK_ISSUER_URI` | Issuer URI del realm |
| `KEYCLOAK_OPENID_CONFIGURATION_URI` | Endpoint well-known OpenID |
| `KEYCLOAK_JWK_SET_URI` | jwk set URI |
| `KEYCLOAK_CLIENT_ID` | `demo-task` |
| `KEYCLOAK_CLIENT_SECRET` | Solo da environment, mai hardcoded |
| `SERVER_PORT` | Porta applicativa (default 8080) |
| `APP_OUTBOX_POLL_INTERVAL_MS` | Intervallo polling outbox (default 5000) |

## Decisioni architetturali adottate

1. **ID tecnico**: `Long` con `GenerationType.IDENTITY` (sequenza PostgreSQL). Semplice, performante, sufficiente per questo contesto.
2. **Ruoli separati**: I ruoli dominio (`AppRole` enum: `OWNER`, `OPERATOR`, `MAINTAINER`, `DEVELOPER`, `REPORTER`) descrivono la responsabilità dell'utente gestito. I ruoli Keycloak (`ADMIN`, `OPERATOR`, `USER`) autorizzano chi chiama le API. I due insiemi sono ortogonali.
3. **Delete fisico**: `DELETE /users/{id}` rimuove definitivamente il record. Nessun soft delete.
4. **Email immutabile**: la colonna ha `updatable = false` sia a livello JPA sia a livello Flyway. L'`UpdateUserRequest` non include il campo email.
5. **Normalizzazione input**: email → lowercase trim; taxCode → uppercase trim; username/name/surname → trim. Eseguita nel mapper MapStruct, non nel service.
6. **Formato errore**: `application/problem+json` (RFC 7807) gestito da `GlobalExceptionHandler` con `@RestControllerAdvice`.
7. **Paginazione**: default `page=0`, `size=20`, `sort=createdAt,desc`. Esposta come `Page<UserSummaryResponse>`.
8. **Filtering risposta**: implementato in `UserMapper` con condizioni sul `KeycloakRole`. L'enum ha i metodi `canSeeTaxCode()` e `canSeeRoles()` per mantenere la logica nel tipo stesso.
9. **Audit**: `AccessLogFilter` (`OncePerRequestFilter`) persiste ogni accesso HTTP in `access_logs`. Il campo `username` è il `subject` del JWT; non viene loggato per richieste anonime.
10. **Evento asincrono**: Outbox Pattern (vedi sezione dedicata). Non si usa `@Async` semplice per evitare perdita eventi in caso di crash.

## Sicurezza e RBAC Keycloak
| Ruolo Keycloak | Permessi |
|---|---|
| `ADMIN` | read, create, update, delete |
| `OPERATOR` | read, create, update |
| `USER` | read |

L'applicazione legge i ruoli da `realm_access.roles` del JWT e li converte in authority Spring `ROLE_*`.

## Endpoint API
| Metodo | Path | Authority richiesta | Note |
|---|---|---|---|
| `GET` | `/users` | `read_user` | Paginato; accettato da ADMIN, OPERATOR, USER |
| `GET` | `/users/{id}` | `read_user` | |
| `POST` | `/users` | `create_user` | Restituisce 201 + `Location` header |
| `PUT` | `/users/{id}` | `update_user` | |
| `DELETE` | `/users/{id}` | `delete_user` | Restituisce 204 |

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
- `UserControllerIT`: integration test con `@SpringBootTest` + MockMvc + PostgreSQL via Testcontainers.
- `UserRepositoryIT`: integration test con PostgreSQL via Testcontainers.

## Servizi Docker opzionali
- `docker compose up -d postgres`: solo database.
- `docker compose --profile security up -d`: database + Keycloak locale di supporto. Il Keycloak locale richiede configurazione manuale di realm/client coerenti con l'applicazione.In particolare nella cartella keycloak è presente un file di configurazione che simula il keycloack remoto. Non è stata implementata la persistenza del dato, quindi ad ogni riavvio di quel container bisognerà aggiornare la KEYCLOAK_CLIENT_SECRET e le pwd degli utenti.
