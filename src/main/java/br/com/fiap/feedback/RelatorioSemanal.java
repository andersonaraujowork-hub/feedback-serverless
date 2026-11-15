package br.com.fiap.feedback;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import com.azure.communication.email.*;
import com.azure.communication.email.models.*;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.data.tables.models.TableEntity;
import java.util.HashMap;
import java.util.Map;

public class RelatorioSemanal {
    @FunctionName("relatorio-semanal")
    public void run(
            @TimerTrigger(name = "timerInfo", schedule = "0 0 8 * * 1") String timerInfo,
            final ExecutionContext context) {
        context.getLogger().info("Gerando relatório semanal...");
        String connectionString = System.getenv("AzureWebJobsStorage");

        TableClient tableClient = new TableClientBuilder()
                .connectionString(connectionString)
                .tableName("avaliacoes")
                .buildClient();

        int total = 0;
        double somaNotas = 0;
        Map<String, Integer> porUrgencia = new HashMap<>();
        Map<String, Integer> porDia = new HashMap<>();

        for (TableEntity entity : tableClient.listEntities()) {
            total++;
            int nota = (int) entity.getProperty("nota");
            String urgencia = (String) entity.getProperty("urgencia");
            String dataEnvio = (String) entity.getProperty("dataEnvio");
            String dia = dataEnvio.substring(0, 10);

            somaNotas += nota;
            porUrgencia.merge(urgencia, 1, Integer::sum);
            porDia.merge(dia, 1, Integer::sum);
        }

        double media = total > 0 ? somaNotas / total : 0;

        context.getLogger().info("=== RELATÓRIO SEMANAL ===");
        context.getLogger().info("Total de avaliações: " + total);
        context.getLogger().info("Média das notas: " + String.format("%.2f", media));
        context.getLogger().info("Por urgência: " + porUrgencia);
        context.getLogger().info("Por dia: " + porDia);

        String emailConnectionString = System.getenv("COMMUNICATION_CONNECTION_STRING");
        String adminEmail = System.getenv("ADMIN_EMAIL");
        String senderEmail = System.getenv("SENDER_EMAIL");

        if (emailConnectionString != null) {
            String corpo = "=== RELATÓRIO SEMANAL ===\n" +
                    "Total de avaliações: " + total + "\n" +
                    "Média das notas: " + String.format("%.2f", media) + "\n" +
                    "Por urgência: " + porUrgencia + "\n" +
                    "Por dia: " + porDia;

            EmailClient emailClient = new EmailClientBuilder()
                    .connectionString(emailConnectionString)
                    .buildClient();

            EmailMessage message = new EmailMessage()
                    .setSenderAddress(senderEmail)
                    .setToRecipients(new EmailAddress(adminEmail))
                    .setSubject("Relatório Semanal de Avaliações")
                    .setBodyPlainText(corpo);

            emailClient.beginSend(message).waitForCompletion();
            context.getLogger().info("Relatório enviado por e-mail para: " + adminEmail);
        }

    }
}
