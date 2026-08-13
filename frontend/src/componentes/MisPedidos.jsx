import { useState, useEffect } from 'react'
import { api } from '../api.js'

const ETAPAS = ['CREADO', 'PAGO_EN_PROCESO', 'PAGADO', 'EN_PREPARACION', 'EN_CAMINO', 'ENTREGADO']
const TERMINALES = ['CANCELADO', 'RECHAZADO']
const CON_ENTREGA = ['EN_PREPARACION', 'EN_CAMINO', 'ENTREGADO']

const ETIQUETAS = {
  CREADO: 'creado',
  PAGO_EN_PROCESO: 'pagando',
  PAGADO: 'pagado',
  EN_PREPARACION: 'preparando',
  EN_CAMINO: 'en camino',
  ENTREGADO: 'entregado'
}

/**
 * Seguimiento de pedidos, y también donde se paga.
 *
 * Recarga cada 3 segundos porque el pedido avanza por eventos y el front no se
 * entera: cuando Pagos cobra o Entregas asigna repartidor, el estado cambia en
 * el servidor sin que nadie avise al navegador. Con WebSockets o SSE se vería
 * al instante; el sondeo es la versión simple.
 *
 * Ese sondeo es justo lo que hace visible la saga: al pulsar «pagar» el pedido
 * queda en PAGO_EN_PROCESO y, sin tocar nada más, va pasando a PAGADO,
 * preparando, en camino y entregado.
 */
export default function MisPedidos({ usuario }) {
  const [pedidos, setPedidos] = useState([])
  const [entregas, setEntregas] = useState({})
  const [error, setError] = useState(null)
  const [pagando, setPagando] = useState(null)

  async function cargar() {
    const lista = await api.misPedidos(usuario.usuarioId)
    setPedidos(lista)

    // La entrega vive en otro servicio: hay que preguntarle aparte
    const conEntrega = lista.filter((p) => CON_ENTREGA.includes(p.estado))
    const resultados = await Promise.all(
      conEntrega.map((p) => api.entregaDe(p.id).catch(() => null))
    )
    const mapa = {}
    conEntrega.forEach((p, i) => { if (resultados[i]) mapa[p.id] = resultados[i] })
    setEntregas(mapa)
    return lista
  }

  useEffect(() => {
    let vivo = true

    async function sondear() {
      try {
        await cargar()
        if (!vivo) return
      } catch (e) {
        if (vivo) setError(e.message)
      }
    }

    sondear()
    const temporizador = setInterval(sondear, 3000)
    return () => { vivo = false; clearInterval(temporizador) }
  }, [usuario])

  async function pagar(pedidoId) {
    setError(null)
    setPagando(pedidoId)
    try {
      await api.pagarPedido(pedidoId)
      // Refresco inmediato para no esperar al siguiente sondeo. Lo que se ve es
      // PAGO_EN_PROCESO: el resultado del cobro llega por evento.
      await cargar()
    } catch (e) {
      setError(e.message)
    } finally {
      setPagando(null)
    }
  }

  if (pedidos.length === 0 && !error) {
    return <p className="ayuda">Todavía no has hecho ningún pedido.</p>
  }

  return (
    <div className="pedidos">
      {error && <p className="error">{error}</p>}

      {pedidos.map((p) => {
        const etapa = ETAPAS.indexOf(p.estado)
        const fallido = TERMINALES.includes(p.estado)
        const entrega = entregas[p.id]

        return (
          <article key={p.id} className={fallido ? 'fallido' : ''}>
            <div className="cabecera">
              <code>{p.id.slice(0, 8)}</code>
              <strong>S/ {p.total.toFixed(2)}</strong>
              <span className={`estado ${fallido ? 'malo' : ''}`}>
                {ETIQUETAS[p.estado] || p.estado.toLowerCase()}
              </span>
            </div>

            {!fallido && (
              <ol className="progreso">
                {ETAPAS.map((e, i) => (
                  <li key={e} className={i <= etapa ? 'hecho' : ''}>
                    {ETIQUETAS[e]}
                  </li>
                ))}
              </ol>
            )}

            <ul className="lineas">
              {p.lineas.map((l) => (
                <li key={l.productoId}>
                  {l.cantidad} × {l.nombreProducto}
                </li>
              ))}
            </ul>

            {p.estado === 'CREADO' && (
              <div className="acciones">
                <button onClick={() => pagar(p.id)} disabled={pagando === p.id}>
                  {pagando === p.id ? 'Pagando…' : `Pagar S/ ${p.total.toFixed(2)}`}
                </button>
                <span className="ayuda">Este pedido todavía no se ha cobrado.</span>
              </div>
            )}

            {p.estado === 'PAGO_EN_PROCESO' && (
              <p className="ayuda">Esperando la respuesta del servicio de Pagos…</p>
            )}

            {p.motivo && <p className="ayuda">{p.motivo}</p>}
            {entrega && (
              <p className="ayuda">
                Entrega {entrega.estado.toLowerCase()} · repartidor {entrega.repartidorId}
              </p>
            )}
          </article>
        )
      })}
    </div>
  )
}
