package br.com.fiap.feedback;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.data.tables.models.TableEntity;

import com.azure.communication.email.*;
import com.azure.communication.email.models.*;

public class NotificarUrgencia {

    @FunctionName("notificar-urgencia")
    public void run(
            @QueueTrigger(name = "message", queueName = "avaliacoes-criticas", connection = "AzureWebJobsStorage") String rowKey,
            final ExecutionContext context) {

        context.getLogger().info("Notificando urgência para avaliação: " + rowKey);

        String connectionString = System.getenv("AzureWebJobsStorage");

        TableClient tableClient = new TableClientBuilder()
                .connectionString(connectionString)
                .tableName("avaliacoes")
                .buildClient();

        TableEntity entity = tableClient.getEntity("avaliacao", rowKey);

        String descricao = (String) entity.getProperty("descricao");
        String urgencia = (String) entity.getProperty("urgencia");
        String dataEnvio = (String) entity.getProperty("dataEnvio");

        String emailConnectionString = System.getenv("COMMUNICATION_CONNECTION_STRING");
        String adminEmail = System.getenv("ADMIN_EMAIL");
        String senderEmail = System.getenv("SENDER_EMAIL");

        if (emailConnectionString != null) {
            EmailClient emailClient = new EmailClientBuilder()
                    .connectionString(emailConnectionString)
                    .buildClient();

            EmailMessage message = new EmailMessage()
                    .setSenderAddress(senderEmail)
                    .setToRecipients(new EmailAddress(adminEmail))
                    .setSubject("Avaliação Crítica Recebida")
                    .setBodyPlainText(
                            "Descrição: " + descricao +
                                    "\nUrgência: " + urgencia +
                                    "\nData: " + dataEnvio);

            emailClient.beginSend(message).waitForCompletion();
            context.getLogger().info("E-mail enviado para: " + adminEmail);
        } else {
            context.getLogger().warning("COMMUNICATION_CONNECTION_STRING não configurado.E-mail não enviado.");
        }

    }

}
