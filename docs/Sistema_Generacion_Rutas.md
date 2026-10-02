# Sistema de Generación Automática de Rutas - Edicards

**Documento técnico-comercial**
**Fecha**: Agosto 2026
**Versión**: 1.0

---

## 1. Resumen Ejecutivo

El sistema de generación automática de rutas de Edicards optimiza las visitas comerciales mediante algoritmos avanzados de geolocalización y agrupación geográfica. El sistema reduce significativamente el tiempo de planificación y optimiza las rutas de los comerciales, mejorando la eficiencia y reduciendo costes de desplazamiento.

### Beneficios Clave

- **Ahorro de tiempo**: Generación automática de rutas en segundos
- **Optimización de costes**: Minimiza distancias y tiempo de desplazamiento
- **Flexibilidad**: Permite ajustes manuales conservando la optimización
- **Visibilidad**: Identificación clara de clientes nuevos vs. existentes en la ruta
- **Trazabilidad**: Información de última visita con código de colores
- **Exportación**: Generación de informes Excel con todos los detalles

---

## 2. Arquitectura del Sistema

### 2.1 Componentes Principales

El sistema está compuesto por **6 módulos** que trabajan coordinadamente:

```
┌─────────────────────────────────────────────────────────┐
│              1. GEOCODIFICACIÓN                          │
│  Asignación de coordenadas (lat/lng) a cada cliente     │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│              2. GESTIÓN DE ZONAS                         │
│  Asignación de clientes a zonas geográficas             │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│              3. CLUSTERING GEOGRÁFICO                    │
│  Agrupación de clientes cercanos en clusters            │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│              4. OPTIMIZACIÓN DE RUTA                     │
│  Cálculo del orden óptimo de visitas                    │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│              5. PERSISTENCIA Y AJUSTES                   │
│  Guardado y preservación de cambios manuales            │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│              6. VISUALIZACIÓN Y EXPORTACIÓN              │
│  Pantalla de rutas y generación de Excel                │
└─────────────────────────────────────────────────────────┘
```

---

## 3. Geocodificación de Clientes

### 3.1 ¿Qué es la geocodificación?

La geocodificación es el proceso de convertir una dirección postal en coordenadas geográficas (latitud y longitud). Por ejemplo:

```
Dirección: Carrer Major 123, 08001 Barcelona
         ↓
Geocodificación
         ↓
Coordenadas: Lat 41.3851, Lng 2.1734
```

### 3.2 Estrategias de Geocodificación

El sistema utiliza **dos servicios** de geocodificación en cascada:

#### Estrategia 1: OpenRouteService (Primaria)
- **Gratuita** hasta 2000 peticiones/día
- Alta precisión en Europa
- Datos de OpenStreetMap
- Utilizada por defecto

#### Estrategia 2: Google Maps Geocoding API (Fallback)
- Se utiliza si OpenRouteService falla
- Mayor cobertura global
- Coste por petición (a partir de cierto volumen)
- Backup de seguridad

### 3.3 Proceso de Asignación de Coordenadas

```
1. Cliente sin coordenadas detectado
   ↓
2. Construir dirección completa:
   - Dirección + Población + Código Postal + Provincia
   ↓
3. Intentar geocodificación con OpenRouteService
   ↓
4. Si falla → Intentar con Google Maps
   ↓
5. Si ambos fallan → Cliente queda sin coordenadas
   ↓
6. Guardar coordenadas en base de datos
```

### 3.4 Validación de Coordenadas

El sistema aplica **filtros de validación** para evitar coordenadas erróneas:

- **Coordenadas nulas**: Se descartan clientes sin lat/lng
- **Coordenadas en (0,0)**: Se consideran inválidas
- **Distancia máxima desde base**: Clientes a más de 1000 km se marcan como "lejanos"
- **Coordenadas duplicadas**: Se deduplicanclientes con mismo código

**Resultado**: Solo se incluyen en la ruta clientes con coordenadas válidas y razonables.

### 3.5 Gestión de Clientes sin Coordenadas

Los clientes que NO pueden geocodificarse se muestran en un **diálogo de estados** que indica:

