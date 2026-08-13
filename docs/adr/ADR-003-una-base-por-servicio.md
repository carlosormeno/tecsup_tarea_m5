# ADR-003 · Un motor Postgres con una base de datos por servicio

| | |
|---|---|
| **Estado** | Aceptada |
| **Fecha** | 2026-08-09 |
| **Secciones del documento** | 2.2 Decisiones clave · 3.1 Módulos · 5.2 Aislamiento de datos |
| **Relacionada con** | [ADR-001](ADR-001-microservicios-frente-a-monolito.md) (las fronteras que esto hace cumplir) · [ADR-004](ADR-004-saga-orquestada.md) (lo que cuesta perder la transacción) |

## Contexto

Si los cinco servicios comparten base de datos, las fronteras de
[ADR-001](ADR-001-microservicios-frente-a-monolito.md) son decorativas: nada
impide que Entregas haga un `JOIN` contra la tabla de productos «solo para
mostrar el nombre», y a partir de ahí Catálogo ya no puede cambiar su esquema
sin romper a otro servicio. La base de datos compartida es la vía más común por
la que unos microservicios se convierten en un monolito distribuido, que reúne
los inconvenientes de los dos estilos.

La restricción práctica es que todo esto corre en un portátil con Podman
Compose: cinco instancias de Postgres son cinco procesos, cinco puertos y cinco
juegos de memoria para una tarea de curso.

## Decisión

**Un único motor Postgres, cinco bases de datos y cinco roles**, uno por
servicio. Cada rol es dueño de su base y **no tiene permiso alguno sobre las
demás**.

| Servicio | Base | Rol |
|---|---|---|
| Usuarios | `userdb` | `user_svc` |
| Catálogo | `catalogdb` | `catalog_svc` |
| Pedidos | `orderdb` | `order_svc` |
| Pagos | `paymentdb` | `payment_svc` |
| Entregas | `deliverydb` | `delivery_svc` |

Las crea `infra/postgres/init/01-crear-bases.sh` al arrancar el contenedor, con
un `REVOKE ... FROM PUBLIC` para que el permiso por defecto de Postgres no deje
una puerta abierta.

El esquema de cada base lo gestiona **Flyway** con migraciones versionadas
propias del servicio, y Hibernate arranca con `ddl-auto: validate`: el esquema
lo manda la migración, no la entidad.

La consecuencia de diseño más importante: **ningún dato se consulta con un
`JOIN` entre contextos**. Cuando Pedidos necesita el nombre y el precio de un
producto, los pide a Catálogo y **los copia** en la línea del pedido. Esa copia
no es desnormalización perezosa, es lo correcto: el precio de un pedido es el
que había cuando se hizo, no el de hoy.

## Alternativas consideradas

### A. Una base compartida por los cinco — *descartada*

La más simple de operar y la que permitiría transacciones ACID entre contextos,
evitando toda la saga. Se descarta porque disuelve las fronteras: el acoplamiento
volvería por el esquema aunque el código estuviera perfectamente separado.

### B. Un esquema por servicio dentro de una misma base — *descartada*

Punto intermedio razonable, y con permisos por esquema se puede llegar a un
aislamiento parecido. Se descarta porque un `JOIN` entre esquemas sigue siendo
una línea de SQL: la separación se sostiene sobre permisos bien puestos y no
sobre una imposibilidad. Con bases distintas, el `JOIN` **no se puede escribir**.

### C. Una instancia de Postgres por servicio — *descartada*

Es el aislamiento máximo y lo que se usaría en producción: fallos, versiones,
copias de seguridad y recursos independientes. Se descarta por coste operativo
en un portátil, y porque **no añade nada a lo que se quiere demostrar**: el
aislamiento lógico que impide el acoplamiento ya lo dan las bases y los roles
separados. Lo que se pierde es aislamiento de disponibilidad, y eso se declara
abajo como riesgo.

## Consecuencias

### Positivas

- **La frontera la impone el motor, no la disciplina.** Un `JOIN` entre
  contextos no compila: no hay forma de escribirlo.
- Cada servicio evoluciona su esquema sin coordinarse con nadie. Flyway lleva
  el control por separado en cada base.
- Los datos copiados entre contextos son explícitos y tienen dueño: la línea de
  pedido guarda `nombre_producto` y `precio_unitario` como **fotografía**, no
  como referencia.

### Negativas

- **No hay transacción entre servicios.** Cobrar y descontar stock ocurren en
  bases distintas, lo que obliga a la saga de
  [ADR-004](ADR-004-saga-orquestada.md) y a la compensación.
- **No hay integridad referencial entre contextos.** `pedido.cliente_id` es un
  `BIGINT` sin clave foránea: la base de Pedidos no puede comprobar que ese
  cliente exista en `userdb`.
- **Las consultas que cruzan contextos hay que componerlas.** El seguimiento de
  un pedido en el front necesita dos llamadas, una a Pedidos y otra a Entregas.
- **Datos duplicados a propósito**, con el riesgo de divergencia que eso
  conlleva. Aquí es inofensivo porque las copias son inmutables por definición
  (el precio histórico no debe cambiar), pero es un patrón que hay que aplicar
  con cuidado.

### Riesgos aceptados

| Riesgo | Por qué se acepta |
|---|---|
| Un solo motor es un punto único de fallo: si se cae, se caen los cinco | Entorno de desarrollo local. En producción irían instancias separadas, sin cambiar una línea de código: solo la URL de conexión |
| Las credenciales están en claro en `application.yaml` | Documentado como limitación en la sección 5.3 del documento de arquitectura |

## Verificación

```bash
podman exec postgres psql -U postgres -c "\l"    # las 5 bases
podman exec postgres psql -U postgres -c "\du"   # los 5 roles

# Un servicio NO puede entrar en la base de otro
podman exec postgres psql -U order_svc -d catalogdb -c "select 1"
# -> FATAL: permission denied for database "catalogdb"
```

| Qué demuestra la decisión | Cómo se comprueba |
|---|---|
| Las 5 bases con su rol propio existen | `\l` y `\du` |
| El aislamiento es real | Conectarse con el rol de un servicio a la base de otro falla |
| El esquema lo manda Flyway | `select * from flyway_schema_history` en cada base |
| Las entidades coinciden con el esquema | Los servicios arrancan con `ddl-auto: validate` sin error |
| El precio se copia, no se referencia | `pedido_linea.precio_unitario` en `V1__crear_tablas.sql` de Pedidos |
