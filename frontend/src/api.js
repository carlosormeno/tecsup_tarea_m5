// =============================================================================
// Llamadas a los microservicios.
//
// El front habla con CUATRO servicios distintos, cada uno en su puerto. No hay
// pasarela de API que los unifique, y eso se ve aquí: es el precio de no tener
// una, y es honesto que se note.
//
// El token se guarda en localStorage para que sobreviva a recargar la página.
// En un sistema real convendría una cookie HttpOnly, porque localStorage es
// accesible desde JavaScript y por tanto vulnerable a XSS.
// =============================================================================

// Dos despliegues, dos formas de llegar a los servicios:
//
//   Desarrollo (Podman Compose) -> cada servicio en su puerto del host. Es
//   cross-origin, y por eso los servicios necesitan CORS.
//
//   Kubernetes -> el Ingress sirve el front y las APIs bajo el MISMO origen,
//   enrutando por prefijo de ruta. Con rutas relativas no hay cross-origin y
//   CORS deja de hacer falta.
//
// Que los prefijos no choquen entre servicios (/auth, /api/usuarios,
// /api/productos, /api/pedidos, /api/entregas) es lo que permite enrutar por
// ruta sin inventar un prefijo artificial por servicio.
const MISMO_ORIGEN = import.meta.env.VITE_MISMO_ORIGEN === 'true'

const SERVICIOS = MISMO_ORIGEN
  ? { usuarios: '', catalogo: '', pedidos: '', entregas: '' }
  : {
      usuarios: 'http://localhost:8081',
      catalogo: 'http://localhost:8082',
      pedidos: 'http://localhost:8083',
      entregas: 'http://localhost:8085'
    }

export const sesion = {
  guardar(datos) {
    localStorage.setItem('sesion', JSON.stringify(datos))
  },
  leer() {
    const crudo = localStorage.getItem('sesion')
    return crudo ? JSON.parse(crudo) : null
  },
  borrar() {
    localStorage.removeItem('sesion')
  }
}

async function pedir(servicio, ruta, opciones = {}) {
  const actual = sesion.leer()
  const cabeceras = { 'Content-Type': 'application/json', ...opciones.headers }

  if (actual?.token) {
    cabeceras.Authorization = `Bearer ${actual.token}`
  }

  const respuesta = await fetch(`${SERVICIOS[servicio]}${ruta}`, { ...opciones, headers: cabeceras })

  if (respuesta.status === 204) return null

  const cuerpo = await respuesta.text()
  const datos = cuerpo ? JSON.parse(cuerpo) : null

  if (!respuesta.ok) {
    // Los servicios devuelven ProblemDetail, así que hay un mensaje
    // aprovechable en lugar de un código pelado.
    throw new Error(datos?.detail || datos?.title || `Error ${respuesta.status}`)
  }
  return datos
}

export const api = {
  registro: (datos) =>
    pedir('usuarios', '/auth/registro', { method: 'POST', body: JSON.stringify(datos) }),

  login: (email, password) =>
    pedir('usuarios', '/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    }),

  usuario: (id) => pedir('usuarios', `/api/usuarios/${id}`),

  productos: () => pedir('catalogo', '/api/productos'),

  crearPedido: (clienteId, direccionEntrega, items) =>
    pedir('pedidos', '/api/pedidos', {
      method: 'POST',
      body: JSON.stringify({ clienteId, direccionEntrega, items })
    }),

  // Este es el que arranca la saga. Devuelve el pedido en PAGO_EN_PROCESO, no
  // el resultado del cobro: eso lo decide Pagos y llega por evento un momento
  // después, así que hay que volver a consultar el pedido para saberlo.
  pagarPedido: (id) => pedir('pedidos', `/api/pedidos/${id}/pagar`, { method: 'POST' }),

  misPedidos: (clienteId) => pedir('pedidos', `/api/pedidos?clienteId=${clienteId}`),

  pedido: (id) => pedir('pedidos', `/api/pedidos/${id}`),

  entregaDe: (pedidoId) => pedir('entregas', `/api/entregas/pedido/${pedidoId}`),

  entregas: () => pedir('entregas', '/api/entregas'),

  // El repartidor reportando su avance. Cada PATCH publica
  // `entrega.estado-cambiado`, que es lo que hace avanzar al pedido: sin
  // alguien que llame aquí, la saga se queda en EN_PREPARACION para siempre.
  avanzarEntrega: (id, nuevoEstado, detalle) => {
    const params = new URLSearchParams({ nuevoEstado })
    if (detalle) params.set('detalle', detalle)
    return pedir('entregas', `/api/entregas/${id}/estado?${params}`, { method: 'PATCH' })
  }
}