- **Total de clientes activos**
- **Clientes con coordenadas válidas**
- **Clientes sin coordenadas** (con listado de nombres)
- **Clientes lejanos** (fuera del radio de 1000 km)
- **Clientes sin zona asignada**

El comercial puede revisar estos casos y:
- Corregir direcciones manualmente
- Asignar zonas
- Marcar clientes como inactivos si corresponde

---

## 4. Gestión de Zonas

### 4.1 Concepto de Zona

Una **zona** es un área geográfica definida por el comercial donde agrupa clientes cercanos. Ejemplos:

- "Barcelona Centro"
- "Hospitalet"
- "Sabadell"
- "Terrassa"

### 4.2 Asignación de Zonas a Clientes

#### Método 1: Asignación Manual
El comercial puede asignar zonas uno a uno desde la ficha del cliente.

#### Método 2: Asignación Masiva
Desde el diálogo "Asignar Clientes a Zona":
1. Seleccionar una zona
2. El sistema muestra clientes sin zona asignada
3. Marcar clientes y asignar en bloque

### 4.3 Importancia de las Zonas

Las zonas tienen **dos funciones críticas**:

1. **Filtrado previo**: Solo se incluyen en la ruta clientes con zona asignada
2. **Clustering mejorado**: El algoritmo agrupa mejor clientes de la misma zona

**Sin zona asignada → Cliente NO aparece en la ruta**

### 4.4 Creación de Nuevas Zonas

Desde el menú "Zonas":
1. Pulsar "Nueva Zona"
2. Introducir nombre descriptivo
3. La zona queda disponible para asignación

**Recomendación**: Crear zonas que reflejen la estructura geográfica real de la ruta del comercial.

---

## 5. Algoritmo de Clustering Geográfico

### 5.1 ¿Qué es el Clustering?

El **clustering geográfico** agrupa clientes cercanos entre sí en "clusters" o grupos. Es como dividir el mapa en "barrios" de visita.

**Ejemplo visual**:
```
Mapa de Barcelona
├── Cluster 1: Eixample (15 clientes)
├── Cluster 2: Gràcia (8 clientes)
├── Cluster 3: Sants (12 clientes)
└── Cluster 4: Sant Martí (10 clientes)
```

### 5.2 Algoritmo Utilizado: DBSCAN Modificado

El sistema utiliza una variante del algoritmo **DBSCAN** (Density-Based Spatial Clustering):

#### Parámetros del Algoritmo

- **Radio de búsqueda**: 3 km
  - Clientes a menos de 3 km entre sí se consideran del mismo cluster

- **Mínimo de clientes por cluster**: 2
  - Se necesitan al menos 2 clientes para formar un cluster

- **Puntos de ruido**: Clientes aislados
  - Si un cliente está a más de 3 km de cualquier otro, forma su propio cluster

#### Proceso de Clustering

```
1. Para cada cliente:
   │
   ├─► Buscar clientes a menos de 3 km
   │
   ├─► Si encuentra ≥ 2 clientes → Crear cluster
   │
   └─► Si no encuentra → Cliente aislado (propio cluster)

2. Resultado: N clusters geográficos
```

### 5.3 Ventajas del Clustering

- **Reduce saltos largos**: No se salta de un extremo a otro de la ciudad
- **Agrupa zonas**: Los clusters suelen coincidir con barrios/zonas
- **Optimización local**: Dentro de cada cluster, se optimiza el orden
- **Escalabilidad**: Funciona bien con 10 o 1000 clientes

### 5.4 Ordenación de Clusters

Una vez creados los clusters, el sistema los **ordena** para minimizar distancia entre clusters:

```
Base (punto de inicio)
  ↓
Cluster más cercano a la base
  ↓
Cluster más cercano al anterior
  ↓
... (continúa)
  ↓
Último cluster
  ↓
Vuelta a la base (opcional)
```

**Algoritmo**: Nearest Neighbor (vecino más cercano)

---

## 6. Optimización de Ruta

### 6.1 Servicios de Optimización Disponibles

El sistema soporta **múltiples servicios** de optimización configurables:

#### Opción 1: Google Maps Routes API (Actual)
- **Ventaja**: Alta precisión, considera tráfico real
- **Coste**: De pago (100 optimizaciones/mes en plan base)
- **Límite**: Máximo 25 waypoints por petición
- **Uso**: Se divide la ruta en subrutas de ≤25 clientes

