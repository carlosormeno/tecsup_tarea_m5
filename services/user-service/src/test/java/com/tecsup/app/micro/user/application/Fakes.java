package com.tecsup.app.micro.user.application;

import com.tecsup.app.micro.user.domain.model.Usuario;
import com.tecsup.app.micro.user.domain.repository.CifradorDeClaves;
import com.tecsup.app.micro.user.domain.repository.EmisorDeTokens;
import com.tecsup.app.micro.user.domain.repository.PedidosPuntuados;
import com.tecsup.app.micro.user.domain.repository.UsuarioRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/** Dobles de los puertos de salida. */
final class Fakes {

    private Fakes() {
    }

    static class FakeUsuarios implements UsuarioRepository {
        private final Map<Long, Usuario> datos = new HashMap<>();
        private final AtomicLong siguienteId = new AtomicLong(1);

        @Override
        public Usuario guardar(Usuario usuario) {
            Long id = usuario.getId() != null ? usuario.getId() : siguienteId.getAndIncrement();
            Usuario conId = Usuario.reconstituir(id, usuario.getNombre(), usuario.getEmail(),
                    usuario.getPasswordHash(), usuario.getRoles(), usuario.getDireccion(),
                    usuario.getPuntosFidelidad(), usuario.isActivo(), usuario.getCreadoEn());
            datos.put(id, conId);
            return conId;
        }

        @Override
        public Optional<Usuario> buscarPorId(Long id) {
            return Optional.ofNullable(datos.get(id));
        }

        @Override
        public Optional<Usuario> buscarPorEmail(String email) {
            return datos.values().stream().filter(u -> u.getEmail().equals(email)).findFirst();
        }

        @Override
        public boolean existeEmail(String email) {
            return buscarPorEmail(email).isPresent();
        }

        @Override
        public List<Usuario> buscarTodos() {
            return List.copyOf(datos.values());
        }
    }

    /**
     * Cifrado falso pero con la propiedad que importa: distingue claves
     * correctas de incorrectas sin usar BCrypt, que es lento a propósito y
     * haría las pruebas mucho más lentas.
     */
    static class FakeCifrador implements CifradorDeClaves {
        @Override
        public String cifrar(String claveEnClaro) {
            return "hash:" + claveEnClaro;
        }

        @Override
        public boolean coincide(String claveEnClaro, String hashGuardado) {
            return ("hash:" + claveEnClaro).equals(hashGuardado);
        }
    }

    static class FakeEmisor implements EmisorDeTokens {
        @Override
        public String emitirPara(Usuario usuario) {
            return "token-de-" + usuario.getId();
        }

        @Override
        public long validezEnSegundos() {
            return 3600;
        }
    }

    static class FakePedidosPuntuados implements PedidosPuntuados {
        private final Set<UUID> registrados = new HashSet<>();
        final List<UUID> historial = new ArrayList<>();

        @Override
        public boolean yaPuntuado(UUID pedidoId) {
            return registrados.contains(pedidoId);
        }

        @Override
        public void registrar(UUID pedidoId, Long usuarioId, int puntos) {
            registrados.add(pedidoId);
            historial.add(pedidoId);
        }
    }
}
