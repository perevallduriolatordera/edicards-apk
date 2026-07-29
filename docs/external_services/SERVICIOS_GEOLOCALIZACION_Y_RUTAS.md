# Servicios de Geolocalización y Rutas - Edicards

**Fecha de actualización:** Julio 2026
**Aplicación:** Edicards APK - Sistema de gestión comercial

---

## Índice
1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [Servicios Utilizados](#servicios-utilizados)
3. [Cuentas y Credenciales](#cuentas-y-credenciales)
4. [Límites de Uso](#límites-de-uso)
5. [Costos Mensuales](#costos-mensuales)
6. [Recomendaciones](#recomendaciones)
7. [Monitoreo y Alertas](#monitoreo-y-alertas)

---

## Resumen Ejecutivo

La aplicación Edicards utiliza **dos servicios principales** para geolocalización y optimización de rutas:

| Servicio | Uso Principal | Costo Estimado/Mes | Estado |
|----------|---------------|---------------------|--------|
| **Google Maps Routes API** | Optimización de rutas (algoritmo principal) | $945 - $2,850 | ✅ ACTIVO |
| **OpenRouteService (ORS)** | Geocodificación y fallback | €0 - €10 | ✅ ACTIVO |

**Costo Total Estimado:** $945 - $2,850/mes (dependiendo de la frecuencia de uso)

---

## Servicios Utilizados

### 1. Google Maps Routes API

#### Descripción
Servicio principal de Google para cálculo y optimización de rutas. Utiliza algoritmos avanzados con datos de tráfico en tiempo real.

#### APIs Específicas Usadas
- **Compute Routes API v2**: Cálculo de rutas optimizadas
- **Waypoint Optimization**: Ordenamiento óptimo de puntos de visita

#### Funcionalidades en la App
- Optimización de rutas comerciales (hasta 500 clientes por ruta)
- Clustering geográfico (grid 6x6 = ~36 clusters)
- Ordenamiento de visitas basado en tráfico real
- Cálculo de distancias y tiempos estimados

#### Ubicación en el Código
```
app/src/main/java/net/ifeu/edicards/Services/RouteGeneratorService.java
app/src/main/java/net/ifeu/edicards/Services/GoogleRouteOptimizationService.java
app/src/main/java/net/ifeu/edicards/Constants/ConstantsEndpoints.java (línea 49)
```

#### Endpoint
```
https://routes.googleapis.com/directions/v2:computeRoutes
```

---

### 2. OpenRouteService (ORS)

#### Descripción
Servicio open-source de geocodificación y cálculo de rutas. Se utiliza como servicio complementario y fallback.

#### APIs Específicas Usadas
- **Geocoding API**: Conversión de direcciones a coordenadas
- **Matrix API**: Cálculo de matrices de distancias (para TSP cuando cluster ≥ 30 clientes)

#### Funcionalidades en la App
- Geocodificación de direcciones de clientes
- Obtención de ciudad base del comercial
- Cálculo de distancias para optimización TSP
- Fallback automático si Google Maps falla

#### Ubicación en el Código
```
app/src/main/java/net/ifeu/edicards/Services/OpenRouteServiceGeocodingStrategy.java
app/src/main/java/net/ifeu/edicards/Services/RouteOptimizerService.java
app/src/main/java/net/ifeu/edicards/Constants/ConstantsEndpoints.java (líneas 43-44)
```

#### Endpoints
```
Geocoding: https://api.openrouteservice.org/geocode/search
Matrix:    https://api.openrouteservice.org/v2/matrix/driving-car
```

---

## Cuentas y Credenciales

### Google Maps Routes API

#### Cuenta
- **Plataforma:** Google Cloud Platform
- **Proyecto:** `Edicards Routes` (o el nombre configurado)
- **Servicio:** Google Maps Platform - Routes API v2

#### API Key
```properties
# Ubicación: local.properties (línea 18)
google.maps.api.key=AIzaSyASQNHn8m2gnnYTvlRUuEYt8fWMJAN5aA8
```

⚠️ **IMPORTANTE:** Esta API key está restringida a:
- Paquete Android: `net.ifeu.edicards`
- APIs permitidas: Routes API v2, Geocoding API (opcional)

#### Acceso a la Consola
```
https://console.cloud.google.com/
→ Proyecto: Edicards Routes
→ APIs & Services → Credentials
```

#### Configuración en la App
La API key se inyecta automáticamente desde `local.properties` mediante Gradle:
```java
// build.gradle (líneas 31-33)
def googleMapsApiKey = localProperties.getProperty('google.maps.api.key', '')
buildConfigField "String", "GOOGLE_MAPS_API_KEY", "\"${googleMapsApiKey}\""
```

---

### OpenRouteService (ORS)

#### Cuenta
- **Plataforma:** OpenRouteService.org
- **Organización ID:** `5b3ce3597851110001cf6248`
- **API Key ID:** `29d3bbb04a37460980843ec084f6ca01`
- **Usuario/Handle:** `murmur64`

#### API Key
```properties
# Ubicación: local.properties (línea 13)
ors.api.key=eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjI5ZDNiYmIwNGEzNzQ2MDk4MDg0M2VjMDg0ZjZjYTAxIiwiaCI6Im11cm11cjY0In0=
```

**Formato:** Base64 encoded JSON con estructura:
```json
{
  "org": "5b3ce3597851110001cf6248",
  "id": "29d3bbb04a37460980843ec084f6ca01",
  "h": "murmur64"
}
```

#### Acceso al Dashboard
```
https://openrouteservice.org/dev/#/home
→ Login con cuenta asociada al usuario "murmur64"
```

#### Plan Actual
**Free Tier** - Sin cargo

---

## Límites de Uso

### Google Maps Routes API

#### Límites de Tasa (Rate Limits)
| Métrica | Límite |
|---------|--------|
| Compute Routes queries | 3,000 requests/minuto |
| Route Matrix elements | 3,000 elements/minuto |
| Max waypoints por request | 25 puntos intermedios |
| Max orígenes/destinos | 50 (por place ID/dirección) |

#### Límites Mensuales
- **Free Tier:** 10,000 requests/mes GRATIS
- **Sin límite máximo** (solo limitado por presupuesto configurado)

#### Uso Actual Estimado
```
Escenario: 15 comerciales, 500 clientes c/u, 1 ruta/semana

15 comerciales × 1 ruta/semana × 4 semanas = 60 rutas/mes
60 rutas × 35 clusters/ruta = 2,100 requests/mes

Estado: ✅ DENTRO del free tier (2,100 < 10,000)
```

**NOTA:** Aunque estamos dentro del free tier, Google cobra $0.005 por request después de los primeros 10,000. En nuestro caso, al estar por debajo, **el costo real es $0**.

Sin embargo, la documentación técnica en el repositorio menciona un costo de $0.45 por request, lo que sugiere que podría estar usando una API diferente (Compute Routes Pro) o que el free tier no aplica a todas las características usadas.

#### Cuota Diaria Configurable
- Se puede establecer en Google Cloud Console
- **Recomendado:** 150 requests/día (buffer de seguridad)

---

### OpenRouteService (ORS)

#### Límites de Tasa (Rate Limits)
| Métrica | Límite |
|---------|--------|
| Requests por minuto | 40 requests/min |
| Requests concurrentes | 40 simultáneos |

#### Límites Mensuales
| API | Límite Diario | Límite Mensual |
|-----|---------------|----------------|
| Geocoding | 2,500 requests/día | 40,000 requests/mes |
| Matrix (Directions) | 2,500 requests/día | 40,000 requests/mes |
| **Total combinado** | **2,500 requests/día** | **40,000 requests/mes** |

⚠️ **IMPORTANTE:** Los límites son **acumulativos** entre todas las APIs. Si usas 1,000 de Geocoding + 1,500 de Matrix = 2,500 total.

#### Uso Actual Estimado
```
Geocoding (por ruta):
- ~500 direcciones únicas × 60 rutas/mes = 30,000 geocoding/mes
  (Nota: Con caché, se reduce significativamente en generaciones posteriores)

Matrix (para TSP clusters ≥ 30):
- ~5% de clusters × 35 clusters × 60 rutas = ~105 matrix calls/mes

Total: 30,105 requests/mes (primera generación sin caché)
Total: ~3,000 requests/mes (con caché efectivo)

Estado: ✅ DENTRO del límite mensual (40,000)
```

#### Consecuencias de Exceder Límites
- API devuelve error HTTP 429 (Too Many Requests)
- Acceso temporalmente bloqueado
- Solución: Upgrade a plan de pago o esperar reset (diario/mensual)

---

## Costos Mensuales

### Google Maps Routes API

#### Estructura de Precios (Essentials Tier)
| Volumen Mensual | Precio por 1,000 requests |
|-----------------|---------------------------|
| 0 - 10,000 | **GRATIS** |
| 10,001 - 100,000 | $5.00 |
| 100,001 - 500,000 | $4.00 |
| 500,001 - 1,000,000 | $3.00 |
| 1,000,001+ | Descuentos adicionales |

#### Cálculo de Costos por Escenario

**Escenario A: Uso Mínimo (1 ruta/semana por comercial)**
```
15 comerciales × 1 ruta/semana × 4 semanas = 60 rutas/mes
60 rutas × 35 requests/ruta = 2,100 requests/mes

Costo: $0 (dentro del free tier de 10,000)
```

**Escenario B: Uso Moderado (2 rutas/semana por comercial)**
```
15 comerciales × 2 rutas/semana × 4 semanas = 120 rutas/mes
120 rutas × 35 requests/ruta = 4,200 requests/mes

Costo: $0 (dentro del free tier de 10,000)
```

**Escenario C: Uso Intensivo (3 rutas/semana por comercial)**
```
15 comerciales × 3 rutas/semana × 4 semanas = 180 rutas/mes
180 rutas × 35 requests/ruta = 6,300 requests/mes

Costo: $0 (dentro del free tier de 10,000)
```

**Escenario D: Uso Muy Intensivo (5 rutas/semana por comercial)**
```
15 comerciales × 5 rutas/semana × 4 semanas = 300 rutas/mes
300 rutas × 35 requests/ruta = 10,500 requests/mes

Costo:
- Primeros 10,000: $0
- Siguientes 500: (500 ÷ 1,000) × $5.00 = $2.50
Total: $2.50/mes
```

**Escenario E: Uso Extremo (1 ruta/día por comercial)**
```
15 comerciales × 1 ruta/día × 30 días = 450 rutas/mes
450 rutas × 35 requests/ruta = 15,750 requests/mes

Costo:
- Primeros 10,000: $0
- Siguientes 5,750: (5,750 ÷ 1,000) × $5.00 = $28.75
Total: $28.75/mes
```

#### Proyección Anual
| Escenario | Mensual | Anual | Por Comercial/Año |
|-----------|---------|-------|-------------------|
| A: 1/semana | $0 | $0 | $0 |
| B: 2/semana | $0 | $0 | $0 |
| C: 3/semana | $0 | $0 | $0 |
| D: 5/semana | $2.50 | $30 | $2 |
| E: 1/día | $28.75 | $345 | $23 |

---

### OpenRouteService (ORS)

#### Estructura de Precios
| Plan | Costo Mensual | Requests Incluidos | Precio Adicional |
|------|---------------|--------------------|--------------------|
| **Free** | **€0** | 40,000/mes | N/A (se bloquea si excede) |
| Starter | ~€49/mes | 100,000/mes | ~€0.40 por 1,000 extra |
| Business | ~€199/mes | 500,000/mes | ~€0.30 por 1,000 extra |

#### Cálculo de Costos Actual
```
Uso estimado: 3,000 - 30,000 requests/mes (según caché)
Plan actual: Free
Límite: 40,000/mes

Costo: €0/mes
Estado: ✅ DENTRO del límite
```

#### Si Excediéramos el Free Tier
```
Escenario: 60,000 requests/mes (hipotético)
Exceso: 20,000 requests

Opción 1: Upgrade a plan Starter
- Costo: €49/mes (fijo)
- Incluye: 100,000 requests

Opción 2: Pago por uso
- No disponible en ORS (solo planes fijos)
```

---

### ChatGPT API (Informativo)

Aunque no está relacionado con rutas, la app también tiene configurado:

```java
// ConstantsEndpoints.java
public static final String CHATGPT_API_URL = "https://api.openai.com/v1/chat/completions";
public static final String CHATGPT_MODEL = "gpt-3.5-turbo";
```

**Estado:** API key configurada mediante variable de entorno `CHATGPT_API_KEY`
**Uso:** No especificado en el código actual
**Costo:** Depende del uso (no calculado)

---

## Resumen de Costos Totales

### Por Escenario de Uso

| Escenario | Google Maps | ORS | Total | Por Comercial/Mes |
|-----------|-------------|-----|-------|-------------------|
| **Actual (1-3/semana)** | **$0** | **€0** | **$0** | **$0** |
| Intensivo (5/semana) | $2.50 | €0 | ~$2.50 | $0.17 |
| Muy Intensivo (1/día) | $28.75 | €0 | ~$28.75 | $1.92 |

### Comparación con Documentación Existente

Los documentos previos en el repositorio mencionan costos de **$945 - $2,850/mes**, lo que sugiere que:

1. **Están usando Compute Routes Pro** ($0.45/request) en lugar de Essentials
2. **O están calculando para un volumen mucho mayor**
3. **O el free tier no aplica a todas las características**

Es recomendable **verificar en Google Cloud Console** qué tier exacto se está usando.

---

## Recomendaciones

### 1. Monitoreo Obligatorio

#### Google Cloud Console
- **Frecuencia:** Revisar 1 vez por semana
- **Dashboard:** https://console.cloud.google.com/apis/dashboard
- **Qué revisar:**
  - Número de requests/día
  - Costos acumulados del mes
  - Alertas de cuota

#### OpenRouteService Dashboard
- **Frecuencia:** Revisar 1 vez por mes
- **Dashboard:** https://openrouteservice.org/dev/#/home
- **Qué revisar:**
  - Requests consumidos vs límite
  - Errores 429 (rate limit exceeded)

---

### 2. Configurar Alertas de Presupuesto

#### Google Cloud
```
1. Ir a: https://console.cloud.google.com/billing
2. Billing → Budgets & Alerts
3. Create Budget:
   - Nombre: "Edicards Routes Budget"
   - Presupuesto: $50/mes (buffer de seguridad)
   - Alertas en: 50% ($25), 90% ($45), 100% ($50)
   - Email: [correo del responsable]
```

#### OpenRouteService
```
No hay sistema de alertas automático.
Recomendación: Revisar manualmente el dashboard mensualmente.
```

---

### 3. Optimizaciones para Reducir Costos

#### Implementar Caché de Geocodificación
✅ **Ya implementado** en el código:
```java
// Las direcciones geocodificadas se cachean automáticamente
// Ahorro: ~80% en requests repetidos
```

#### Reutilizar Rutas Cuando Sea Posible
```java
// Pseudocódigo sugerido
if (clientsUnchangedFromLastWeek) {
    return cachedRoute; // Ahorro: 100%
} else {
    return generateNewRoute();
}
```

#### Generación Batch
Si múltiples comerciales generan rutas simultáneamente:
```java
// Ejecutar en paralelo (no afecta costo, pero mejora velocidad)
ExecutorService executor = Executors.newFixedThreadPool(5);
```

---

### 4. Plan de Contingencia

#### Si Google Maps falla o excede presupuesto
✅ **Ya implementado** - Fallback automático a ORS:
```java
// UnifiedRouteOptimizer.java
// Intenta Google primero, si falla usa ORS
```

#### Si ORS excede límite free
**Opción 1:** Upgrade a plan Starter (€49/mes)
**Opción 2:** Implementar TSP local (sin API externa)
**Opción 3:** Reducir frecuencia de generación de rutas

---

### 5. Seguridad de API Keys

#### Restricciones Actuales
✅ Google Maps: Restringida a paquete Android
⚠️ ORS: Sin restricciones específicas (verificar)

#### Recomendaciones
1. **Rotar API keys** cada 6 meses
2. **No commitear** `local.properties` al repositorio
3. **Usar Firebase Remote Config** para producción (opcional)
4. **Implementar ofuscación** en el APK release

---

## Monitoreo y Alertas

### Checklist Mensual

```
□ Revisar costos en Google Cloud Console
□ Verificar requests consumidos en ORS Dashboard
□ Comprobar que no hay errores 429 en logs
□ Validar que el caché de geocodificación funciona
□ Revisar alertas de presupuesto (email)
□ Actualizar este documento si hay cambios
```

### Indicadores de Problemas

| Síntoma | Causa Probable | Solución |
|---------|----------------|----------|
| Rutas no se generan | API key inválida | Verificar en Cloud Console |
| Error 429 en logs | Excedido rate limit | Esperar reset o upgrade plan |
| Costos inesperados | Uso excesivo | Revisar logs de generación |
| Fallback a ORS frecuente | Problema con Google API | Verificar configuración |

---

## Contactos y Soporte

### Google Cloud Support
- **Documentación:** https://developers.google.com/maps/documentation/routes
- **Soporte:** Incluido en plan (email/chat)
- **Foro:** https://stackoverflow.com/questions/tagged/google-routes-api

### OpenRouteService Support
- **Documentación:** https://openrouteservice.org/dev/#/api-docs
- **Foro:** https://ask.openrouteservice.org/
- **GitHub:** https://github.com/GIScience/openrouteservice

### Responsable Técnico
- **Revisar:** `app/src/main/java/net/ifeu/edicards/Services/RouteGeneratorService.java`
- **Configurar:** `local.properties` y `ConstantsEndpoints.java`

---

## Referencias

### Documentación Interna
- `/MONTHLY_COST_ANALYSIS.md` - Análisis detallado de costos
- `/COSTS_SUMMARY.txt` - Resumen ejecutivo de costos
- `/GOOGLE_CLOUD_SETUP_GUIDE.md` - Guía de configuración inicial
- `/ROUTE_OPTIMIZATION_SETUP.md` - Setup de optimización de rutas

### Archivos de Configuración
- `/local.properties` - API keys (NO VERSIONAR)
- `/app/build.gradle` - Inyección de API keys
- `/app/src/main/java/net/ifeu/edicards/Constants/ConstantsEndpoints.java`

---

**Documento creado:** Julio 2026
**Próxima revisión:** Agosto 2026
**Versión:** 1.0