#### Opción 2: OpenRouteService Optimization
- **Ventaja**: Gratuita hasta 2500 peticiones/día
- **Límite**: Máximo 20 waypoints por petición
- **Precisión**: Buena en Europa
- **Configuración**: Requiere API key

#### Opción 3: Algoritmo Local (Fallback)
- **Ventaja**: 100% offline, sin límites
- **Algoritmo**: 2-opt heurístico
- **Uso**: Si fallan los servicios externos
- **Precisión**: Aceptable para <50 clientes

### 6.2 Proceso de Optimización

```
Por cada cluster:
  │
  ├─► 1. Obtener lista de clientes del cluster
  │
  ├─► 2. Dividir en subrutas de ≤25 clientes (si es necesario)
  │
  ├─► 3. Para cada subruta:
  │     │
  │     ├─► a) Construir petición al servicio de optimización
  │     │
  │     ├─► b) Enviar coordenadas + punto de inicio
  │     │
  │     ├─► c) Recibir orden optimizado
  │     │
  │     └─► d) Guardar orden de visita
  │
  └─► 4. Combinar subrutas del cluster
```

### 6.3 Configuración del Servicio

El servicio se configura en `ConstantsEndpoints.java`:

```java
// Opciones:
// - "google" → Google Maps Routes API
// - "ors" → OpenRouteService
// - "local" → Algoritmo local
public static final String ROUTE_OPTIMIZER_SERVICE = "google";
```

**Recomendación actual**: Google Maps por su precisión, con fallback a local si falla.

---

## 7. Conservación de Orden Manual

### 7.1 Problema Resuelto

Anteriormente, cada vez que se regeneraba la ruta (por sincronización o botón "Recalcular"), **se perdían** todos los ajustes manuales que hubiera hecho el comercial.

**Ahora**, el sistema **conserva el orden existente** y solo añade nuevos clientes.

### 7.2 Funcionamiento

```
Situación: Ruta existente con 50 clientes (ordenados manualmente)
         + 5 clientes nuevos sin geocodificar

Regeneración de ruta:
  │
  ├─► 1. Cargar ruta existente ANTES de borrar
  │
  ├─► 2. Guardar posiciones de los 50 clientes existentes
  │
  ├─► 3. Geocodificar los 5 clientes nuevos
  │
  ├─► 4. Optimizar posiciones de los 5 nuevos
  │
  ├─► 5. Combinar:
  │      - Clientes 1-50: Mismo orden que antes
  │      - Clientes 51-55: Orden optimizado al final
  │
  └─► Resultado: 55 clientes (50 conservados + 5 nuevos)
```

### 7.3 Identificación Visual de Clientes Nuevos

Los clientes añadidos en la última generación se marcan con un **badge verde**:

```
✨ NUEVO
```

Esto permite al comercial identificar rápidamente:
- Qué clientes son nuevos en la ruta
- Cuáles ya estaban y conservan su posición

**El badge desaparece** en la siguiente regeneración (pasan a ser "existentes").

---

## 8. Pantalla de Rutas

### 8.1 Componentes de la Interfaz

La pantalla de rutas muestra:

#### Cabecera
- **Total de clientes**: Número total en la ruta
- **Filtros**: Por zona y por fecha de última visita
- **Acciones**:
  - Recalcular ruta
  - Exportar a Excel
  - Ver estados de clientes

#### Lista de Clientes
Cada cliente muestra:
- **Orden de visita**: Número de posición (1, 2, 3...)
- **Código y nombre**: Identificación del cliente
- **Zona**: Zona asignada
- **Dirección**: Dirección completa
- **Fecha última visita**: Con código de colores
- **Badge "NUEVO"**: Si fue añadido en última generación

#### Acciones por Cliente
- **Arrastrar**: Cambiar orden manualmente (mantén pulsado)
- **Pulsación**: Ver ficha completa del cliente

### 8.2 Filtros Disponibles

#### Filtro por Zona
```
[Todas las zonas ▼]
  ├─ Todas las zonas
  ├─ Barcelona Centro (15 clientes)
  ├─ Hospitalet (8 clientes)
  └─ Sabadell (12 clientes)
```

