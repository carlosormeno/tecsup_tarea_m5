# ADR-011 · JWT con clave simétrica compartida

| | |
|---|---|
| **Estado** | Aceptada |
| **Fecha** | 2026-08-11 |
| **Secciones del documento** | 5.1 Autenticación y autorización · 5.3 Gestión de secretos |
| **Relacionada con** | [ADR-008](ADR-008-que-se-comparte.md) (el filtro vive en `shared`) · [ADR-005](ADR-005-validacion-de-precio-sincrona.md) (la llamada entre servicios que propaga el token) |

## Contexto

Los cinco servicios exponen API REST y ninguno debe quedar abierto. Hace falta
que un cliente se autentique una vez y que **los cinco servicios reconozcan esa
identidad** sin volver a preguntar a nadie, porque una llamada de validación por
petición convertiría al servicio de Usuarios en un punto único de fallo de todo
el sistema.

JWT resuelve eso: el token lleva la identidad firmada y cada servicio la
verifica por su cuenta. La decisión que queda es **con qué clave se firma y
quién puede firmar**.

Hay además una pregunta previa que el enunciado planteaba de forma absoluta —
«ningún endpoint público»— y que en la práctica no se puede cumplir al pie de la
letra: `/auth/login` no puede exigir el token que todavía no existe, Prometheus
raspa métricas cada 15 segundos y no renueva tokens, y un Swagger que exija
pegar un token a mano es inservible en una demostración.

## Decisión

**Una única clave simétrica HS256 compartida por los cinco servicios**, con
jjwt 0.12.6.

- **Usuarios es el único que firma.** Es el único con `JwtEmisor` y el único que
  lee la propiedad `jwt.expiracion-segundos` (3600 s). Los otros cuatro solo
  verifican.
- **La verificación es común y vive una sola vez**, en `shared`:
  `JwtTokenProvider` + `JwtAuthenticationFilter` + `SecurityConfig`
  ([ADR-008](ADR-008-que-se-comparte.md)).
- **La lista blanca es mínima y explícita**, por propiedad en cada servicio:

  | Servicio | `seguridad.rutas-publicas` |
  |---|---|
  | Usuarios | `/auth/**`, `/actuator/**`, Swagger |
  | Los otros cuatro | `/actuator/**`, Swagger |

  **Ningún endpoint de negocio es público en ninguno de los cinco.** `/auth/**`
  solo se abre donde tiene que estar abierto.
- **El token se propaga entre servicios**: la llamada de Pedidos a Catálogo
  reenvía la cabecera `Authorization` del cliente
  ([ADR-005](ADR-005-validacion-de-precio-sincrona.md)), de modo que no existe
  un canal interno sin autenticar.
- **Un 401 devuelve un cuerpo `ProblemDetail`**, no una respuesta vacía: el
  front necesita poder explicar al usuario qué ocurrió.

## Alternativas consideradas

### A. Claves asimétricas RS256 con JWKS — *descartada*

Usuarios firmaría con la clave privada y publicaría la pública en
`/.well-known/jwks.json`; los demás la descargarían para verificar.

**Es la opción correcta**, y su ventaja es exactamente la debilidad de lo que se
ha elegido: solo el emisor puede firmar. Los demás servicios pueden verificar
pero no fabricar tokens.

Se descarta por alcance: obliga a gestionar un par de claves, a exponer y cachear
el JWKS y a resolver qué pasa si Usuarios no responde cuando otro servicio
arranca. Para una tarea de curso con cinco servicios del mismo autor, el
mecanismo se entiende igual de bien con clave simétrica, y el riesgo que
introduce está acotado a un entorno local.

### B. Sesiones con estado en un almacén compartido (Redis) — *descartada*

Permite revocar al instante, que es la carencia real de JWT. Se descarta porque
introduce un componente más y un punto único de fallo, y porque cada petición
pasaría a depender de una consulta remota.

### C. Validación centralizada: cada servicio pregunta a Usuarios — *descartada*

