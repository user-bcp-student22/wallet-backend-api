# Spec Delta

## Purpose

Definir el paso simulado de validación facial para la demo, su resultado observable y el control de avance entre el registro de passkey y la verificación del celular. La simulación no realiza KYC ni valida una identidad real.

## ADDED Requirements

### Requirement: Simulación facial solo para demo

El backend MUST obtener el resultado de la simulación desde una configuración/mock del servidor, disponible solo en perfiles `dev` y `test`. MUST NOT recibir imágenes, plantillas biométricas ni una decisión de aprobación enviada por el cliente. El resultado MUST NOT presentarse como identidad real verificada.

#### Scenario: El backend produce un resultado de demo
- **WHEN** una persona solicita la simulación con una sesión válida y la configuración mock está disponible
- **THEN** el backend devuelve el resultado configurado (`APPROVED` o `NOT_APPROVED`) sin capturar ni transmitir datos faciales

#### Scenario: El mock no está disponible
- **WHEN** se solicita la simulación y el proveedor mock no está disponible
- **THEN** el backend responde `503 Service Unavailable` con `application/problem+json` y no cambia el estado de onboarding

### Requirement: La passkey permanece separada de la simulación

Completar el registro obligatorio de passkey MUST NOT completar por sí solo la simulación. La respuesta del registro de passkey SHALL dejar el onboarding en `FACIAL_VERIFICATION_PENDING` con `nextAction` `VERIFY_FACIAL_IDENTITY`. `user.biometricRegistered` MUST seguir indicando exclusivamente que se registró la passkey.

#### Scenario: La passkey se completa
- **WHEN** se registra correctamente la passkey
- **THEN** `biometricRegistered` es `true` y el onboarding queda en `FACIAL_VERIFICATION_PENDING` / `VERIFY_FACIAL_IDENTITY`

### Requirement: El endpoint de simulación controla el avance

`POST /v1/onboarding/facial-verifications` MUST requerir JWT Bearer y el estado `FACIAL_VERIFICATION_PENDING`. No recibe body. Un estado distinto MUST responder `409 Conflict` y no cambiar el onboarding.

#### Scenario: La persona solicita la simulación en el paso correcto
- **WHEN** la persona autenticada llama al endpoint mientras su estado es `FACIAL_VERIFICATION_PENDING`
- **THEN** el backend procesa el resultado mock y devuelve el resultado junto con el onboarding resultante

#### Scenario: La persona intenta omitir la passkey o repetir fuera de estado
- **WHEN** se llama el endpoint sin JWT válido o el onboarding no está en `FACIAL_VERIFICATION_PENDING`
- **THEN** el backend responde `401 Unauthorized` o `409 Conflict`, respectivamente, sin avanzar el onboarding

### Requirement: Solo el resultado aprobado permite continuar

El resultado `APPROVED` MUST cambiar el onboarding a `PHONE_PENDING` / `ENTER_PHONE`. El resultado `NOT_APPROVED`, que incluye un resultado indeterminado en esta demo, MUST mantener el paso facial pendiente. Ninguno de estos resultados activa por sí solo la wallet.

#### Scenario: Resultado aprobado
- **WHEN** el mock devuelve `APPROVED`
- **THEN** la respuesta es `200 OK`, informa `result: APPROVED` y el onboarding pasa a `PHONE_PENDING` / `ENTER_PHONE`

#### Scenario: Resultado rechazado o indeterminado
- **WHEN** el mock devuelve `NOT_APPROVED`
- **THEN** la respuesta es `200 OK`, informa `result: NOT_APPROVED` y el onboarding permanece en `FACIAL_VERIFICATION_PENDING` / `VERIFY_FACIAL_IDENTITY`

#### Scenario: Resultado no disponible
- **WHEN** el mock no puede producir un resultado
- **THEN** el backend responde `503` y el celular continúa bloqueado

### Requirement: Datos de la simulación no son datos biométricos

El endpoint MUST NOT aceptar ni devolver imágenes o plantillas faciales. El resultado simulado MUST NOT incluirse en eventos analíticos; la demo no emite eventos de este paso.

#### Scenario: Solicitud y respuesta de demo
- **WHEN** se ejecuta la simulación
- **THEN** el request no contiene datos faciales y la respuesta solo expone el resultado simulado y el estado de onboarding

### Requirement: La wallet sigue inactiva hasta completar el onboarding

La wallet MUST NOT considerarse activa hasta que se completen todos los pasos obligatorios y el estado sea `COMPLETED` / `OPEN_HOME`.

#### Scenario: Simulación aprobada pero celular pendiente
- **WHEN** el resultado es `APPROVED` pero el celular todavía no se verificó
- **THEN** el estado no es `COMPLETED` / `OPEN_HOME` y la wallet permanece inactiva