#### Filtro por Última Visita
```
[Todos ▼]
  ├─ Todos
  ├─ Sin visitas
  ├─ >30 días
  ├─ 15-30 días
  └─ <15 días
```

Los filtros son **acumulativos**: Se pueden combinar zona + fecha.

### 8.3 Reordenación Manual

El comercial puede **cambiar el orden** arrastrando clientes:

```
Antes:
1. Cliente A
2. Cliente B  ← Arrastrar hacia arriba
3. Cliente C

Después:
1. Cliente B
2. Cliente A
3. Cliente C
```

**El cambio se guarda automáticamente** y se conserva en futuras regeneraciones.

---

## 9. Exportación a Excel

### 9.1 Contenido del Archivo

El archivo Excel generado incluye **9 columnas**:

| Columna | Descripción | Ejemplo |
|---------|-------------|---------|
| Orden | Posición en la ruta | 1, 2, 3... |
| Código | Código del cliente | C0001 |
| Nombre | Nombre comercial | Bar Pepito |
| Zona | Zona asignada | Barcelona Centro |
| Dirección | Dirección completa | Carrer Major 123 |
| Población | Ciudad | Barcelona |
| CP | Código postal | 08001 |
| Teléfono | Teléfono de contacto | 931234567 |
| Última Visita | Fecha última visita + color | 15/08/2026 |

### 9.2 Código de Colores de Última Visita

El sistema aplica **colores** según antigüedad de la visita:

| Color | Condición | Significado |
|-------|-----------|-------------|
| 🟢 Verde | ≤ 7 días | Visitado recientemente |
| 🟡 Amarillo | 8-15 días | Visita reciente |
| 🟠 Naranja | 16-30 días | Requiere atención pronto |
| 🔴 Rojo | > 30 días | Requiere visita urgente |
| ⚫ Gris | Sin visitas | Nunca visitado |

**Utilidad**: El comercial puede priorizar clientes según el color.

### 9.3 Ordenación en Excel

El Excel **respeta el orden exacto** de la pantalla de rutas:

- Si hay filtros activos → Se exportan solo los filtrados
- El orden de las filas = orden de visita
- Los colores se aplican automáticamente

**Ubicación del archivo**: `/sdcard/Edicards/Rutas/ruta_[fecha].xls`

---

## 10. Estados de Clientes

### 10.1 Diálogo de Estados

El botón "Estados" muestra un **informe detallado**:

```
╔═══════════════════════════════════════════╗
║  ESTADO DE CLIENTES - GENERACIÓN DE RUTAS ║
╠═══════════════════════════════════════════╣
║                                            ║
║  📊 Total clientes activos: 150            ║
║                                            ║
║  ✅ Clientes con coordenadas: 120          ║
║  ❌ Clientes sin coordenadas: 25           ║
║  🌍 Clientes lejanos (>1000km): 3          ║
║  ⚠️  Clientes sin zona: 2                  ║
║                                            ║
║  🗺️  Clientes en ruta final: 118           ║
║                                            ║
╚═══════════════════════════════════════════╝

Clientes sin coordenadas:
  • Bar Manolo (no se pudo geocodificar)
  • Cafetería Sol (dirección incompleta)
  ...

Clientes sin zona:
  • Restaurante Luna
  • Tienda Estrella
```

### 10.2 Acciones Correctivas

Basándose en este informe, el comercial puede:

1. **Clientes sin coordenadas**:
   - Revisar y corregir direcciones
   - Añadir información faltante (CP, provincia)
   - Volver a geocodificar

2. **Clientes lejanos**:
   - Verificar si es un error de coordenadas
   - Marcar como inactivo si no corresponde a la zona

3. **Clientes sin zona**:
   - Asignar zona correspondiente
   - Crearla si no existe

**Regenerar ruta** después de las correcciones.

---

## 11. Flujo Completo de Trabajo

### Semana 1: Configuración Inicial

```
Lunes:
  1. Importar clientes desde central
  2. Crear zonas geográficas (Barcelona Centro, Hospitalet, etc.)
  3. Asignar zonas a clientes (masivamente o individual)
  4. Geocodificar clientes (automático en asignación)
  5. Revisar "Estados" para detectar problemas
  6. Generar primera ruta
  7. Revisar y ajustar orden manualmente si es necesario
  8. Exportar a Excel
```

