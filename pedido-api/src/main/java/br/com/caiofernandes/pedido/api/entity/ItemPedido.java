package br.com.caiofernandes.pedido.api.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@Data
public class ItemPedido {
    private UUID idItemPedido = UUID.randomUUID();
    private Produto produto = new Produto();
    private int quantidade;
}
