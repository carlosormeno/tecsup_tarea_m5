import { useState, useEffect } from 'react'
import { api } from '../api.js'

const ETIQUETAS = {
  ASIGNADA: 'asignada',
  EN_CAMINO: 'en camino',
  COMPLETADA: 'completada',
  FALLIDA: 'fallida'
}

/**
 * Panel del repartidor.
 *
 * Esta vista existe porque Entregas asigna el repartidor y ahí se detiene: la
 * entrega no avanza sola. `PATCH /api/entregas/{id}/estado` está pensado para
 * la aplicación del repartidor, que es un actor humano, y esto es esa
 * aplicación reducida a tres botones.
 *
 * Cada botón publica `entrega.estado-cambiado`, y ese evento es lo que empuja
 * al pedido: COMPLETADA lo lleva a ENTREGADO, y de ahí Usuarios suma los
 * puntos de fidelidad. Pulsar aquí y ver moverse «Mis pedidos» es la saga
 * completa a la vista.
 */
export default function Repartidor() {
  const [entregas, setEntregas] = useState([])
  const [error, setError] = useState(null)
  const [ocupado, setOcupado] = useState(null)

  async function cargar() {
    setEntregas(await api.entregas())
  }

  useEffect(() => {
    let vivo = true

    async function sondear() {
      try {
        const lista = await api.entregas()
        if (vivo) setEntregas(lista)
      } catch (e) {
        if (vivo) setError(e.message)
      }
    }

    sondear()
    const temporizador = setInterval(sondear, 3000)
    return () => { vivo = false; clearInterval(temporizador) }
  }, [])

  async function avanzar(id, nuevoEstado, detalle) {
    setError(null)
    setOcupado(id)
    try {
      await api.avanzarEntrega(id, nuevoEstado, detalle)
      await cargar()
    } catch (e) {
      setError(e.message)
    } finally {
      setOcupado(null)
    }
  }

  if (entregas.length === 0 && !error) {
    return (
      <p className="ayuda">
        No hay entregas. Aparecen cuando un pedido se paga y se confirma.
      </p>
    )
  }

  return (
    <div className="pedidos">
      {error && <p className="error">{error}</p>}

      {entregas.map((e) => {
        const terminada = e.estado === 'COMPLETADA' || e.estado === 'FALLIDA'

        return (
          <article key={e.id} className={e.estado === 'FALLIDA' ? 'fallido' : ''}>
            <div className="cabecera">
              <code>pedido {e.pedidoId.slice(0, 8)}</code>
              <strong>repartidor {e.repartidorId}</strong>
              <span className={`estado ${e.estado === 'FALLIDA' ? 'malo' : ''}`}>
                {ETIQUETAS[e.estado]}
              </span>
            </div>

            <p className="ayuda">{e.direccion}</p>
            {e.detalle && <p className="ayuda">{e.detalle}</p>}

            {!terminada && (
              <div className="acciones">
                {e.estado === 'ASIGNADA' && (
                  <button
                    onClick={() => avanzar(e.id, 'EN_CAMINO', 'Pedido recogido')}
                    disabled={ocupado === e.id}>
                    Recoger y salir
                  </button>
                )}
                {e.estado === 'EN_CAMINO' && (
                  <button
                    onClick={() => avanzar(e.id, 'COMPLETADA', 'Entregado en puerta')}
                    disabled={ocupado === e.id}>
                    Marcar entregado
                  </button>
                )}
                <button
                  className="enlace"
                  onClick={() => avanzar(e.id, 'FALLIDA', 'Nadie en el domicilio')}
                  disabled={ocupado === e.id}>
                  no se pudo entregar
                </button>
              </div>
            )}
          </article>
        )
      })}
    </div>
  )
}
