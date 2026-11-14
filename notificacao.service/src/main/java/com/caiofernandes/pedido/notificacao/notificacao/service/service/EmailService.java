package com.caiofernandes.pedido.notificacao.notificacao.service.service;

import com.caiofernandes.pedido.notificacao.notificacao.service.entity.Pedido;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    public void enviarEmail(Pedido pedido){
        try {
            SimpleMailMessage simpleMailMessage = new SimpleMailMessage();

            simpleMailMessage.setFrom("api-company@company.com");
            simpleMailMessage.setTo(pedido.getEmailNotification());
            simpleMailMessage.setSubject("Pedido de compra");
            simpleMailMessage.setText(this.gerarMensagem(pedido));

            mailSender.send(simpleMailMessage);

            System.out.println("Email enviado com sucesso!");

        } catch (Exception e) {
            System.err.println("Erro ao enviar email: " + e.getMessage());
            throw e;
        }
    }

    private String gerarMensagem(Pedido pedido){
        String pedidoId = pedido.getIdPedido().toString();
        String cliente = pedido.getCliente();
        String valor =  pedido.getValorTotal().toString();
        String status = pedido.getStatus().toString();

        return String.format("Olá %s seu pedido de n' %s com valor %s, foi realizado com sucessp. \nStatus: %s", cliente, pedidoId, valor, status);
    }
}
