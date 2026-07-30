# Contesto progetto: User Management REST API

> Documento di contesto ottimizzato per agenti IA. Riflette lo stato **implementato** del progetto. Usare questa fonte come specifica funzionale e tecnica di riferimento. Per ogni nuova modifica, partire da quanto già realizzato e preservarne la coerenza.

## Stato del progetto

**Implementato e funzionante.** Tutti i requisiti obbligatori e la maggior parte di quelli opzionali sono stati completati. Il codice si trova in `user-management-api/`.

## Obiettivo

Servizio REST in Java per gestire l'anagrafica degli utenti di un prodotto: creazione, lettura, modifica, cancellazione e gestione dei ruoli.

## Stack adottato

| Componente | Versione / Tecnologia |
|---|---|
| Linguaggio | Java 17 |
| Framework | Spring Boot 3.3.x |
| Build | Maven (Maven Wrapper) |
| Persistence | Spring Data JPA + Hibernate + Flyway |
| Database prod | PostgreSQL 16 |
| Database test | PostgreSQL via Testcontainers (tutti i test) |
| Security | Spring Security 6 + OAuth2 Resource Server + Keycloak JWT |
| Mapping | MapStruct 1.5.5 + Lombok |
| Documentazione API | SpringDoc OpenAPI 2.6 (Swagger UI) |
| Containerizzazione | Docker + Docker Compose |
| Unit test | JUnit 5, Mockito, BDDMockito, AssertJ |
| Integration test | `@SpringBootTest` + MockMvc + Testcontainers |

## Struttura package

```
com.example.usermanagement/
├── config/
│   ├── SecurityConfig.java          # FilterChain, JwtAuthenticationConverter
│   ├── AsyncConfig.java             # @EnableAsync, executor
│   ├── OpenApiConfig.java
│   ├── audit/
│   │   └── AccessLogFilter.java     # OncePerRequestFilter per audit HTTP
│   └── jwt/
│       ├── JwtConfig.java           # Binding proprietà keycloak.*
│       └── JwtRoleExtractor.java    # Estrae KeycloakRole dal JWT
├── controller/
│   └── UserController.java
├── service/
│   ├── UserService.java             # Interfaccia
│   ├── UserServiceImpl.java
│   └── AccessLogService.java
├── repository/
│   ├── UserRepository.java
│   ├── RoleRepository.java
│   ├── OutboxEventRepository.java
│   └── AccessLogRepository.java
├── model/
│   ├── User.java                    # Entity JPA
│   ├── Role.java                    # Entity JPA (ruoli dominio)
│   ├── AppRole.java                 # Enum: OWNER, OPERATOR, MAINTAINER, DEVELOPER, REPORTER
│   └── AccessLog.java               # Entity JPA per audit
├── dto/
│   ├── CreateUserRequest.java       # record con Bean Validation
│   ├── UpdateUserRequest.java       # record con Bean Validation
│   ├── UserResponse.java            # record
│   └── UserSummaryResponse.java     # record (usato nella paginazione)
├── mapper/
│   └── UserMapper.java              # MapStruct con filtering per ruolo
├── event/
│   ├── UserCreatedEvent.java        # record (payload evento)
│   ├── OutboxEvent.java             # Entity JPA (tabella outbox_events)
│   ├── OutboxEventPublisher.java    # Persiste l'evento nel DB nella stessa transazione
│   ├── OutboxEventPoller.java       # @Scheduled: polling PENDING → dispatcha ApplicationEvent
│   └── UserCreatedEventListener.java # @EventListener: gestisce l'evento applicativo
├── exception/
│   ├── GlobalExceptionHandler.java  # @RestControllerAdvice
│   ├── ResourceNotFoundException.java
│   └── DuplicateEmailException.java
└── enums/
    ├── KeycloakRole.java            # ADMIN, OPERATOR, USER (con canSeeTaxCode / canSeeRoles)
    └── OutboxStatus.java            # PENDING, PROCESSED, FAILED
```

## Modello dominio implementato

### Entity User (`users`)

| Campo | Tipo DB | Note |
|---|---|---|
| `id` | `BIGSERIAL` PK | Generato da sequenza PostgreSQL (`GenerationType.IDENTITY`) |
| `username` | `VARCHAR(100)` UNIQUE NOT NULL | |
| `email` | `VARCHAR(255)` UNIQUE NOT NULL | Immutabile dopo creazione, normalizzata lowercase |
| `tax_code` | `VARCHAR(16)` UNIQUE NOT NULL | Normalizzato uppercase |
| `name` | `VARCHAR(100)` NOT NULL | (campo nel codice: `name`, non `firstName`) |
| `surname` | `VARCHAR(100)` NOT NULL | (campo nel codice: `surname`, non `lastName`) |
| `created_at` | `TIMESTAMP` | Auto, non modificabile |
| `updated_at` | `TIMESTAMP` | Auto, aggiornato ad ogni save |

