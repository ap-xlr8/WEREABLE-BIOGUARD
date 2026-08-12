# Informe DevSecOps — WEREABLE-BIOGUARD (Android Wear OS)

**Fecha:** 2026-08-10
**Repo:** `ap-xlr8/WEREABLE-BIOGUARD` · **Rama:** `main`
**Stack:** Android Wear OS / Kotlin, Compose, Room + SQLCipher, Wear Data Layer

---

## 1. Exposición de información sensible

**Veredicto: NO se encontraron credenciales reales** en archivos versionados ni en el historial git.

| Elemento | Estado |
|---|---|
| Claves API / Firebase / FCM | ✅ Ninguna (`google-services.json` no existe; sin dependencias Firebase) |
| `keystore.properties` | ✅ Ignorado; solo `keystore.properties.example` con **placeholders** |
| Contraseñas de keystore | ✅ Vienen de env vars (`BIOGUARD_*`) o `keystore.properties` local; sin hardcodeo; build release falla si faltan |
| `local.properties` | ✅ Ignorado (solo `sdk.dir`) |
| Tokens en el reloj | ✅ No se guarda JWT en el watch; identificadores y nonce cifrados AES-GCM (Android Keystore) |
| `.gitignore` | OK — cubre `*.keystore`, `*.jks`, `*.p12`, `*.apk`, `*.aab`, `keystore.properties`, `local.properties` |

**Notas:**
- `CONTEXT_APP.md:128` — URL del backend móvil en documentación (info-disclosure menor si el repo fuera público).
- `.idea/` commiteado (higiene, no es secreto).

---

## 2. CI/CD — DevSecOps

### `.github/workflows/ci.yml`
- Job `test`: unit tests, `lintDebug`, SBOM CycloneDX, subida de reportes.
- Job `build` (solo push a main): restaura keystore desde `secrets.BIOGUARD_KEYSTORE_BASE64`, firma con env vars, verifica con `apksigner`, genera `SHA256SUMS.txt`, sube APK/AAB.

### `.github/workflows/security.yml`
- **CodeQL** (java-kotlin), **Gitleaks** (fetch-depth 0), **dependency-review** (PR, fail high), **Trivy** sobre SBOM (HIGH/CRITICAL, exit-code 1). Semanal + dispatch. Acciones pinneadas por SHA.

**Ausencias:** sin deploy/publish automatizado (distribución por artefactos firmados, documentado); sin Dependabot/Renovate.

---

## 3. Configuración de seguridad Wear OS

- `usesCleartextTraffic="false"` ✅
- `allowBackup="false"` + reglas que excluyen todo del backup. ✅
- **Sin permiso `INTERNET`** — el watch no habla HTTP; la telemetría viaja por Wear Data Layer al teléfono. Superficie de red nula. ✅
- `minSdk=30`, `targetSdk=36`, `compileSdk 36`.
- **R8/ProGuard activado** (`isMinifyEnabled=true`, `isShrinkResources=true`).
- Firma release solo por env/keystore local; `check()` impide construir sin firma. ✅
- Wrapper Gradle fijado con `distributionSha256Sum`. ✅

**Componentes exportados:**
- `PairingListenerService` `exported="true"` — mitigado en código (`isNearby`, límite 1 KB, protocolo v2, nonce con antigüedad máx. 300 s, comparación constante).
  - ⚠️ La ruta `auto-pair` **salta la validación de nonce** (`PairingListenerService.kt:35`) — evaluar si es aceptable.
- `BioGuardPassiveListenerService` / `BioGuardComplicationService` protegidos por permisos de binding. ✅
- `BioGuardSensorService` `exported="false"`. ✅
- ⚠️ `MainActivity.kt:110` solicita `READ_BODY_TEMPERATURE` (API 36+) pero **no está declarado** en el manifest.

---

## 4. Manejo de datos

**Bien implementado:**
- Emparejamiento por QR con **firma ECDSA P-256** (`SHA256withECDSA`), nonce de un solo uso, clave de identidad en Android Keystore; `trustedNodeId` cifrado.
- Comandos entrantes solo si `sourceNodeId == trustedNodeId`, allowlist de rutas, tope de 8 KB, validación de rangos.
- Telemetría (BPM, temp, GSR, HRV, SpO2, glucosa estimada) por **Wear Data Layer** al móvil con cola persistente durable en Room cifrado + reintentos (WorkManager).
- **Room + SQLCipher** cifrado (passphrase AES-256 en Keystore) con migración segura; DataStore cifra identificadores/nonce con AES-GCM.

**A mejorar:**
- ⚠️ Logs de datos biométricos/PII **sin `BuildConfig.DEBUG`**:
  - `SensorDataRepositoryImpl.kt:129,411` — BPM.
  - `BioGuardSensorService.kt:207` — BMI, peso, estatura (PII).
  - `BioGuardPassiveListenerService.kt:36` — pulso pasivo.
- `WearableAlertNotifier.kt:156` — notificación "AYUDA SOLICITADA" con `VISIBILITY_PUBLIC` (contexto de salud en pantalla de bloqueo).

---

## 5. Dependencias

| Dependencia | Versión | Observación |
|---|---|---|
| **Guava** | **31.1-android** (2022) | ⚠️ Desactualizada (existen 33.x/34.x) |
| **zxing** | **3.5.4** | ⚠️ Existe 3.6.0 |
| **kotlinx-coroutines** | **1.9.0** | ⚠️ Existe 1.10.x |
| AGP 9.2.1, Kotlin 2.2.10, Compose BOM 2026.06, Hilt 2.59, Room 2.8.4, WorkManager 2.11.2 | — | OK |

- Mitigación presente: SBOM + Trivy en CI (fail HIGH/CRITICAL) + dependency-review en PRs.

---

## 6. Estado de Remedición

| # | Severidad | Acción | Estado |
|---|---|---|---|
| 1 | Media | Envolver logs de datos biométricos/PII en `BuildConfig.DEBUG` | ✅ RESUELTO (`SensorDataRepositoryImpl`, `BioGuardSensorService`, `BioGuardPassiveListenerService`) |
| 2 | Media | Eliminar bypass `auto-pair` (validación de nonce) en `PairingListenerService.kt` | ✅ RESUELTO (Reto obligatorio `consumePairingChallenge`) |
| 3 | Baja | Declarar `READ_BODY_TEMPERATURE` en el manifest | ✅ RESUELTO (`AndroidManifest.xml`) |
| 4 | Baja | Notificación "AYUDA SOLICITADA": configurar `VISIBILITY_PRIVATE` | ✅ RESUELTO (`WearableAlertNotifier.kt`) |
| 5 | Baja | Añadir Dependabot | ✅ RESUELTO (`.github/dependabot.yml`) |
| 6 | Info | Sanitización de URL en documentación (`CONTEXT_APP.md`) | ✅ RESUELTO (`api.bioguard.example.com`) |

