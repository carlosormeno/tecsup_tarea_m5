import { useState } from 'react'
import { api } from '../api.js'

/** Login y registro. Los dos únicos endpoints que no necesitan token. */
export default function Acceso({ onEntrar }) {
  const [esRegistro, setEsRegistro] = useState(false)
  const [nombre, setNombre] = useState('')
  const [email, setEmail] = useState('carlos@test.com')
  const [password, setPassword] = useState('password123')
  const [direccion, setDireccion] = useState('Av. Arequipa 123')
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(false)

  async function enviar(e) {
    e.preventDefault()
    setError(null)
    setCargando(true)
    try {
      if (esRegistro) {
        await api.registro({ nombre, email, password, direccion })
      }
      onEntrar(await api.login(email, password))
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }

  return (
    <div className="acceso">
      <form onSubmit={enviar}>
        <h1>Pedidos de Comida</h1>
        <p className="ayuda">{esRegistro ? 'Crea tu cuenta' : 'Entra con tu cuenta'}</p>

        {esRegistro && (
          <input
            placeholder="Nombre"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            required
          />
        )}
        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <input
          type="password"
          placeholder="Contraseña"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        {esRegistro && (
          <input
            placeholder="Dirección de entrega"
            value={direccion}
            onChange={(e) => setDireccion(e.target.value)}
          />
        )}

        {error && <p className="error">{error}</p>}

        <button type="submit" disabled={cargando}>
          {cargando ? 'Un momento…' : esRegistro ? 'Registrarme' : 'Entrar'}
        </button>

        <button type="button" className="enlace" onClick={() => setEsRegistro(!esRegistro)}>
          {esRegistro ? '¿Ya tienes cuenta? Entra' : '¿No tienes cuenta? Regístrate'}
        </button>
      </form>
    </div>
  )
}
