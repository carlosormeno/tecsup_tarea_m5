import { useState } from 'react'
import { api } from '../api.js'

/**
 * Carrito y checkout.
 *
 * El precio que se muestra aquí es solo informativo: el que vale es el que
 * order-service consulta a Catálogo al crear el pedido. Si cambiara entre
 * medias, manda el del servidor.
 */
export default function Carrito({ carrito, usuario, onCambiarCantidad, onVaciar, onPedidoCreado }) {
  const [direccion, setDireccion] = useState('Av. Arequipa 123')
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)

  const total = carrito.reduce((s, i) => s + i.producto.precio * i.cantidad, 0)

  async function confirmar() {
    setError(null)
    setEnviando(true)
    try {
      await api.crearPedido(
        usuario.usuarioId,
        direccion,
        carrito.map((i) => ({ productoId: i.producto.id, cantidad: i.cantidad }))
      )
      onVaciar()
      onPedidoCreado()
    } catch (e) {
      setError(e.message)
    } finally {
      setEnviando(false)
    }
  }

  if (carrito.length === 0) {
    return <p className="ayuda">El carrito está vacío.</p>
  }

  return (
    <div className="carrito">
      <table>
        <tbody>
          {carrito.map((i) => (
            <tr key={i.producto.id}>
              <td>
                {i.producto.nombre}
                <span className="ayuda"> · S/ {i.producto.precio.toFixed(2)} c/u</span>
              </td>
              <td className="cantidad">
                <button
                  onClick={() => onCambiarCantidad(i.producto.id, -1)}
                  title={i.cantidad === 1 ? 'Quitar del carrito' : 'Quitar uno'}>
                  −
                </button>
                <span>{i.cantidad}</span>
                <button
                  onClick={() => onCambiarCantidad(i.producto.id, +1)}
                  disabled={i.cantidad >= i.producto.stock}
                  title={i.cantidad >= i.producto.stock ? 'No hay más stock' : 'Añadir uno'}>
                  +
                </button>
              </td>
              <td className="derecha">S/ {(i.producto.precio * i.cantidad).toFixed(2)}</td>
            </tr>
          ))}
          <tr className="total">
            <td colSpan="2">Total</td>
            <td className="derecha">S/ {total.toFixed(2)}</td>
          </tr>
        </tbody>
      </table>

      <label>
        Dirección de entrega
        <input value={direccion} onChange={(e) => setDireccion(e.target.value)} />
      </label>

      {error && <p className="error">{error}</p>}

      {total > 500 && (
        <p className="aviso">
          Los pedidos por encima de S/ 500 los rechaza el servicio de Pagos al
          pagarlos. Sirve para ver la compensación de la saga.
        </p>
      )}

      <p className="ayuda">
        El pedido queda pendiente de pago: se cobra desde «Mis pedidos».
      </p>

      <div className="acciones">
        <button onClick={confirmar} disabled={enviando}>
          {enviando ? 'Creando…' : 'Crear pedido'}
        </button>
        <button className="enlace" onClick={onVaciar}>Vaciar carrito</button>
      </div>
    </div>
  )
}
