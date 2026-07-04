package com.caiofernandes.pedido.notificacao.notificacao.service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Produto {

    private UUID idProduto = UUID.randomUUID();
    private String nome;
    private Double valor;

}
