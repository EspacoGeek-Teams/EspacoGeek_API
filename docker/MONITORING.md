# Prometheus + Grafana - EspacoGeek Backend

Guia completo para habilitar monitoramento com Prometheus e Grafana.

## 📋 Índice

1. [Visão Geral](#visão-geral)
2. [Pré-requisitos](#pré-requisitos)
3. [Instalação Rápida](#instalação-rápida)
4. [Configuração Detalhada](#configuração-detalhada)
5. [Métricas Disponíveis](#métricas-disponíveis)
6. [Dashboards](#dashboards)
7. [GraphQL Monitoring](#graphql-monitoring)
8. [Troubleshooting](#troubleshooting)

---

## 🎯 Visão Geral

Este projeto utiliza:

- **Prometheus**: Coleta de métricas do Spring Boot via actuator
- **Grafana**: Visualização de métricas em dashboards
- **Micrometer**: Integração com Spring Boot para exportar métricas
- **GraphQL Instrumentation**: Instrumentação customizada para capturar métricas de operações GraphQL

### Métricas Capturadas

#### JVM
- Uso de memória (heap/non-heap)
- Threads
- Garbage Collection
- Classes carregadas

#### Spring Boot Actuator
- HTTP requests (taxa, duração)
- Banco de dados (HikariCP)
- Tomcat (threads, conexões)
- Logs (por nível)

#### GraphQL (Customizado)
- Taxa de operações por nome (query/mutation)
- Tempo de resposta percentilizado (p50, p95, p99)
- Taxa de sucesso/erro por operação
- Distribuição por tipo (query vs mutation)

---

## 📦 Pré-requisitos

- Docker e Docker Compose instalados
- Backend Spring Boot rodando em `localhost:8080`
- Arquivo `.env.espacogeek` com variáveis de ambiente

---

## 🚀 Instalação Rápida

### 1. Copiar arquivos de configuração

```bash
cd docker/

# Verificar se os arquivos existem
ls -la prometheus.yml grafana-datasources.yml docker-compose-monitoring.yml
```

### 2. Iniciar containers

```bash
docker-compose -f docker-compose-monitoring.yml up -d
```

### 3. Acessar Grafana

```
URL: http://localhost:3000
Usuário: admin
Senha: admin
```

Imediatamente após o primeiro acesso, **mude a senha do admin**!

### 4. Verificar Prometheus

```
URL: http://localhost:9090
Targets: http://localhost:9090/targets
```

Verifique se o `espacogeek-backend` está com status "UP".

---

## ⚙️ Configuração Detalhada

### Backend - application.properties

Já configurado! Verificar se possui:

```properties
# Actuator / Management Endpoints
management.endpoints.web.exposure.include=health,info,prometheus
management.endpoint.health.show-details=always
management.metrics.export.prometheus.enabled=true
```

**Métricas disponíveis em**: `http://localhost:8080/actuator/prometheus`

### Prometheus Configuration

Arquivo: `docker/prometheus.yml`

O Prometheus será executado com:
- Scrape interval: 15s
- Retention: 30 dias
- Job "espacogeek-backend" → `localhost:8080/actuator/prometheus`

Para adicionar mais jobs (Node Exporter, MySQL), descomente as seções no arquivo.

### Grafana Provisioning

Arquivos de configuração automática em `docker/`:
- `grafana-datasources.yml`: Define Prometheus como datasource
- `grafana-dashboards.yml`: Aponta para diretório de dashboards

Dashboards são carregados automaticamente de:
`docker/grafana-dashboards/`

---

## 📊 Métricas Disponíveis

### JVM Metrics (Micrometer)

```
jvm_memory_used_bytes{area="heap|nonheap",id="..."}
jvm_memory_max_bytes{area="heap|nonheap",id="..."}
jvm_threads_live
jvm_threads_peak
jvm_threads_daemon_threads
jvm_gc_pause_seconds
jvm_classes_loaded_classes
```

### HTTP Metrics

```
http_server_requests_seconds_count{method,status,uri}
http_server_requests_seconds_sum{method,status,uri}
http_server_requests_seconds_bucket{le,method,status,uri}
```

### HikariCP (Connection Pool)

```
hikaricp_connections{pool="..."}
hikaricp_connections_active{pool="..."}
hikaricp_connections_idle{pool="..."}
hikaricp_connections_pending{pool="..."}
```

### Tomcat

```
tomcat_sessions_active_current_sessions
tomcat_threads_current
tomcat_threads_busy
tomcat_global_sent_bytes_total
tomcat_global_received_bytes_total
tomcat_global_error_total
```

### GraphQL (Customizado)

```
graphql_operations_total{operation="...",type="query|mutation",status="success|error"}
graphql_operations_duration_seconds{operation="...",type="query|mutation"}
graphql_operations_duration_seconds_bucket{...}
graphql_operations_duration_seconds_count{...}
graphql_operations_duration_seconds_sum{...}
```

---

## 📈 Dashboards

### Dashboard de GraphQL

**Localização**: `docker/grafana-dashboards/graphql-dashboard.json`

Painéis inclusos:
1. **Total GraphQL Operations (5m)** - Estatísticas
2. **Avg GraphQL Response Time** - Tempo médio de resposta
3. **GraphQL Errors (5m)** - Taxa de erros
4. **Success Rate** - Taxa de sucesso percentual
5. **Operations Rate by Query/Mutation** - Gráfico de taxa
6. **Response Time Percentiles** - P50, P95, P99
7. **Operations by Type** - Query vs Mutation

### Adicionar novo dashboard

1. No Grafana, clique em **"+"** → **"Import"**
2. Cole o JSON do dashboard
3. Selecione Prometheus como datasource
4. Clique em **"Import"**

---

## 🔍 GraphQL Monitoring

### Como funciona

1. **GraphQLExecutionInstrumentation** intercepta todas as operações GraphQL
2. Extrai nome da operação (ex: "GetAnimeList", "CreateUser")
3. Tipo de operação (query, mutation, subscription)
4. Duração em milissegundos
5. Status (success/error)
6. Registra métricas no Micrometer

### Visualizar top operações

**Query Prometheus**:
```promql
# Top 10 queries mais chamadas (5 minutos)
topk(10, sum by (operation) (increase(graphql_operations_total{type="query"}[5m])))

# Top 10 mutations mais lentas (p95)
topk(10, histogram_quantile(0.95, sum by (operation) (rate(graphql_operations_duration_seconds_bucket{type="mutation"}[5m]))))

# Taxa de erro por operação
sum by (operation) (rate(graphql_operations_total{status="error"}[5m]))
```

### Alertas para GraphQL

Adicionar ao `prometheus-rules.yml` (quando criado):

```yaml
groups:
  - name: graphql
    rules:
      - alert: HighGraphQLErrorRate
        expr: |
          (sum(rate(graphql_operations_total{status="error"}[5m])) /
           sum(rate(graphql_operations_total[5m]))) > 0.1
        for: 5m
        annotations:
          summary: "Taxa de erro GraphQL acima de 10%"

      - alert: SlowGraphQLOperation
        expr: |
          histogram_quantile(0.95, sum by (operation)
          (rate(graphql_operations_duration_seconds_bucket[5m]))) > 1
        for: 5m
        annotations:
          summary: "Operação GraphQL lenta: {{ $labels.operation }}"
```

---

## 🐛 Troubleshooting

### Prometheus não conecta ao backend

**Verificação**:
```bash
docker logs prometheus

# Verificar se backend está rodando
curl http://localhost:8080/actuator/health

# Verificar métricas
curl http://localhost:8080/actuator/prometheus | head -20
```

**Solução**:
- Usar `host.docker.internal:8080` no docker-compose.yml (macOS/Windows)
- Ou usar `backend` no docker-compose se o backend estiver na mesma rede

### Grafana não mostra dados

**Verificação**:
```bash
docker exec grafana grafana-cli admin list-admins

# Ver logs
docker logs grafana
```

**Solução**:
1. Acessar Grafana e adicionar datasource manualmente
2. Configuration → Data Sources → Add Prometheus
3. URL: `http://prometheus:9090`
4. Save & test

### GraphQL metrics não aparecem

**Verificação**:
```bash
curl http://localhost:8080/actuator/prometheus | grep graphql_operations
```

**Se não aparecer**:
1. Fazer uma requisição GraphQL:
   ```bash
   curl -X POST http://localhost:8080/graphql \
     -H "Content-Type: application/json" \
     -d '{"query":"query { __typename }"}'
   ```

2. Aguardar 15 segundos (intervalo de scrape)
3. Verificar novamente

### Container restart loop

```bash
docker-compose -f docker-compose-monitoring.yml logs prometheus
docker-compose -f docker-compose-monitoring.yml logs grafana
```

---

## 📚 Referências

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/grafana/)
- [Micrometer Documentation](https://micrometer.io/docs)
- [Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)
- [GraphQL Instrumentation](https://graphql-java.com/documentation/v16/instrumentation/)

---

## 🔐 Segurança

### Em Produção

1. **Mudar senhas padrão do Grafana**
   ```bash
   # Admin password
   docker exec grafana grafana-cli admin set-password admin newpassword123
   ```

2. **Proteger Prometheus**
   - Use proxy reverso (nginx/Apache)
   - Configure básico HTTP auth
   - Restrinja IPs

3. **Volumes persistentes**
   - Grafana data
   - Prometheus data
   - Backups de banco de dados

4. **Variáveis de ambiente**
   - Não colocar credenciais no docker-compose.yml
   - Usar `.env.monitoring`

---

## 🚦 Próximos Passos

1. ✅ Configurar Prometheus + Grafana
2. ✅ Adicionar GraphQL monitoring
3. ⏳ Configurar alertas (AlertManager)
4. ⏳ Adicionar Node Exporter (métricas do SO)
5. ⏳ Adicionar MySQL Exporter (métricas do BD)
6. ⏳ Backup automático de dados
7. ⏳ Integração com ELK para logs (opcional)

---

**Última atualização**: 2026-02-14
**Versão**: 2.0
**Status**: ✅ Operacional

