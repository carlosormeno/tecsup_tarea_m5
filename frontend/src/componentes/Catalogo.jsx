import { useState, useEffect } from 'react'
import { api } from '../api.js'

/** Productos del catálogo. Los no disponibles se muestran pero no se pueden pedir. */
export default function Catalogo({ onAgregar }) {
  const [productos, setProductos] = useState([])
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    api.productos()
      .then(setProductos)
      .catch((e) => setError(e.message))
      .finally(() => setCargando(false))
  }, [])

  if (cargando) return <p className="ayuda">Cargando catálogo…</p>
  if (error) return <p className="error">{error}</p>

  return (
    <div className="tarjetas">
      {productos.map((p) => (
        <article key={p.id} className={p.disponible ? '' : 'agotado'}>
          <h3>{p.nombre}</h3>
          <p className="ayuda">{p.descripcion}</p>
          <div className="pie">
            <strong>S/ {p.precio.toFixed(2)}</strong>
            {p.disponible ? (
              <button onClick={() => onAgregar(p)}>Agregar</button>
            ) : (
              // `disponible` es `activo && stock > 0`, así que un producto no
              // disponible con existencias es uno retirado de la carta, no uno
              // agotado. Decirlo bien cuesta una línea.
              <span className="marca-agotado">
                {p.stock === 0 ? 'Agotado' : 'No disponible'}
              </span>
            )}
          </div>
          <p className={`ayuda stock ${p.disponible ? '' : 'sin-stock'}`}>
            stock: {p.stock}
          </p>
        </article>
      ))}
    </div>
  )
}
