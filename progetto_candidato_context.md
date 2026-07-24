# Contesto progetto: User Management REST API

> Documento di contesto ottimizzato per agenti IA. Usare questa fonte come specifica funzionale di riferimento. Se una decisione non è definita qui, proporre una soluzione semplice, documentata e coerente con Java/Spring.

## Obiettivo

Realizzare un servizio REST in Java per gestire l'anagrafica degli utenti di un prodotto. Il servizio deve consentire creazione, lettura, modifica e cancellazione degli utenti, oltre alla gestione dei relativi ruoli.

## Stack preferenziale

- Java
- Spring Boot / Spring Web
- Maven
- Spring Data JPA/Hibernate
- Database a scelta: preferibilmente PostgreSQL; H2 per test veloci
- Docker/Docker Compose per dipendenze dei test di integrazione
- JUnit 5, Mockito e test Spring
- Spring Security con integrazione Keycloak/OAuth2 JWT
- API documentata con OpenAPI/Swagger, se utile

## Modello dominio

### User

Campi obbligatori:

- `username`
- `email`
- `taxCode` / codice fiscale
- `firstName` / nome
- `lastName` / cognome
- uno o più ruoli applicativi

Vincoli:

- `email` è unica e non modificabile dopo la creazione.
- Non possono esistere due utenti con la stessa email.
- Un utente può avere uno o più ruoli.
- I ruoli applicativi indicati nella specifica sono: `OWNER`, `OPERATOR`, `MAINTAINER`, `DEVELOPER`, `REPORTER`.

La scelta dell'identificativo tecnico, delle cardinalità esatte e delle regole di validazione non specificate è lasciata all'implementazione, ma deve essere esplicitata nel README.

## Requisiti funzionali obbligatori

1. Ottenere la lista degli utenti.
2. Ottenere il dettaglio di un singolo utente.
3. Creare un utente con i suoi ruoli.
4. Modificare un utente esistente o i suoi ruoli.
5. Cancellare un utente.

Comportamenti REST attesi da definire/documentare:

- `GET /users`
- `GET /users/{id}`
- `POST /users`
- `PUT` o `PATCH /users/{id}`
- `DELETE /users/{id}`
- errori di validazione, utente inesistente, email duplicata e accesso negato con status HTTP appropriati.

## Requisiti opzionali

### Autenticazione

Integrare autenticazione tramite:

- token JWT; oppure
- OAuth2 Authorization Code.

È gradito tracciare applicativamente l'utente autenticato che esegue le operazioni.

Configurazione Keycloak fornita dalla specifica:

- OpenID configuration: `https://idpgw.test4mind.com/realms/demo-interview/.well-known/openid-configuration`
- client id: `demo-task`
- client secret: fornito nel PDF originale; non inserirlo nel repository. Usare environment variable o secret manager.

### RBAC

Implementare autorizzazione coerente con ruoli e permessi Keycloak:

| Ruolo | Permessi |
|---|---|
| `ADMIN` | `read_user`, `create_user`, `update_user`, `delete_user` |
| `OPERATOR` | `read_user`, `create_user`, `update_user` |
| `USER` | `read_user` |

Utenti Keycloak di test indicati nella specifica:

- `admin_user`
- `creator_user`
- `reader_user`

Le password sono presenti solo nel PDF originale e devono essere trattate come credenziali di test riservate: mai hardcodarle o committarle.

### Filtering della risposta

Condizionato al completamento di autenticazione e RBAC:

- `ADMIN`: vede tutti gli attributi dell'utente.
- `OPERATOR`: non vede `taxCode`.
- `USER`: non vede `taxCode` né `roles`.

Implementare il filtering a livello DTO/response mapping, non esponendo accidentalmente l'entity JPA direttamente.

### Evento asincrono

Pubblicare un evento asincrono dopo la creazione di un nuovo utente. La tecnologia è libera: evento interno Spring, RabbitMQ, VerneMQ, WebSocket o STOMP. La soluzione scelta deve essere documentata e coperta da test.

## Qualità tecnica attesa

- Separazione chiara tra controller, application/service, domain e persistence.
- DTO per input/output; validazione con Bean Validation.
- Gestione centralizzata degli errori con payload coerente.
- Transazioni esplicite dove necessarie.
- Vincoli di unicità garantiti anche dal database, non solo dal service.
- Codice leggibile, con responsabilità singole e dipendenze minimali.
- Uso ragionato dei design pattern: evitare astrazioni non necessarie.
- Logging utile senza registrare password, token o dati sensibili.
- Configurazione esterna per database, Keycloak e broker.
- README con avvio locale, variabili d'ambiente, API, decisioni architetturali e limiti noti.

## Testing

### Unit test

- JUnit 5 e Mockito.
- Testare service, validazioni, regole di business, gestione duplicati e mapping.
- Testare autorizzazione e filtering dove applicabile.

### Integration test

- Test Spring del layer web e persistence.
- Docker/Testcontainers per database e, se adottato, broker.
- Verificare il flusso completo delle operazioni CRUD.
- Verificare constraint email unica a livello database.
- Se possibile, usare un Keycloak container o mock controllati per gli scenari security.

## Consegna e Definition of Done

Il progetto deve essere sviluppato in Java e pubblicato su un repository GitHub condivisibile con il recruiter.

È completo quando:

- i requisiti funzionali obbligatori sono implementati;
- compila senza errori;
- la build Maven termina correttamente;
- i test passano;
- l'applicazione può essere avviata e utilizzata;
- i requisiti opzionali implementati sono documentati;
- non sono presenti segreti nel codice o nella cronologia Git.

## Ambiguità da risolvere esplicitamente

1. La specifica usa i ruoli dominio `OWNER`, `OPERATOR`, `MAINTAINER`, `DEVELOPER`, `REPORTER`, ma l'RBAC Keycloak usa `ADMIN`, `OPERATOR`, `USER`. Non assumere che siano lo stesso insieme: documentare la scelta di separare ruoli dominio e ruoli tecnici, oppure definire una mappatura.
2. Il PDF cita un diagramma UML, ma il testo estratto non ne contiene i dettagli. Ricostruire il modello solo dai campi e vincoli testuali, salvo verifica visuale del diagramma originale.
3. Non sono specificati paginazione, ordinamento, formato errore, soft delete, audit, gestione concorrenza o semantica precisa di modifica ruoli. Scegliere default ragionevoli e documentarli.

## Istruzione operativa per l'IA

Quando modifichi il progetto, parti dai requisiti obbligatori, preserva compatibilità con lo stack sopra indicato, aggiungi test proporzionati alla modifica e segnala sempre assunzioni, rischi di sicurezza e decisioni che richiedono conferma. Non introdurre segreti, dipendenze o complessità non necessarie.
