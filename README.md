# Feedback Serverless — Tech Challenge Fase 4

Plataforma de feedback de aulas com funções serverless no Azure. Estudantes avaliam aulas via API e administradores recebem alertas e relatórios por e-mail.

## Arquitetura

```
POST /avaliacao
      │
      ▼
receber-avaliacao (HTTP Trigger)
      │  grava na Table Storage
      │  se nota crítica (0-3) → envia para fila
      ▼
avaliacoes-criticas (Azure Queue)
      │
      ▼
notificar-urgencia (Queue Trigger)
      │  envia e-mail ao admin

relatorio-semanal (Timer Trigger — toda segunda às 8h)
      │  lê Table Storage e envia relatório por e-mail
```

**Recursos Azure:**
- Azure Functions (Consumption Plan — Windows)
- Azure Table Storage — armazena avaliações
- Azure Queue Storage — fila de avaliações críticas
- Azure Communication Services — envio de e-mail
- Application Insights — monitoramento

## Funções

### `receber-avaliacao`
- **Trigger:** HTTP POST `/api/receber-avaliacao`
- **Body:**
```json
{
  "descricao": "string",
  "nota": 0
}
```
- **Regra de urgência:** nota 0-3 = CRITICA, 4-6 = MEDIA, 7-10 = BAIXA
- **Retorno:** 200 com mensagem de sucesso ou 400 com erro de validação

### `notificar-urgencia`
- **Trigger:** Queue `avaliacoes-criticas`
- **Ação:** envia e-mail ao admin com descrição, urgência e data

### `relatorio-semanal`
- **Trigger:** Timer — toda segunda-feira às 8h (`0 0 8 * * 1`)
- **Ação:** envia e-mail com total de avaliações, média das notas, quantidade por urgência e por dia

## Variáveis de Ambiente

| Variável | Descrição |
|---|---|
| `AzureWebJobsStorage` | Connection string do Azure Storage |
| `COMMUNICATION_CONNECTION_STRING` | Connection string do Azure Communication Services |
| `ADMIN_EMAIL` | E-mail do administrador |
| `SENDER_EMAIL` | E-mail remetente (domínio do ACS) |

## Deploy

### Pré-requisitos
- Java 21
- Maven
- Azure CLI
- Azure Functions Core Tools

### Manual
```bash
mvn clean package azure-functions:deploy
```

### Automático (CI/CD)
A cada push na branch `main`, o GitHub Actions compila e faz deploy automaticamente via `.github/workflows/deploy.yml`.

## Monitoramento

O Application Insights está habilitado no Function App. Para acessar:

1. Portal Azure → **feedbackserverlessah** → **Application Insights**
2. Métricas disponíveis: invocações, erros, duração das funções e logs de execução
