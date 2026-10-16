# Design

Referencias: [proposal.md](./proposal.md), [Issue #5](https://github.com/user-bcp-student49/wallet-backend-api/issues/5), [ENDPOINTS.md](../../../ENDPOINTS.md) y [openapi.yaml](../../../openapi.yaml).

## Estado

Diseño aprobado para una simulación de demo. No realiza ni representa KYC real; cualquier captura o evaluación facial real sigue bloqueada hasta contar con aprobación de Producto y Cumplimiento.

## Contexto técnico

- El backend es una aplicación Quarkus. El recurso implementado `GoogleAuthResource` expone `POST /v1/auth/google`; también existe `/hello`.
- `AuthSessionService` resuelve la identidad, crea o recupera `Account` y copia su estado actual a la sesión.
- `Account` conserva `onboardingStatus` y `nextAction` como texto. Para una cuenta nueva, los valores actuales son `BIOMETRIC_PENDING` y `REGISTER_BIOMETRIC`.
- `AuthSessionResponse.Onboarding` expone `status`, `nextAction` y `phoneNumberMasked`. Las pruebas de servicio y de recurso protegen la respuesta inicial de Google.
- `ENDPOINTS.md` y `openapi.yaml` describen passkey y celular, pero esos endpoints no están implementados actualmente en recursos del backend.
- La passkey usa verificación local del autenticador WebAuthn. No demuestra identidad KYC y el rostro o su plantilla no se envían a esta API.

## Goals

1. Introducir un paso simulado distinto del registro de passkey, solo para demo.
2. Hacer que el backend sea la autoridad del resultado configurado y del avance al celular.
3. Preservar la respuesta actual de `POST /v1/auth/google` y los significados de `biometricRegistered`, `BIOMETRIC_PENDING` y `REGISTER_BIOMETRIC`.
4. Impedir que `NOT_APPROVED` o una indisponibilidad permitan continuar o se presenten como identidad validada.
5. Mantener el alcance mínimo: sin captura biométrica, proveedor real, analítica ni almacenamiento durable.

## Non-goals

- Realizar una evaluación facial real, definir método KYC, prueba de vida, comparación documental o umbrales.
- Definir consentimiento, retención, eliminación o eventos analíticos para datos biométricos.
- Reemplazar la passkey, convertir la autenticación local del dispositivo en KYC o activar la wallet al completar solo este paso.
- Diseñar cambios de cliente: la app no está en este repositorio.

## Contratos protegidos

| Contrato | Fuente | Decisión de diseño |
|---|---|---|
| Inicio de sesión y registro | `POST /v1/auth/google` | Mantener endpoint público, request y formato de errores actuales. Una cuenta nueva sigue iniciando en `BIOMETRIC_PENDING` / `REGISTER_BIOMETRIC`. |
| Passkey | `ENDPOINTS.md`, `openapi.yaml` | Mantenerla obligatoria e independiente. `biometricRegistered` sigue representando WebAuthn, no el resultado facial simulado. |
| Celular y activación | `ENDPOINTS.md` | Insertar la simulación después de passkey y antes de celular. Solo `APPROVED` permite solicitar el código SMS. |
| Datos faciales | `ENDPOINTS.md` | El endpoint de demo no acepta ni devuelve imágenes o plantillas; no se generan eventos analíticos. |

El flujo demo es `Google → passkey → simulación facial → celular → COMPLETED`. La respuesta inicial de Google permanece sin cambios. La respuesta documentada al completar passkey cambia a `FACIAL_VERIFICATION_PENDING` / `VERIFY_FACIAL_IDENTITY`.

## Componentes afectados

| Componente | Cambio previsto cuando se levante el bloqueo |
|---|---|
| `Account` | Representar el estado facial de demo separado de `biometricRegistered`, conservando el almacenamiento en memoria. |
| Servicio de onboarding | Aplicar transiciones deterministas y evitar que el celular se solicite antes de `APPROVED`. |
| `AuthSessionResponse.Onboarding` | Mantener los campos actuales y añadir solo estados/acciones de demo definidos en la spec. |
| Recurso facial | Añadir `POST /v1/onboarding/facial-verifications`, protegido, sin body facial y con respuesta del mock del backend. |
| `ENDPOINTS.md` y `openapi.yaml` | Actualizar juntos el paso posterior a passkey, el endpoint, sus respuestas y la configuración solo de demo. |
| Aplicación cliente | Consumir el paso, instrucciones y acciones aprobadas; queda fuera de este repositorio y de esta implementación. |

El proveedor mock devuelve un resultado determinista a partir de una propiedad de configuración de demo (`APPROVED`, `NOT_APPROVED` o `UNAVAILABLE`). El cliente no puede indicar el resultado. La implementación del mock queda limitada a perfiles `dev` y `test`, siguiendo el patrón de `MockGoogleIdentityProvider`.

## Decisiones técnicas y alternativas

### D1. Passkey y simulación facial son conceptos separados

- **Elegido:** mantener `biometricRegistered` y `BIOMETRIC_PENDING` asociados exclusivamente al paso WebAuthn. La finalización de passkey conduce al estado demo `FACIAL_VERIFICATION_PENDING`.
- **Alternativa descartada:** reutilizar `BIOMETRIC_PENDING` para ambos pasos o marcar `biometricRegistered` según el resultado facial. Confundiría autenticación local con acreditación de identidad y rompería el contrato documentado.
- **Consecuencia:** se extiende el contrato de onboarding sin alterar la respuesta inicial de Google.

### D2. Endpoint explícito para la simulación

- **Elegido:** `POST /v1/onboarding/facial-verifications`, protegido por JWT, sin cuerpo y disponible solo con el mock de demo. Devuelve resultado simulado más el estado de onboarding actualizado.
- **Alternativa descartada:** recibir `approved` o datos faciales del cliente. El cliente no debe decidir el resultado ni enviar biometría.
- **Consecuencia:** un estado incorrecto responde `409`; `UNAVAILABLE` responde `503` con `Problem` y no cambia el onboarding.

### D3. Mantener el modelo MVP existente

- **Elegido:** extender el modelo de onboarding actual con los estados mínimos de demo y validaciones explícitas. No introducir un motor configurable ni persistencia nueva.
- **Alternativa descartada:** rediseñar todo el onboarding como una máquina de estados genérica. El alcance de una demo no justifica esa complejidad.
- **Consecuencia:** el resultado se pierde al reiniciar, aceptable para esta demo.

### D4. Minimizar el resultado que cruza el límite del backend

- **Elegido:** transportar únicamente el resultado simulado; no aceptar ni almacenar imágenes, plantillas ni atributos biométricos.
- **Alternativa descartada:** registrar material facial o eventos detallados por conveniencia de depuración.
- **Consecuencia:** errores y logs deben evitar datos biométricos y secretos; la forma concreta del resultado queda pendiente de la revisión de privacidad y seguridad.

## Estrategia incremental

1. Añadir pruebas de caracterización del contrato actual de Google y ejecutar la suite antes del cambio funcional.
2. Escribir pruebas unitarias de resultados/transiciones del mock y pruebas HTTP del endpoint protegido antes de implementar el comportamiento.
3. Implementar proveedor de demo, transición de onboarding y recurso HTTP en incrementos pequeños.
4. Actualizar `ENDPOINTS.md` y `openapi.yaml` juntos y validar que describan los mismos estados y respuestas.
5. Ejecutar suite/build y OpenSpec; comprobar que perfiles productivos no acepten el mock ni expongan la demo como KYC real.

## Estrategia de pruebas

- Mantener verdes `AuthSessionServiceTest` y `GoogleAuthResourceTest`, especialmente la respuesta pública de Google y el estado inicial de passkey.
- Probar `APPROVED`, `NOT_APPROVED` e `UNAVAILABLE`; solo el primero cambia a `PHONE_PENDING`.
- Probar que el endpoint requiere JWT, solo acepta el estado posterior a passkey y no permite saltar el paso.
- Probar `409` para estado incorrecto y `503 application/problem+json` para mock no disponible.
- Verificar que la respuesta no contiene datos faciales y que el mock no se carga en perfiles de producción.
- Validar que `ENDPOINTS.md` y `openapi.yaml` describan el mismo flujo.

## Riesgos y trade-offs

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Simulación confundida con KYC real | Demo puede crear una afirmación falsa de identidad verificada | Marcar modo demo y no exponerlo en perfiles productivos. |
| Reutilizar estados de passkey | Se puede confundir WebAuthn con validación facial | Mantener estados e indicador separados. |
| Resultado almacenado solo en memoria | Se pierde al reiniciar | Aceptado para demo; no añadir persistencia. |
| Contrato no sincronizado | Cliente puede saltar o no reconocer el paso | Actualizar y validar OpenAPI y `ENDPOINTS.md` en la misma tarea. |

## Fuera de alcance que sigue bloqueado

- Validación facial real, proveedor biométrico, consentimiento y umbrales KYC.
- Captura, transferencia, almacenamiento y retención de información facial.
- Analítica del paso y revisión manual.

## Rollback

Revertir el endpoint y los estados de demo, y restaurar en la documentación el avance passkey → celular. No hay datos faciales que migrar o borrar; el estado de cuentas es en memoria.