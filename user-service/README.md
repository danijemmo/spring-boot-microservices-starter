# User Service

Spring Boot user service for the ecommerce project. It stores local user data in PostgreSQL and creates/authenticates users through Keycloak.

## Local Configuration

The default local settings are in `src/main/resources/application.yaml`:

```yaml
server:
  port: 8082

keycloak:
  token-url: http://localhost:8080/realms/ecommerce/protocol/openid-connect/token
  server-url: http://localhost:8080
  realm: ecommerce
  client-id: ecommerce-app
  client-secret: <client-secret>
  default-role: ROLE_USER

spring:
  mail:
    host: localhost
    port: 1025

app:
  mail:
    from: no-reply@ecommerce.local
  security:
    password-reset-token-ttl-minutes: 15

grpc:
  server:
    port: 9090
```

Use a Keycloak client from the `ecommerce` realm. Do not point this service at the `master` realm unless the code is changed to separate the admin token realm from the target user realm.

## Keycloak Realm Setup

Create or verify this Keycloak configuration before running registration.

### 1. Create the Realm

Create a realm named:

```text
ecommerce
```

The token endpoint must be reachable at:

```text
http://localhost:8080/realms/ecommerce/protocol/openid-connect/token
```

If this endpoint returns `404`, the realm name or Keycloak base URL is wrong.

### 2. Create the Application Client

In realm `ecommerce`, create or update client:

```text
ecommerce-app
```

Recommended client settings:

```text
Client authentication: ON
Service accounts roles: ON
Direct access grants: ON
Standard flow: optional
Valid redirect URIs: http://localhost:*
Web origins: http://localhost:*
```

`Direct access grants` is required because this service logs users in with the password grant at `/api/auth/login`.

Copy the client secret from:

```text
Clients -> ecommerce-app -> Credentials
```

Then set it in `application.yaml`:

```yaml
keycloak:
  client-id: ecommerce-app
  client-secret: <copied-secret>
```

## Password Reset Flow

Password reset is handled locally by this service.

1. `POST /api/auth/forgot-password` with the user's email.
2. The service generates a one-time token, stores a hashed copy in PostgreSQL, and emails the raw token.
3. `POST /api/auth/reset-password` with `email`, `token`, and `newPassword`.
4. The service verifies the token locally and then updates the password in Keycloak.

The mail server settings in `spring.mail` must point to a reachable SMTP server for this flow to work.

### 3. Assign Admin Permissions to the Service Account

The Spring service uses `client_credentials` to call the Keycloak Admin API. The roles must be assigned to the service account user, not merely created on the client.

Open:

```text
Clients -> ecommerce-app -> Service accounts roles
```

Click `Assign role`, filter by clients, then assign these roles from client `realm-management`:

```text
manage-users
view-users
query-users
manage-realm
```

Alternative path:

```text
Users -> service-account-ecommerce-app -> Role mapping -> Assign role
```

Then filter by clients and assign the same `realm-management` roles.

### 4. Create the Default User Role

This service assigns a realm-level role to every newly registered user:

```yaml
keycloak:
  default-role: ROLE_USER
```

Create this as a realm role:

```text
Realm roles -> Create role -> ROLE_USER
```

If your realm role is named `USER` instead, update the service config:

```yaml
keycloak:
  default-role: USER
```

This must be a realm role. The current implementation does not assign client roles.

## Verify Keycloak Access

Request a service-account token:

```bash
curl -s -X POST http://localhost:8080/realms/ecommerce/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=ecommerce-app" \
  -d "client_secret=<client-secret>"
```

Decode the returned `access_token`. It must contain `realm-management` roles:

```json
{
  "resource_access": {
    "realm-management": {
      "roles": [
        "manage-users",
        "view-users",
        "query-users",
        "manage-realm"
      ]
    }
  }
}
```

If `resource_access.realm-management` is missing, registration will fail with `403`.

## Common Keycloak Errors

`404 Not Found` while fetching `grantToken`:

The realm URL is wrong. Check `keycloak.server-url`, `keycloak.realm`, and `keycloak.token-url`.

`401 Unauthorized` while fetching `grantToken`:

The client id or secret is wrong, or `Client authentication` is disabled.

`403 Forbidden` while creating the user:

The service account does not have `realm-management/manage-users`.

`Admin client is not allowed to assign role 'ROLE_USER'`:

The service account can create users but cannot assign realm roles. Add `realm-management/manage-realm`.

`Realm role 'ROLE_USER' does not exist`:

Create `ROLE_USER` under `Realm roles`, or change `keycloak.default-role` to the existing realm role name.

## Run

Start the service:

```bash
./mvnw spring-boot:run
```

Compile without tests:

```bash
./mvnw -DskipTests compile
```

Run tests:

```bash
./mvnw test
```

If tests fail because port `9090` is already in use, stop the process using that port or configure a different gRPC port for tests.
