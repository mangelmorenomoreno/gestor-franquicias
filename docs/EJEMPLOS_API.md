# Ejemplos de uso de la API

Solicitudes y respuestas basadas en los controladores y DTO del backend. Todas las rutas usan el prefijo `/api/v1`. Para ejecutarlas se puede usar [Swagger local](http://localhost:8088/swagger-ui/index.html) o [Swagger de producción](http://2.25.123.13:8088/swagger-ui/index.html).

Los identificadores y datos son ilustrativos; pueden cambiar según el contenido de PostgreSQL. La propiedad `timestamp` se genera en UTC con la fecha y hora de cada solicitud. Los ejemplos se presentan de forma independiente: cambiar un nombre o eliminar un producto modifica las respuestas posteriores.

## Franquicias

### Crear franquicia

```http
POST /api/v1/franquicias
Content-Type: application/json

{
  "nombre": "Franquicia Central"
}
```

**Respuesta: `201`**

```json
{
  "code": "201",
  "message": "Franquicia creada correctamente.",
  "success": true,
  "data": {
    "idFranquicia": 1,
    "nombre": "Franquicia Central"
  },
  "errors": [],
  "timestamp": "2026-11-20T20:00:00Z"
}
```

### Listar franquicias

```http
GET /api/v1/franquicias
```

**Respuesta: `200`**

```json
{
  "code": "200",
  "message": "Franquicias consultadas correctamente.",
  "success": true,
  "data": [
    {
      "idFranquicia": 1,
      "nombre": "Franquicia Central"
    }
  ],
  "errors": [],
  "timestamp": "2026-11-20T20:00:00Z"
}
```

### Consultar franquicia y sucursales

```http
GET /api/v1/franquicias/1
```

**Respuesta: `200`**

```json
{
  "code": "200",
  "message": "Franquicia consultada correctamente.",
  "success": true,
  "data": {
    "idFranquicia": 1,
    "nombre": "Franquicia Central",
    "sucursales": [
      {
        "idSucursal": 10,
        "nombre": "Sucursal Norte"
      }
    ]
  },
  "errors": [],
  "timestamp": "2026-11-20T20:00:00Z"
}
```

### Consultar sucursales de una franquicia

```http
GET /api/v1/franquicias/1/sucursales
```

**Respuesta: `200`**

```json
{
  "code": "200",
  "message": "Sucursales consultadas correctamente.",
  "success": true,
  "data": [
    {
      "idSucursal": 10,
      "nombre": "Sucursal Norte"
    }
  ],
  "errors": [],
  "timestamp": "2026-11-20T20:00:00Z"
}
```

### Cambiar nombre de franquicia

```http
PATCH /api/v1/franquicias/1
Content-Type: application/json

{
  "nombre": "Franquicia Central Renovada"
}
```

**Respuesta: `200`**

```json
{
  "code": "200",
  "message": "Nombre de la franquicia actualizado.",
  "success": true,
  "data": {
    "idFranquicia": 1,
    "nombre": "Franquicia Central Renovada"
  },
  "errors": [],
  "timestamp": "2026-11-20T20:00:00Z"
}
```

## Sucursales

### Crear sucursal

```http
POST /api/v1/franquicias/1/sucursales
Content-Type: application/json

{
  "nombre": "Sucursal Norte"
}
```

**Respuesta: `201`**

```json
{
  "code": "201",
  "message": "Sucursal creada correctamente.",
  "success": true,
  "data": {
    "idSucursal": 10,
    "nombre": "Sucursal Norte"
  },
  "errors": [],
  "timestamp": "2026-11-20T20:00:00Z"
}
```

### Listar sucursales

```http
GET /api/v1/sucursales
```

**Respuesta: `200`**

```json
{
  "code": "200",
  "message": "Sucursales consultadas correctamente.",
  "success": true,
  "data": [
    {
      "idSucursal": 10,
      "idFranquicia": 1,
      "nombre": "Sucursal Norte"
    }
  ],
  "errors": [],
  "timestamp": "2026-11-20T20:00:00Z"
}
```

### Consultar sucursal y productos

```http
GET /api/v1/sucursales/10
```

**Respuesta: `200`**

```json
{
  "code": "200",
  "message": "Sucursal consultada correctamente.",
  "success": true,
  "data": {
    "idSucursal": 10,
    "idFranquicia": 1,
    "nombre": "Sucursal Norte",
    "productos": [
      {
        "idProducto": 100,
        "idSucursal": 10,
        "nombre": "Café",
        "stock": 20
      }
    ]
  },
  "errors": [],
  "timestamp": "2026-11-20T20:00:00Z"
}
```

### Consultar productos de una sucursal

```http
GET /api/v1/sucursales/10/productos
```

**Respuesta: `200`**

```json
{
  "code": "200",
  "message": "Productos consultados correctamente.",
  "success": true,
  "data": [
    {
      "idProducto": 100,
      "idSucursal": 10,
      "nombre": "Café",
      "stock": 20
    }
  ],
  "errors": [],
  "timestamp": "2026-11-20T20:00:00Z"
}
```

### Cambiar nombre de sucursal

```http
PATCH /api/v1/sucursales/10
Content-Type: application/json

{
  "nombre": "Sucursal Parque"
}
```

**Respuesta: `200`**

```json
{
  "code": "200",
  "message": "Nombre de la sucursal actualizado.",
  "success": true,
  "data": {
    "idSucursal": 10,
    "nombre": "Sucursal Parque"
  },
  "errors": [],
  "timestamp": "2026-11-20T20:00:00Z"
}
```

## Productos

### Crear producto

```http
POST /api/v1/sucursales/10/productos
Content-Type: application/json

{
  "nombre": "Café",
  "stock": 20
}
```

**Respuesta: `201`**

```json
{
  "code": "201",
  "message": "Producto creado correctamente.",
  "success": true,
  "data": {
    "idProducto": 100,
    "idSucursal": 10,
    "nombre": "Café",
    "stock": 20
  },
  "errors": [],
  "timestamp": "2026-11-20T20:00:00Z"
}
```

### Listar productos de todas las sucursales

```http
GET /api/v1/productos
```

**Respuesta: `200`**

```json
{
  "code": "200",
  "message": "Productos consultados correctamente.",
  "success": true,
  "data": [
    {
      "idProducto": 100,
      "idSucursal": 10,
      "nombre": "Café",
      "stock": 20
    },
    {
      "idProducto": 101,
      "idSucursal": 10,
      "nombre": "Agua",
      "stock": 8
    }
  ],
  "errors": [],
  "timestamp": "2026-11-20T20:00:00Z"
}
```

### Consultar un producto

```http
GET /api/v1/sucursales/10/productos/100
```

**Respuesta: `200`**

```json
{
  "code": "200",
  "message": "Producto consultado correctamente.",
  "success": true,
  "data": {
    "idProducto": 100,
    "idSucursal": 10,
    "nombre": "Café",
    "stock": 20
  },
  "errors": [],
  "timestamp": "2026-11-20T20:00:00Z"
}
```

### Actualizar stock

```http
PATCH /api/v1/sucursales/10/productos/100/stock
Content-Type: application/json

{
  "stock": 30
}
```

**Respuesta: `200`**

```json
{
  "code": "200",
  "message": "Stock actualizado correctamente.",
  "success": true,
  "data": {
    "idProducto": 100,
    "idSucursal": 10,
    "nombre": "Café",
    "stock": 30
  },
  "errors": [],
  "timestamp": "2026-11-20T20:00:00Z"
}
```

### Cambiar nombre del producto

```http
PATCH /api/v1/sucursales/10/productos/100
Content-Type: application/json

{
  "nombre": "Café Premium"
}
```

**Respuesta: `200`**

```json
{
  "code": "200",
  "message": "Nombre del producto actualizado.",
  "success": true,
  "data": {
    "idProducto": 100,
    "idSucursal": 10,
    "nombre": "Café Premium",
    "stock": 20
  },
  "errors": [],
  "timestamp": "2026-11-20T20:00:00Z"
}
```

### Consultar mayor stock por sucursal

```http
GET /api/v1/franquicias/1/productos/mayor-stock
```

**Respuesta: `200`**

```json
{
  "code": "200",
  "message": "Productos con mayor stock consultados correctamente.",
  "success": true,
  "data": [
    {
      "idSucursal": 10,
      "nombreSucursal": "Sucursal Norte",
      "idProducto": 100,
      "nombreProducto": "Café",
      "stock": 30
    },
    {
      "idSucursal": 11,
      "nombreSucursal": "Sucursal Sur",
      "idProducto": 103,
      "nombreProducto": "Agua",
      "stock": 45
    }
  ],
  "errors": [],
  "timestamp": "2026-11-20T20:00:00Z"
}
```

### Eliminar producto

```http
DELETE /api/v1/sucursales/10/productos/100
```

**Respuesta: `204 No Content`**, sin cuerpo.

## Ejemplo de recurso inexistente

```http
GET /api/v1/franquicias/999999
```

**Respuesta: `404`**

```json
{
  "code": "404",
  "message": "La franquicia no existe.",
  "success": false,
  "data": null,
  "errors": [],
  "timestamp": "2026-11-20T20:00:00Z"
}
```

Los errores de validación tienen código `400`, y los conflictos de unicidad o de actualización concurrente responden con `409`. Las descripciones de validación dependen de la restricción que se incumpla.

> Para probar escrituras es preferible utilizar una base local. La API pública de demostración no requiere autenticación y no debe contener datos personales.
