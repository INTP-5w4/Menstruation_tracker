# Mi Ciclo — rastreador menstrual Local-First

App Android (Java, vistas XML) para registrar el ciclo menstrual con **privacidad absoluta**:
sin cuentas, sin servidores, sin analíticas. Todo vive en el teléfono; el respaldo opcional en
Google Drive se cifra en el dispositivo antes de salir.

## Funciones

| Módulo | Qué hace |
|---|---|
| **Aviso de privacidad** | Obligatorio en el primer inicio: hay que desplazarse hasta el final y marcar la casilla. "Atrás" cierra la app. Se guarda en SharedPreferences cifradas (con versión, para volver a pedirlo si cambia). |
| **Motor del ciclo** | Próxima regla, día y fase actual (menstrual, folicular, ovulación, lútea), ventana fértil, retraso, irregularidad y nivel de confianza. Promedia los últimos 6 ciclos y descarta valores atípicos. |
| **Inicio** | Anillo visual del ciclo, predicciones y avisos (medicamentos hormonales, ciclos irregulares, retraso). Registrar inicio/fin de regla con un toque. |
| **Calendario** | Vista mensual con regla registrada/prevista, ventana fértil y ovulación; historial editable de reglas. |
| **Registro diario** | Flujo, 18 síntomas, estado de ánimo, notas y relaciones sexuales (con método de protección). Sugiere marcar inicio de regla si registras flujo y avisa si hubo relación sin protección en ventana fértil. |
| **Medicamentos** | Bitácora de tomas, marca de "altera el ciclo" (automática para anticonceptivos/terapia hormonal), recordatorio diario con acción "Ya la tomé" en la notificación. |
| **Notificaciones** | WorkManager: recordatorios de medicamentos, cambio de fase, inicio de ventana fértil, regla próxima y retraso. Nunca muestran detalles en la pantalla de bloqueo; modo discreto opcional. |
| **Respaldo** | El archivo SQLite completo se cifra con AES-256-GCM y se sube a la carpeta oculta `appDataFolder` de Google Drive (manual o diario). También exportar/importar a archivo. Al conectar Drive en un teléfono nuevo, ofrece restaurar. |
| **Seguridad** | Bloqueo con huella/rostro/PIN, bloqueo de capturas de pantalla, respaldo automático de Android desactivado, borrado total de datos. |

## Arquitectura (Clean Architecture + MVVM)

```
com.example.andrs
├── domain/            Java puro, sin Android framework
│   ├── model/         Period, DailyLog, Medication, CycleStatus…
│   ├── engine/        CycleEngine (cálculo de fases y predicciones)
│   ├── repository/    Interfaces de repositorio
│   └── usecase/       GetCycleStatus, LogPeriod
├── data/
│   ├── local/         Room: entidades, DAOs, AppDatabase
│   ├── security/      SecurePrefs (EncryptedSharedPreferences), BackupCrypto (AES-256-GCM)
│   ├── sync/          DriveAuthManager (OAuth drive.appdata), DriveRestClient (REST v3)
│   └── repository/    Implementaciones
├── presentation/      Activities/Fragments + ViewModels (LiveData)
├── worker/            WorkManager: recordatorios, alertas del ciclo, respaldo
└── di/                AppContainer (inyección manual)
```

### Cifrado

- **Preferencias**: `EncryptedSharedPreferences` (AES-256-SIV/GCM), clave maestra en el Android Keystore.
- **Respaldos**: AES-256-GCM con clave derivada de una *frase de respaldo* (PBKDF2-HMAC-SHA256,
  210 000 iteraciones, sal aleatoria). Se usa una frase y no una clave del Keystore porque esta
  última queda atada al teléfono y no permitiría restaurar en otro. Si se olvida la frase, el
  respaldo no se puede recuperar.

## Configurar Google Drive (necesario para el respaldo en la nube)

La app usa la Authorization API de Google Play Services con el alcance mínimo
`https://www.googleapis.com/auth/drive.appdata` (solo ve su propia carpeta oculta).

1. En [Google Cloud Console](https://console.cloud.google.com/) crea un proyecto y habilita **Google Drive API**.
2. Configura la **pantalla de consentimiento OAuth** y añade el alcance `.../auth/drive.appdata`.
3. Crea un **ID de cliente OAuth de tipo Android** con el paquete `com.example.andrs` y la huella SHA-1
   de tu certificado de firma:
   ```bash
   ./gradlew signingReport
   ```
4. No hace falta poner el ID en el código: Google lo asocia por paquete + SHA-1.

Sin este paso la app funciona igual, solo que "Conectar Google Drive" fallará.

## Compilar y probar

```bash
./gradlew assembleDebug
```

```bash
./gradlew testDebugUnitTest
```

Las pruebas incluyen el motor del ciclo, el cifrado de respaldos, las reglas de registro y una
prueba con Robolectric que arranca la app y recorre todas las pantallas.

## Notas

- `minSdk` 26 (PBKDF2-HMAC-SHA256 nativo). `targetSdk` 37.
- Antes de publicar, cambia el `applicationId` `com.example.andrs` por uno propio (Google Play no acepta `com.example`).
- Las predicciones son estimaciones: la app no es un método anticonceptivo ni un diagnóstico médico.
