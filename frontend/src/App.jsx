import { useState, useEffect } from 'react'
import { api, sesion } from './api.js'
import Acceso from './componentes/Acceso.jsx'
import Catalogo from './componentes/Catalogo.jsx'
import Carrito from './componentes/Carrito.jsx'
import MisPedidos from './componentes/MisPedidos.jsx'
import Repartidor from './componentes/Repartidor.jsx'

/**
 * Estado en useState y vistas cambiadas con una variable.
 *
 * Sin router ni librería de estado a propósito: con cuatro vistas y un carrito,
 * añadirlas sería más ceremonia que ayuda.
 */
export default function App() {
  const [usuario, setUsuario] = useState(sesion.leer())
  const [vista, setVista] = useState('catalogo')
  const [carrito, setCarrito] = useState([])
  const [puntos, setPuntos] = useState(null)

  // Los puntos de fidelidad los actualiza user-service al consumir
  // `pedido.entregado`, así que hay que volver a consultarlos al cambiar de
  // vista: el front no se entera de los eventos.
  useEffect(() => {
    if (!usuario) return
    api.usuario(usuario.usuarioId)
      .then((u) => setPuntos(u.puntosFidelidad))
      .catch(() => setPuntos(null))
  }, [usuario, vista])

  function entrar(datos) {
    sesion.guardar(datos)
    setUsuario(datos)
  }

  function salir() {
    sesion.borrar()
    setUsuario(null)
    setCarrito([])
  }

  function agregar(producto) {
    setCarrito((actual) => {
      const existente = actual.find((i) => i.producto.id === producto.id)
      if (existente) {
        return actual.map((i) =>
          i.producto.id === producto.id ? { ...i, cantidad: i.cantidad + 1 } : i
        )
      }
      return [...actual, { producto, cantidad: 1 }]
    })
  }

  /**
   * Sube o baja la cantidad de una línea. Al llegar a cero, la línea sale.
   *
   * El tope es el stock que devolvió el catálogo. Es solo una ayuda visual:
   * ese stock puede haber cambiado desde que se cargó la página, y la
   * validación de verdad ocurre en el servidor.
   */
  function cambiarCantidad(productoId, delta) {
    setCarrito((actual) =>
      actual
        .map((i) => {
          if (i.producto.id !== productoId) return i
          const nueva = Math.min(i.cantidad + delta, i.producto.stock)
          return { ...i, cantidad: nueva }
        })
        .filter((i) => i.cantidad > 0)
    )
  }

  if (!usuario) return <Acceso onEntrar={entrar} />

  const unidades = carrito.reduce((n, i) => n + i.cantidad, 0)

  return (
    <div className="app">
      <header>
        <h1>Pedidos de Comida</h1>
        <nav>
          <button
            className={vista === 'catalogo' ? 'activo' : ''}
            onClick={() => setVista('catalogo')}>
            Catálogo
          </button>
          <button
            className={vista === 'carrito' ? 'activo' : ''}
            onClick={() => setVista('carrito')}>
            Carrito {unidades > 0 && <span className="globo">{unidades}</span>}
          </button>
          <button
            className={vista === 'pedidos' ? 'activo' : ''}
            onClick={() => setVista('pedidos')}>
            Mis pedidos
          </button>
          {/*
            El repartidor no es el cliente: es otro actor. Comparte pantalla
            porque esto es una demostración, no porque sea el mismo usuario.
          */}
          <button
            className={vista === 'repartidor' ? 'activo' : ''}
            onClick={() => setVista('repartidor')}>
            Repartidor
          </button>
        </nav>
        <div className="usuario">
          <span>
            {usuario.nombre}
            {puntos !== null && <em> · {puntos} puntos</em>}
          </span>
          <button className="enlace" onClick={salir}>salir</button>
        </div>
      </header>

      <main>
        {vista === 'catalogo' && <Catalogo onAgregar={agregar} />}
        {vista === 'carrito' && (
          <Carrito
            carrito={carrito}
            usuario={usuario}
            onCambiarCantidad={cambiarCantidad}
            onVaciar={() => setCarrito([])}
            onPedidoCreado={() => setVista('pedidos')}
          />
        )}
        {vista === 'pedidos' && <MisPedidos usuario={usuario} />}
        {vista === 'repartidor' && <Repartidor />}
      </main>
    </div>
  )
}
