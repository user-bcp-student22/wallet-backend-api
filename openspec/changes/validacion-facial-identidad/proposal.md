# Proposal

## Why

HU-2 plantea una validación facial KYC, pero el Issue deja pendientes la aprobación y las decisiones regulatorias. Para avanzar con la demo sin presentar una simulación como KYC real, el alcance aprobado para este cambio es un resultado facial completamente simulado, sin captura ni procesamiento de rostros.

## What Changes

- Incorporar después de la passkey y antes del celular un paso de simulación facial en perfiles `dev` y `test`.
- Exponer `POST /v1/onboarding/facial-verifications`, protegido por JWT y sin datos faciales en el request; el resultado procede de una configuración de demo del backend.
- Permitir avanzar al celular únicamente con resultado simulado `APPROVED`. `NOT_APPROVED` mantiene el paso pendiente; `UNAVAILABLE` responde con `503` y no cambia el estado.
- Mantener `biometricRegistered` reservado para la passkey. El resultado de la demo no acredita identidad ni activa por sí solo la wallet.
- **BREAKING:** el contrato documentado avanza de passkey a `FACIAL_VERIFICATION_PENDING` en vez de `PHONE_PENDING`; el nuevo paso requiere aprobación antes de llamar los endpoints de celular.

La spec define exclusivamente una simulación para la demo. Cualquier validación facial real, captura biométrica o decisión KYC sigue fuera de alcance y requiere aprobación de Producto y Cumplimiento.

## Capabilities

### New Capabilities

- `facial-identity-verification`: paso simulado de demo, resultados deterministas y control del avance del onboarding; no realiza KYC real.

### Modified Capabilities

No hay capacidades publicadas en `openspec/specs/` que modificar.

## Alcance

- Proveer un mock backend determinista seleccionable por configuración, limitado a `dev` y `test`.
- Modelar `APPROVED`, `NOT_APPROVED` (incluye indeterminado en esta simulación) y `UNAVAILABLE`.
- Permitir solicitar el resultado solo después de completar la passkey; solo `APPROVED` habilita continuar al celular.
- No capturar, enviar, almacenar ni registrar imágenes o plantillas faciales; no emitir analítica para este paso.
- Mantener la activación de la wallet condicionada a completar todos los pasos del onboarding.

## Fuera de alcance

- Realizar KYC real, captura facial, prueba de vida, comparación con documento o integración con proveedor biométrico.
- Definir requisitos legales, consentimiento, umbrales de identidad, reintentos de KYC real, revisión manual, retención o eliminación de biometría.
- Reemplazar el registro obligatorio de passkey por validación facial, o convertir el reconocimiento facial local del dispositivo en prueba de identidad KYC.
- Activar la wallet por completar únicamente la validación facial.
- Implementar endpoints de passkey o celular, ni cambiar su lógica fuera de la transición documentada necesaria para insertar el paso demo.
- Cliente frontend, persistencia durable y analítica.

## Comportamiento que debe preservarse

Según [ENDPOINTS.md](../../../ENDPOINTS.md), el baseline es Google → registro obligatorio de passkey → celular → `COMPLETED` / `OPEN_HOME`. La passkey usa el autenticador del dispositivo; el rostro o su plantilla no se envían a esta API y el dispositivo puede usar otros métodos locales. La demo inserta un paso simulado entre passkey y celular; la verificación del celular continúa bloqueada hasta completarlo.

El cambio respecto al Issue es explícito: esta propuesta implementa una simulación demostrativa, no acredita identidad ni cumple KYC real. La passkey permanece intacta y `biometricRegistered` conserva su significado WebAuthn.

Los requisitos y escenarios normativos de la simulación están en [spec.md](./specs/facial-identity-verification/spec.md).

## Impact

- **Contrato de onboarding:** actualizar `ENDPOINTS.md` y `openapi.yaml` para reflejar el estado entre passkey y celular y el nuevo endpoint protegido.
- **Backend:** añadir un mock de resultado solo para `dev`/`test`; el backend no recibe datos faciales.
- **Límite:** ningún artefacto debe presentar la simulación como identidad verificada o KYC completado.
- **Estado de este cambio:** la propuesta y el diseño se limitan a la demo; la validación real sigue bloqueada por las aprobaciones de Producto y Cumplimiento.