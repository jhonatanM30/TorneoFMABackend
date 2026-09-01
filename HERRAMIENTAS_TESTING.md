# Herramientas para Testing de la API

## 🛠️ Opciones disponibles

Tienes 3 opciones principales para probar la API:

---

## 1️⃣ POSTMAN (Recomendado - Visual y fácil)

### Instalación
1. Descarga desde: https://www.postman.com/downloads/
2. Instala en tu computadora
3. Crea una cuenta (o usa sin cuenta)

### Ventajas
- Interfaz visual e intuitiva
- Puedes guardar colecciones de requests
- Genera automáticamente ejemplos de código
- Historial de peticiones
- Tests automáticos

### Pasos para usar con MAS-QUE-AMIGOS
1. Abre Postman
2. Click en "Import"
3. Selecciona la opción "Raw text"
4. Copia el contenido del archivo `MasQueAmigos-Collection.postman_collection.json` de la guía de pruebas
5. Pega en Postman y da click en Import
6. ¡Ya puedes ejecutar todos los requests!

### Ejemplo básico en Postman
```
1. Click en "+" para nuevo tab
2. Selecciona método: POST
3. Pega URL: http://localhost:57075/api/equipos
4. Click en "Body"
5. Selecciona "raw" y "JSON"
6. Pega:
   {
     "nombre": "Deportivo Cali",
     "directorTecnico": "Rafael Dudamel",
     "imagenUrl": "https://example.com/cali.png",
     "titulos": 12,
     "tipoClasificacion": "Primer Division"
   }
7. Click en "Send"
```

---

## 2️⃣ INSOMNIA (Alternativa moderna)

### Instalación
1. Descarga desde: https://insomnia.rest/
2. Instala en tu computadora
3. Abre la aplicación

### Ventajas
- Más moderna que Postman
- Soporte nativo para GraphQL
- Mejor gestor de variables
- Gratuita y open-source

### Pasos básicos
Similar a Postman: New Request → Selecciona método → Pega URL → Configura body → Send

---

## 3️⃣ CURL (Línea de comandos - más técnico)

### ¿Qué es?
Herramienta nativa de Windows (PowerShell) para hacer peticiones HTTP desde terminal

### Ventajas
- No necesita instalar nada
- Muy rápido
- Fácil de automatizar
- Perfecto para scripting

### Pasos para usar
1. Abre PowerShell (tecla Windows + R, escribe "powershell")
2. Copia-pega un comando curl de la guía de pruebas
3. Presiona Enter

### Ejemplo
```powershell
curl -X POST http://localhost:57075/api/equipos `
  -H "Content-Type: application/json" `
  -d '{
    "nombre": "Deportivo Cali",
    "directorTecnico": "Rafael Dudamel",
    "imagenUrl": "https://example.com/cali.png",
    "titulos": 12,
    "tipoClasificacion": "Primer Division"
  }'
```

**Nota en PowerShell:** Usa backtick (`) al final de línea para continuar en la siguiente, no se usa && como en bash.

---

## 📊 Comparativa de herramientas

| Característica | Postman | Insomnia | Curl |
|---|---|---|---|
| Fácil de usar | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ |
| Instalación | Requerida | Requerida | Nativa (Win10+) |
| Interfaz visual | Sí | Sí | No |
| Guardar colecciones | Sí | Sí | No (pero script) |
| Variables de entorno | Sí | Sí | No |
| Automatización | Sí | Sí | Sí |
| Gratuito | Parcial | Sí | Sí |

---

## 🚀 Recomendación para principiantes

**Usa POSTMAN** porque:
1. Es la más fácil de aprender
2. Tiene interfaz visual clara
3. Puedes guardar todos tus requests en una colección
4. No necesitas memorizar comandos

---

## 🔧 Requisitos mínimos

Sea cual sea la herramienta que uses, necesitas:

1. ✅ **API en ejecución**
   ```bash
   cd c:\FICOHSA\RutaAprendizaje\mas-que-amigos\mas-que-amigos
   ./mvnw spring-boot:run
   ```

2. ✅ **Verificar que está activa**
   - Abre navegador y ve a http://localhost:57075
   - O usa cualquier tool y haz un GET a http://localhost:57075/api/equipos

3. ✅ **La guía de pruebas (GUIA_PRUEBAS.md)**
   - Tienes los payloads listos
   - Tienes el orden correcto de ejecución
   - Tienes los casos de error para validar

---

## 💡 Tips de testing

### Usa variables para reutilizar IDs
En Postman e Insomnia puedes guardar valores en variables:

```
Si un equipo devuelve: {"id": 1, "nombre": "Deportivo Cali", ...}

Guarda el ID 1 como variable: {{equipoId}}

Luego úsalo en jugadores: {"idEquipo": {{equipoId}}, ...}
```

### Organiza los requests por módulo
```
├── Equipos
│   ├── POST Crear
│   ├── GET Listar
│   ├── GET Por nombre
│   ├── PUT Actualizar
│   └── DELETE Eliminar
├── Jugadores
│   ├── POST Crear
│   ├── GET Listar
│   ├── GET Por nombre
│   ├── PUT Actualizar
│   └── DELETE Eliminar
├── Partidos
├── Estadísticas
└── Alineaciones
```

### Verifica status codes
- **200 OK**: Operación exitosa
- **201 Created**: Recurso creado
- **204 No Content**: Eliminación exitosa
- **400 Bad Request**: Validación fallida
- **404 Not Found**: Recurso no existe
- **500 Internal Server Error**: Error del servidor

---

## 🐛 Solución de problemas

### "Connection refused" al intentar conectar
- Verifica que la API está corriendo: http://localhost:57075
- Si no, ejecuta: `./mvnw spring-boot:run` en la carpeta del proyecto

### "400 Bad Request"
- Revisión el formato JSON en tu request
- Verifica que todos los campos obligatorios están presentes
- Consulta GUIA_PRUEBAS.md para ejemplos correctos

### "404 Not Found"
- Verifica que el recurso existe (ej: el equipo con id=1)
- Verifica que la URL es correcta

### Error con caracteres especiales en URL
- Usa URL encoding: `Deportivo%20Cali` en lugar de `Deportivo Cali`
- Postman lo hace automáticamente

---

## 📝 Checklist antes de empezar

- [ ] API en ejecución en http://localhost:57075
- [ ] Postman/Insomnia/Terminal disponibles
- [ ] GUIA_PRUEBAS.md abierta como referencia
- [ ] Datos de ejemplo listos (nombres de equipos, jugadores, etc.)

¡Listo para empezar a probar! 🎯
