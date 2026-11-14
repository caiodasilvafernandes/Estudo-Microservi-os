package br.com.caiofernandes.pedidos.processador.repository;

import br.com.caiofernandes.pedidos.processador.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PedidoRepository extends JpaRepository<Pedido, UUID> {
}
