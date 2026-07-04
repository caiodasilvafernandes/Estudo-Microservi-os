package com.caiofernandes.pedido.notificacao.notificacao.service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ItemPedido {
    private UUID idItemPedido = UUID.randomUUID();
    private Produto produto;
    private int quantidade;
}
