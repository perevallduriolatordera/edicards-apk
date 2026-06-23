# Test de Google Route Optimization API

## 🧪 Test Simple con cURL

Para verificar que tu API key funciona con Route Optimization API, ejecuta este comando en tu terminal:

```bash
curl -X POST \
  'https://routes.googleapis.com/v2:optimizeTours?key=AIzaSyA7hU2mIgO1lS8pOU_L1HROpSH-7TcUgnw' \
  -H 'Content-Type: application/json' \
  -H 'X-Goog-FieldMask: routes.vehicleLabel,routes.visits.shipmentLabel' \
  -d '{
    "model": {
      "shipments": [
        {
          "label": "cliente1",
          "deliveries": [{
            "arrivalLocation": {"latitude": 41.3851, "longitude": 2.1734}
          }]
        },
        {
          "label": "cliente2",
          "deliveries": [{
            "arrivalLocation": {"latitude": 41.3879, "longitude": 2.1699}
          }]
        },
        {
          "label": "cliente3",
          "deliveries": [{
            "arrivalLocation": {"latitude": 41.3828, "longitude": 2.1761}
          }]
        }
      ],
      "vehicles": [{
        "startLocation": {"latitude": 41.3874, "longitude": 2.1686},
        "endLocation": {"latitude": 41.3874, "longitude": 2.1686}
      }]
    }
  }'
```

---

## ✅ Respuesta Esperada (Éxito)

Si todo está configurado correctamente, verás una respuesta JSON como esta:

```json
{
  "routes": [{
    "visits": [
      {"shipmentLabel": "cliente2"},
      {"shipmentLabel": "cliente3"},
      {"shipmentLabel": "cliente1"}
    ]
  }]
}
```

**Esto significa:** La API optimizó el orden de visitas.

---

## ❌ Errores Comunes

### Error 1: "API key not valid"
```json
{
  "error": {
    "code": 400,
    "message": "API key not valid. Please pass a valid API key."
  }
}
```

**Solución:**
- Verifica que copiaste bien la API key
- Espera 5 minutos (los cambios tardan en aplicarse)

---

### Error 2: "PERMISSION_DENIED"
```json
{
  "error": {
    "code": 403,
    "message": "Route Optimization API has not been used..."
  }
}
```

**Solución:**
- Ve a Google Cloud Console
- Asegúrate de habilitar "Route Optimization API"
- Espera 2-3 minutos y vuelve a intentar

---

### Error 3: "RESOURCE_EXHAUSTED" o "Billing not enabled"
```json
{
  "error": {
    "code": 429,
    "message": "Project XXX has billing disabled..."
  }
}
```

**Solución:**
- Ve a Google Cloud Console > Billing
- Vincula una cuenta de facturación
- No te cobrarán por el test (solo €0.001)

---

## 🎯 Siguientes Pasos

Una vez que el test con cURL funcione:

1. ✅ Compila la app en Android Studio
2. ✅ Genera una ruta desde la app
3. ✅ Verifica los logs para ver "GOOGLE ROUTE OPTIMIZATION API"
4. ✅ Compara distancia total con ORS

---

## 📞 Si Tienes Problemas

Comparte el error exacto que obtienes y te ayudaré a solucionarlo.

Comandos útiles para debugging:

```bash
# Ver cuota disponible
curl -X GET \
  'https://serviceusage.googleapis.com/v1/projects/YOUR_PROJECT_ID/services/routeoptimization.googleapis.com?key=AIzaSyA7hU2mIgO1lS8pOU_L1HROpSH-7TcUgnw'
```