Sin claves repartidas y con revocación inmediata. Se descarta porque convierte a
Usuarios en dependencia síncrona de **todas** las peticiones del sistema: si cae,
cae todo. Es justo lo contrario de lo que se buscaba con
[ADR-001](ADR-001-microservicios-frente-a-monolito.md).

### D. Una pasarela de API que valide en el borde — *descartada*

Un único punto de validación delante de los cinco. Es la arquitectura habitual y
simplificaría los servicios. Se descarta por alcance: añade un componente al
despliegue, y dejaría a los servicios desprotegidos si alguien los llamara
directamente. Se menciona en el documento como evolución natural.

## Consecuencias

### Positivas

- **Verificación local y sin red**: ningún servicio consulta a nadie para saber
  quién llama. Usuarios puede estar caído y los otros cuatro siguen atendiendo
  peticiones autenticadas.
- **Un solo sitio que auditar**: la lógica de seguridad son tres clases en
  `shared`, no cinco copias.
- **La configuración de acceso es legible en un vistazo**, en una propiedad de
  cada `application.yaml`, en lugar de repartida por anotaciones.
- Verificado por pruebas en cada servicio: sin token `401`, con token válido
  `200`, firma ajena `401`, token caducado `401`, cabecera con basura `401`.

### Negativas

- **Cualquiera de los cinco servicios podría emitir tokens válidos.** Con clave
  simétrica, verificar y firmar es la misma capacidad. Que solo Usuarios tenga
  `JwtEmisor` es una convención del código, no una imposibilidad criptográfica:
  si Entregas quisiera fabricar un token de administrador, podría.
  **Es la consecuencia más seria de este ADR y por eso se declara la primera.**
- **Filtrar la clave en un servicio las compromete todas.** No hay rotación
  parcial posible: cambiarla obliga a reiniciar los cinco a la vez.
- **No hay revocación.** Un token robado vale hasta que caduca, una hora
  después. Sin lista negra, cerrar sesión es un gesto del navegador.
- **El secreto está en claro en los `application.yaml`**, con valor por defecto
  para desarrollo. En producción iría en un gestor de secretos; hoy está
  documentado como limitación en la sección 5.3.

### Riesgos aceptados

| Riesgo | Por qué se acepta |
|---|---|
| Los cinco servicios pueden firmar tokens | Entorno local, mismo autor y mismo dominio de confianza. En producción: RS256 con JWKS, sin cambiar el resto de la arquitectura — solo el `JwtTokenProvider` |
| Token válido durante una hora tras el cierre de sesión | Una hora es un compromiso razonable frente a la molestia de renovar; no se manejan datos sensibles |
| Swagger y Actuator abiertos | Es la lista blanca mínima que permite demostrar el sistema y raspar métricas. Ningún endpoint de negocio entra en ella |
| El JWT viaja en `localStorage` en el front | Vulnerable a XSS; lo correcto sería una cookie `HttpOnly`. Declarado en el código del front |

## Verificación

```bash
# Ningún endpoint de negocio es público en ninguno de los cinco
grep -n "rutas-publicas" services/*/src/main/resources/application.yaml
# -> solo actuator y Swagger; /auth/** únicamente en user-service

# Solo un servicio firma
grep -rln "Jwts.builder" services/*/src/main/java
# -> únicamente user-service (JwtEmisor)
```

| Qué demuestra la decisión | Cómo se comprueba |
|---|---|
| Sin token no se entra | `SecurityConfigTest.sinToken()` → `401` |
| Una firma ajena se rechaza | `SecurityConfigTest.firmaInvalida()` → `401` |
| Un token caducado se rechaza | `SecurityConfigTest.tokenCaducado()` → `401` |
| Una cabecera corrupta no rompe el filtro | `SecurityConfigTest.cabeceraBasura()` → `401`, no `500` |
| El token se propaga entre servicios | `RestClientConfig.propagarJwt()`; sin él, Catálogo respondería `401` a Pedidos |
| El 401 lleva cuerpo | `SecurityConfig` escribe un `ProblemDetail` en lugar de usar `HttpStatusEntryPoint` |
