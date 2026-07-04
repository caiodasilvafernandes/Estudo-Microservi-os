package br.com.caiofernandes.pedido.api.dto;

import br.com.caiofernandes.pedido.api.entity.ItemPedido;
import br.com.caiofernandes.pedido.api.entity.Pedido;

import java.util.List;

public record PedidoReqDTO(String cliente,
                           List<ItemPedido> itens,
                           Double valorTotal,
                           String emailNotification) {}