### Semanas Siguientes: Mantenimiento

```
Cada lunes:
  1. Sincronización automática con central
     - Nuevos clientes se geocodifican automáticamente
     - Ruta se regenera conservando orden existente
  2. Revisar badge "✨ NUEVO" para clientes añadidos
  3. Ajustar posiciones de nuevos clientes si es necesario
  4. Exportar Excel actualizado

Durante la semana:
  - Recalcular ruta si se añaden clientes manualmente
  - Usar filtros para planificar días específicos
  - Arrastrar clientes para optimizar el orden según criterio propio
```

---

## 12. Librerías y Tecnologías Utilizadas

### 12.1 Servicios Externos

| Servicio | Función | Licencia/Coste |
|----------|---------|----------------|
| **OpenRouteService** | Geocodificación | Gratuito hasta 2000/día |
| **Google Maps Geocoding API** | Geocodificación (backup) | De pago ($ por 1000 peticiones) |
| **Google Maps Routes API** | Optimización de rutas | De pago ($ por optimización) |

### 12.2 Algoritmos Internos

| Componente | Algoritmo | Complejidad |
|-----------|-----------|-------------|
| **Clustering** | DBSCAN modificado | O(n²) |
| **Ordenación clusters** | Nearest Neighbor | O(c²) donde c = clusters |
| **Optimización local** | 2-opt heuristic | O(n²) |
| **Deduplicación** | HashSet | O(n) |

### 12.3 Librerías Java

- **Apache POI**: Generación de archivos Excel (.xls)
- **org.json**: Parseo de respuestas API
- **Android SQLite**: Base de datos local
- **Android RecyclerView**: Lista de rutas con drag&drop

---

## 13. Limitaciones y Consideraciones

### 13.1 Limitaciones Técnicas

| Aspecto | Limitación | Motivo |
|---------|-----------|---------|
| Clientes por petición | 25 (Google Routes) | Límite de la API |
| Peticiones geocodificación | 2000/día (ORS) | Plan gratuito |
| Distancia máxima | 1000 km desde base | Filtro de coordenadas erróneas |
| Radio clustering | 3 km | Optimizado para ciudades |

### 13.2 Casos Especiales

#### Clientes Rurales Dispersos
Si los clientes están muy separados (>3 km entre sí):
- El clustering crea muchos clusters pequeños
- La ruta puede tener saltos largos
- **Solución**: Ajustar manualmente o crear sub-rutas por día

#### Clientes con Direcciones Incompletas
- No se pueden geocodificar
- No aparecen en la ruta
- **Solución**: Completar direcciones en ficha del cliente

#### Cambios en Central Durante la Semana
- Los cambios no se reflejan hasta la sincronización
- **Solución**: Sincronización manual desde menú "Herramientas"

### 13.3 Costes Operativos

**Escenario típico**: 150 clientes activos

| Concepto | Frecuencia | Coste Mensual Estimado |
|----------|-----------|----------------------|
| Geocodificación inicial | Una vez | 0€ (OpenRouteService gratuito) |
| Geocodificación nuevos | 10 clientes/mes | 0€ (bajo el límite gratuito) |
| Optimización rutas | 4 veces/mes | Variable según plan Google Maps |
| Almacenamiento datos | Permanente | 0€ (local en dispositivo) |

**Nota**: Los costes de Google Maps dependen del plan contratado y volumen de uso.

---

## 14. Roadmap y Mejoras Futuras

### 14.1 Corto Plazo (3 meses)

- [ ] **Visualización en mapa**: Ver ruta en Google Maps integrado
- [ ] **Estimación de tiempos**: Tiempo estimado por visita + desplazamiento
- [ ] **Navegación GPS**: Integración con apps de navegación
- [ ] **Histórico de rutas**: Ver rutas de semanas anteriores

### 14.2 Medio Plazo (6 meses)

- [ ] **Optimización multi-día**: Generar rutas para toda la semana
- [ ] **Priorización inteligente**: Sugerir prioridades según última visita
- [ ] **Rutas dinámicas**: Reordenar según incidencias del día
- [ ] **Estadísticas**: KM recorridos, clientes visitados, etc.

