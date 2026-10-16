# Tasks

Referencias: [proposal.md](./proposal.md), [spec](./specs/facial-identity-verification/spec.md) y [design.md](./design.md).

Orden: caracterizar primero el contrato actual; escribir las pruebas del comportamiento nuevo antes de implementarlo; cambiar solo la simulación y sus contratos.

## 1. Caracterización previa

- [x] 1.1 Ejecutar `gradlew.bat test --tests "*AuthSessionServiceTest" --tests "*GoogleAuthResourceTest" --no-daemon` antes de cambios funcionales. Verificación: suite verde y resultado registrado como baseline. Evidencia: `AuthSessionServiceTest` y `GoogleAuthResourceTest` pasan con `QUARKUS_HTTP_TEST_PORT=19081`; sin el puerto alternativo la prueba HTTP chocó con un proceso preexistente en `8081`.
- [x] 1.2 Confirmar una prueba de regresión que asegure que `POST /v1/auth/google` sigue siendo público y conserva `BIOMETRIC_PENDING` / `REGISTER_BIOMETRIC`, `biometricRegistered: false` y la forma actual de `AuthSession`. Verificación: `GoogleAuthResourceTest.acceptsPublicRequestAndReturnsNewAccountSession` pasa sin header `Authorization`.

## 2. Pruebas del comportamiento simulado

- [x] 2.1 Añadir pruebas unitarias para la transición de passkey completada a `FACIAL_VERIFICATION_PENDING` / `VERIFY_FACIAL_IDENTITY`, manteniendo `biometricRegistered: true`. Verificación: solo cambia el estado de onboarding; el significado de passkey no cambia. Evidencia: `FacialVerificationServiceTest.completedPasskeyStartsFacialVerificationWithoutChangingBiometricMeaning`.
- [x] 2.2 Añadir pruebas unitarias para los resultados configurados `APPROVED`, `NOT_APPROVED` y `UNAVAILABLE`. Verificación: resultado determinista y ningún dato facial requerido. Evidencia: `FacialVerificationServiceTest` cubre los tres resultados y la no mutación ante indisponibilidad.
- [x] 2.3 Añadir pruebas de recurso para `POST /v1/onboarding/facial-verifications`: JWT requerido, estado permitido, sin body facial, resultado y estado de respuesta según cada escenario. Verificación: `APPROVED` permite `PHONE_PENDING`; `NOT_APPROVED` no avanza; `UNAVAILABLE` responde `503` sin mutar el onboarding. Evidencia: `FacialVerificationResourceTest`, `FacialVerificationNotApprovedResourceTest` y `FacialVerificationUnavailableResourceTest` pasan.
- [x] 2.4 Añadir pruebas para estado previo incorrecto y proveedor mock no disponible. Verificación: `409 Conflict` para estado incorrecto, `application/problem+json` para `503`, y ningún caso marca la wallet como activa. Evidencia: pruebas de estado previo, `503` Problem y persistencia del estado facial pendiente pasan.

## 3. Implementación incremental

- [x] 3.1 Añadir el resultado de demo y el mock configurado para `dev`/`test`, siguiendo el patrón de perfil de `MockGoogleIdentityProvider`. Verificación: pruebas unitarias para los tres resultados; el mock no está disponible en perfiles productivos. Evidencia: `MockFacialVerificationProvider` y `FacialVerificationService` están limitados a perfiles `dev`/`test`; pruebas de resultados en verde.
- [x] 3.2 Extender `AccountRepository` con la búsqueda/actualización mínima por cuenta y añadir la transición de onboarding necesaria. Verificación: pruebas del repositorio y del servicio confirman transición a facial pendiente y avance solo con `APPROVED`. Evidencia: `findById`/`update`, transición de passkey y resultado aprobado/rechazado cubiertos por pruebas.
- [x] 3.3 Añadir los modelos de respuesta del endpoint conservando los campos existentes de `Onboarding`. Verificación: serialización contiene solo `result` y el estado de onboarding, sin imágenes, plantillas ni datos personales adicionales. Evidencia: `FacialVerificationResponse` y aserciones HTTP para `result`, `onboarding` y ausencia de campos faciales.
- [x] 3.4 Implementar `POST /v1/onboarding/facial-verifications` protegido por JWT y válido únicamente en `FACIAL_VERIFICATION_PENDING`. Verificación: pruebas de recurso de la sección 2 pasan; `UNAVAILABLE` no muta el estado. Evidencia: suite focal de recursos faciales en verde.

## 4. Contrato de integración

- [x] 4.1 Actualizar `ENDPOINTS.md` para reflejar passkey → simulación → celular, documentar el endpoint protegido, los resultados y el `503`; aclarar que no es KYC real. Verificación: documentación coincide con la spec. Evidencia: flujo, estados, request sin body, resultados y errores documentados.
- [x] 4.2 Actualizar `openapi.yaml` con la operación, autenticación, respuestas, estados y esquema de resultado. Verificación: YAML válido y contrato coherente con `ENDPOINTS.md` y `spec.md`. Evidencia: `yaml.safe_load` valida el documento y confirma la ruta `/v1/onboarding/facial-verifications`.

## 5. Validación final

- [x] 5.1 Ejecutar `gradlew.bat test --no-daemon`. Verificación: todas las pruebas pasan. Evidencia: `gradlew.bat test --no-daemon` → `BUILD SUCCESSFUL` con `QUARKUS_HTTP_TEST_PORT=19081`.
- [x] 5.2 Ejecutar `gradlew.bat build --no-daemon`. Verificación: build exitoso. Evidencia: `gradlew.bat build --no-daemon` → `BUILD SUCCESSFUL`.
- [x] 5.3 Ejecutar `openspec validate validacion-facial-identidad --strict`. Verificación: cambio sin errores de validación. Evidencia: `openspec validate validacion-facial-identidad --type change --strict --json` → `valid: true`, `issues: []`.

## Límite de integración

El repositorio actualmente no implementa los endpoints de registro de passkey ni de verificación de celular descritos en `ENDPOINTS.md`. Este cambio no los implementa; las pruebas del paso facial usan cuentas de prueba en el estado posterior a passkey. El flujo HTTP completo dependerá de esa integración preexistente o de un cambio separado.