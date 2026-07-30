# ProgettoDemoForInterview
This is a java project for interview simulation

# CONTEXT
```
The context is in progetto_candidato_context.md, it is not necessary read progetto_candidato.pdf if present
```

# INSTRUCTIONS

```bash
- Passwords, secrets, and environment variables must be defined in the .env file located in the user-management-api folder.
- Security note: Do not commit or share the .env file through the source code repository. It must be exchanged only through approved secure channels.
- The command below starts a PostgreSQL container and the Java application. The application will connect to a remote Keycloak instance.


# 1. Copiare e valorizzare le variabili
cp .env.example .env

# 2. Avviare Tutti i Container tranne KEYCLOAK locale
docker compose up --build -d

```



# CUSTOM AGENTS
Use Spring Backend Expert agent for backend tasks.