### 14.3 Largo Plazo (12 meses)

- [ ] **IA predictiva**: Sugerir orden según histórico de ventas
- [ ] **Sincronización en tiempo real**: Cambios instantáneos desde central
- [ ] **Modo offline completo**: Optimización sin conexión
- [ ] **Integración calendarios**: Bloquear franjas horarias

---

## 15. Preguntas Frecuentes (FAQ)

### ¿Por qué un cliente no aparece en mi ruta?

**Posibles causas**:
1. No tiene coordenadas → Revisar dirección y geocodificar
2. No tiene zona asignada → Asignar zona
3. Está marcado como inactivo → Activar cliente
4. Está fuera del radio de 1000 km → Verificar coordenadas

**Solución**: Revisar diálogo "Estados" para identificar el problema.

---

### ¿Puedo cambiar el orden de la ruta manualmente?

**Sí**, arrastrando los clientes en la lista. Los cambios se guardan automáticamente y se conservan en futuras regeneraciones.

---

### ¿Con qué frecuencia debo regenerar la ruta?

**Recomendación**:
- Automáticamente cada lunes (sincronización)
- Manualmente si se añaden/modifican clientes durante la semana

---

### ¿Qué pasa con los clientes que ajusté manualmente cuando se regenera?

El sistema **conserva el orden** de clientes existentes. Solo se añaden nuevos clientes al final, optimizados entre ellos.

---

### ¿Puedo tener múltiples rutas?

Actualmente, el sistema gestiona **una ruta activa** por vendedor. Para múltiples rutas, se pueden usar:
- **Zonas**: Filtrar por zona para ver sub-rutas
- **Exportaciones**: Generar Excels separados por zona

---

### ¿El sistema considera el tráfico en tiempo real?

**Parcialmente**: Google Maps Routes API puede considerar tráfico histórico. Para tráfico en tiempo real, se recomienda usar navegación GPS durante la ruta.

---

### ¿Qué hago si un cliente tiene coordenadas incorrectas?

1. Editar la ficha del cliente
2. Corregir la dirección
3. Pulsar "Geocodificar Cliente" (si existe la opción)
4. O regenerar la ruta completa

Las nuevas coordenadas sustituirán a las antiguas.

---

## 16. Contacto y Soporte

Para dudas o incidencias relacionadas con el sistema de rutas:

- **Soporte Técnico**: [contacto técnico]
- **Documentación adicional**: Carpeta `/docs` en el repositorio
- **Versión del documento**: 1.0 (Agosto 2026)

---

## Anexo A: Ejemplo Práctico

### Caso: Comercial de Barcelona con 100 Clientes

**Datos iniciales**:
- 100 clientes activos en Barcelona y alrededores
- 15 sin dirección completa
- Base en Hospitalet

**Proceso**:

```
Día 1: Configuración
─────────────────────
1. Crear zonas:
   • Barcelona Centro
   • Eixample
   • Gràcia
   • Sants
   • Hospitalet

2. Asignar zonas masivamente:
   • Barcelona Centro: 25 clientes
   • Eixample: 20 clientes
   • Gràcia: 15 clientes
   • Sants: 18 clientes
   • Hospitalet: 22 clientes

3. Revisar "Estados":
   ✅ 85 clientes con coordenadas
   ❌ 15 clientes sin coordenadas

4. Corregir direcciones de los 15

5. Regenerar ruta → 85 clientes ordenados

6. Ajustar manualmente:
   • Mover cliente X antes que Y (mismo edificio)
   • Priorizar cliente Z (pedido urgente)

7. Exportar Excel para la semana

Resultado:
─────────
Ruta optimizada con 85 clientes
Ahorro estimado: 40% menos km vs. orden alfabético
Tiempo de configuración: 2 horas
```

**Semana siguiente**:
```
Sincronización lunes:
────────────────────
• 5 clientes nuevos añadidos desde central
• Se geocodifican automáticamente
• Se añaden al final de la ruta (posiciones 86-90)
• Badge "✨ NUEVO" visible
• Orden de los 85 anteriores: CONSERVADO
```

---

**FIN DEL DOCUMENTO**