**Relazioni**: `User` ↔ `Role` = ManyToMany lazy su tabella `user_roles`.

### Entity Role (`roles`)

Enum persistito: `OWNER`, `OPERATOR`, `MAINTAINER`, `DEVELOPER`, `REPORTER` (= `AppRole`).

### Entity AccessLog (`access_logs`)

Registra ogni richiesta HTTP (metodo, path, statusCode, username, ip, userAgent). Popolata da `AccessLogFilter`. Escluse le richieste verso `/actuator`, `/swagger-ui`, `/v3/api-docs`.

### Entity OutboxEvent (`outbox_events`)

Persistenza degli eventi asincroni. Campi: `id` (UUID), `aggregate_type`, `aggregate_id`, `event_type`, `payload` (JSON), `status` (PENDING/PROCESSED/FAILED), `created_at`, `processed_at`.

## Requisiti funzionali: stato implementazione

| # | Requisito | Stato |
|---|---|---|
| 1 | `GET /users` — lista paginata | ✅ |
| 2 | `GET /users/{id}` — dettaglio | ✅ |
| 3 | `POST /users` — creazione con ruoli | ✅ |
| 4 | `PUT /users/{id}` — modifica (email esclusa) | ✅ |
| 5 | `DELETE /users/{id}` — cancellazione fisica | ✅ |

## Requisiti opzionali: stato implementazione

| Requisito | Stato | Note |
|---|---|---|
| Autenticazione JWT (Keycloak) | ✅ | Resource Server con JWT |
| Tracciamento utente autenticato | ✅ | Via `AccessLogFilter` e `SecurityContextHolder` |
| RBAC Keycloak | ✅ | `@PreAuthorize("hasAuthority('...')")` su ogni endpoint |
| Filtering risposta per ruolo | ✅ | In `UserMapper` via `KeycloakRole` |
| Evento asincrono post-creazione | ✅ | Outbox Pattern (vedi sotto) |
| Paginazione `GET /users` | ✅ | `Pageable` con default `sort=createdAt,desc` |

## Endpoints API

| Metodo | Path | Authority richiesta | Note |
|---|---|---|---|
| `GET` | `/users` | `read_user` | Paginato; accettato da ADMIN, OPERATOR, USER |
| `GET` | `/users/{id}` | `read_user` | |
| `POST` | `/users` | `create_user` | Restituisce 201 + `Location` header |
| `PUT` | `/users/{id}` | `update_user` | |
| `DELETE` | `/users/{id}` | `delete_user` | Restituisce 204 |

## Sicurezza e RBAC: implementazione effettiva

Il `JwtAuthenticationConverter` estrae **due tipi** di authority dal JWT:

1. **Realm roles** (`realm_access.roles`) → authority `ROLE_ADMIN`, `ROLE_OPERATOR`, `ROLE_USER`
   - Usati da `JwtRoleExtractor` per determinare il `KeycloakRole` e applicare il filtering della risposta.

2. **Client roles** (`resource_access.demo-task.roles`) → authority piatta `read_user`, `create_user`, `update_user`, `delete_user`
   - Usati da `@PreAuthorize("hasAuthority('...')")` sul controller per l'autorizzazione.

### Matrice ruoli Keycloak

| Ruolo Keycloak | Authority client | Vede `taxCode` | Vede `roles` |
|---|---|---|---|
| `ADMIN` | `read_user`, `create_user`, `update_user`, `delete_user` | ✅ | ✅ |
| `OPERATOR` | `read_user`, `create_user`, `update_user` | ❌ | ✅ |
| `USER` | `read_user` | ❌ | ❌ |

### Configurazione Keycloak (da environment)

- Issuer URI: `KEYCLOAK_ISSUER_URI`
- Jwk Set URI `KEYCLOAK_JWK_SET_URI`
- OpenID config: `https://idpgw.test4mind.com/realms/demo-interview/.well-known/openid-configuration`
- Client ID: `demo-task` (variabile `KEYCLOAK_CLIENT_ID`)
- Client secret: solo da variabile `KEYCLOAK_CLIENT_SECRET` — mai nel codice

## Evento asincrono: Outbox Pattern

Implementato con pattern **Transactional Outbox** per garantire consistenza senza distributed transaction:

