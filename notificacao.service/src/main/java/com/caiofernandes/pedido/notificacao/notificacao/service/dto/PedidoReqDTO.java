package com.caiofernandes.pedido.notificacao.notificacao.service.dto;

import com.caiofernandes.pedido.notificacao.notificacao.service.entity.ItemPedido;

import java.util.List;

public record PedidoReqDTO(String cliente,
                           List<ItemPedido> itens,
                           Double valorTotal,
                           String emailNotification) {}