1. `UserServiceImpl.createUser()` persiste l'utente e un `OutboxEvent` (status=PENDING) nella **stessa transazione**.
2. `OutboxEventPoller` esegue `@Scheduled(fixedDelay=${app.outbox.poll-interval-ms:5000})`, carica gli eventi PENDING e li pubblica come `ApplicationEvent` Spring.
3. `UserCreatedEventListener` riceve l'evento e lo elabora (logging, side-effect futuri).
4. In caso di errore, l'`OutboxEvent` passa a status FAILED (con log di errore).

> Scelta: questa soluzione garantisce che nessun evento venga perso anche in caso di crash dopo il salvataggio, a costo di un polling DB ogni 5 secondi (configurabile).

## Payload DTO

### `CreateUserRequest`
```json
{
  "username": "mrossi",
  "email": "mario.rossi@example.com",
  "taxCode": "RSSMRA80A01H501U",
  "name": "Mario",
  "surname": "Rossi",
  "roles": ["OWNER", "REPORTER"]
}
```

### `UpdateUserRequest` (email non presente — immutabile)
```json
{
  "username": "mrossi",
  "taxCode": "RSSMRA80A01H501U",
  "name": "Mario",
  "surname": "Rossi",
  "roles": ["MAINTAINER"]
}
```

### `UserResponse` (campi filtrati per ruolo)
```json
{
  "id": 1,
  "username": "mrossi",
  "email": "mario.rossi@example.com",
  "taxCode": "RSSMRA80A01H501U",
  "name": "Mario",
  "surname": "Rossi",
  "roles": ["OWNER", "REPORTER"],
  "createdAt": "2024-01-01T10:15:30Z",
  "updatedAt": "2024-01-01T10:15:30Z"
}
```

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

## Testing

### File di test

| File | Tipo | Cosa testa |
|---|---|---|
| `UserServiceTest` | Unit (Mockito) | createUser (evento + filtering OPERATOR), email duplicata, updateUser (email immutabile), getUser (filtering USER) |
| `UserControllerIT` | Integration (MockMvc + Testcontainers PostgreSQL) | Filtering per ruolo (GET), creazione (OPERATOR), 403 su DELETE con USER |
| `UserRepositoryIT` | Integration (Testcontainers PostgreSQL) | Constraint unicità email a livello DB |

### Esecuzione test

```bash
# dalla directory user-management-api/
./mvn test
```

I test di integrazione richiedono Docker in esecuzione (Testcontainers). Se Docker non è disponibile, `@Testcontainers(disabledWithoutDocker = true)` salta i container test.

## Avvio locale

```bash
# 1. Copiare e valorizzare le variabili
cp .env.example .env

# 2. Avviare Tutti i Container tranne KEYCLOAK locale
docker compose up --build -d

# 2B. [OPZIONALE e solo ai fini di test]  keycloak si avvierà solamente con il profilo security caricando automaticamente la configurazione dalla cartella keycloack. Bisognerà inserire solo le pwd degli utenti e ricreare il KEYCLOAK_CLIENT_SECRET da ui in quanto non è stata configurata la persistenza del dato. In questo caso andranno cambiate le configurazioni del file env.
docker compose --profile security up --build -d

# Swagger UI: http://localhost:8080/swagger-ui.html
```

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

## Ambiguità risolte

1. **Ruoli dominio vs Keycloak**: tenuti separati (vedi decisione n.2). `AppRole` per i dati, `KeycloakRole` per l'autorizzazione API.
2. **Diagramma UML**: ricostruito dai campi testuali. I campi del modello sono `name`/`surname` (non `firstName`/`lastName`).
3. **Paginazione**: implementata con `Pageable` Spring Data.
4. **Formato errore**: RFC 7807 `problem+json`.
5. **Soft delete**: non implementato — delete fisico.
6. **Modifica ruoli**: `PUT /users/{id}` sostituisce l'intero set di ruoli con quello fornito nel payload.
7. **Concorrenza**: nessun lock ottimistico; accettabile per il contesto dimostrativo.

## Istruzione operativa per l'IA

- **Non modificare** l'architettura a layer, il pattern Outbox, la struttura DTO/mapper/entity senza motivazione esplicita.
- **Preservare**: i nomi di campo `name`/`surname` (non `firstName`/`lastName`), il tipo ID `Long`, il filtering via `KeycloakRole`.
- **Aggiungere test** proporzionati a ogni modifica della business logic.
- **Non introdurre** segreti, dipendenze non necessarie o complessità non giustificate.
- **Segnalare** sempre assunzioni, rischi di sicurezza e decisioni che richiedono conferma prima di implementarle.