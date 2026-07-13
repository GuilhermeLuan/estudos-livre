# PRD — Estuda Livre

> Codinome provisório para uma plataforma própria de gestão de estudos, inspirada no problema atendido pelo Aprovado, porém independente, open source e self-hosted.

| Campo | Valor |
|---|---|
| Status | Proposta inicial |
| Data | 13 de julho de 2026 |
| Responsável de produto | A definir |
| Público inicial | Concurseiros e estudantes individuais |
| Estratégia | Individual-first, multiusuário preparado |
| Distribuição | Self-hosted, PWA instalável, sem telemetria externa por padrão |

## 1. Resumo executivo

O Estuda Livre será uma aplicação web instalável para planejar, executar e analisar estudos. Ela reunirá cronômetro de horas líquidas, registro manual, edital verticalizado, ciclos de estudo, fila de revisões, desempenho em questões, histórico e painéis. Sua diferença fundamental será dar ao usuário controle real da instalação e dos dados: implantação por Docker Compose, operação local ou em servidor próprio, funcionamento offline, sincronização transparente, backups documentados e exportação completa em formatos abertos.

O MVP não tentará ser curso preparatório, banco de questões ou tutor de IA. Ele será o melhor sistema pessoal de decisão e registro: responder rapidamente “o que estudar agora?”, “o que revisar hoje?”, “onde estou fraco?” e “estou avançando no edital?”.

O produto seguirá a simplicidade que usuários valorizam no Aprovado, mas reduzirá trabalho manual e dependência da plataforma. A experiência principal terá um botão dominante para iniciar a próxima sessão, uma caixa de entrada de revisões e um painel que conecta horas, cobertura do edital e desempenho — sem transformar o estudo em administração do próprio sistema.

## 2. Pesquisa e evidências

### 2.1 O que o Aprovado oferece hoje

A pesquisa foi realizada em páginas públicas em 13/07/2026; áreas autenticadas não foram acessadas. O produto de referência oferece:

- cadastro de matérias e conteúdos, com recomendação de usar o edital como estrutura;
- atividades por cronômetro ou lançamento manual, com horas líquidas, pausas e anotações;
- calendário, histórico filtrável, gráficos de tempo por matéria e linha do tempo;
- registro de quantidade de exercícios, acertos e tipo de estudo;
- planejamento semanal/mensal e ciclo ponderado por dificuldade, número e peso de questões;
- revisões periódicas configuráveis e revisões semanais;
- aplicativos móveis com uso offline e sincronização com o serviço central;
- modelo gratuito com anúncios e pacotes pagos vitalícios.

Fontes primárias: [página inicial](https://aprovadoapp.com/pt), [central de ajuda](https://aprovadoapp.com/pt/ajuda), [planejamento e ciclo](https://aprovadoapp.com/pt/pacotes/planejamentoestudos), [exercícios e tipos de estudo](https://aprovadoapp.com/pt/pacotes/registroexercicios), [revisões](https://aprovadoapp.com/pt/pacotes/revisaoestudos) e [histórico de versões no App Store](https://apps.apple.com/br/app/aprovado/id586767537).

### 2.2 Sinais qualitativos do público

Relatos públicos indicam que a simplicidade do registro de horas é um valor importante e que excesso de informação pode atrapalhar. Também aparecem necessidades recorrentes de acompanhar acertos por tópico, encaixar questões e revisões no ciclo e reduzir atualizações manuais. Esses relatos são sinais qualitativos, não uma amostra estatisticamente representativa. Fontes: [comparação de ferramentas](https://www.reddit.com/r/concursospublicos/comments/1rnmna2/plataforma_aprovado/), [controle de estudo e acertos por assunto](https://www.reddit.com/r/concursospublicos/comments/1ga7k2f/aplicativo_para_controle_de_estudos/), [ciclo, questões e revisão espaçada](https://www.reddit.com/r/concursospublicos/comments/1qm2vod/algu%C3%A9m_usa_algum_appplataforma_pra_organizar/) e [controle de revisões](https://www.reddit.com/r/concursospublicos/comments/1dchk2d/controle_de_revis%C3%A3o/).

### 2.3 Oportunidades identificadas

1. **Soberania dos dados:** documentação pública do Aprovado descreve backup no serviço central e exclusão da conta, mas a ajuda não apresenta importação ou exportação pelo usuário. O Estuda Livre terá exportação completa e restauração como requisitos centrais.
2. **Planejamento conectado ao edital:** não apenas horas por matéria, mas cobertura e domínio por tópico, mantendo a navegação `objetivo → matéria → tópico`.
3. **Menos manutenção manual:** registrar uma sessão deve atualizar ciclo, progresso, revisões e estatísticas em uma única ação.
4. **Revisão sustentável:** permitir estratégias simples por intervalo e ajustes por desempenho, sem obrigar todos a usar o mesmo método.
5. **Offline sem surpresas:** fila de sincronização visível, conflitos auditáveis e nenhuma perda silenciosa.
6. **Self-hosting de verdade:** instalação, atualização, saúde, backup e recuperação tratados como produto, não apenas como arquivos para desenvolvedores.

## 3. Problema

Concurseiros precisam administrar grande volume de conteúdo por meses ou anos. Planilhas oferecem controle, mas exigem manutenção; ferramentas comerciais reduzem essa carga, porém mantêm dados e continuidade do serviço sob controle de terceiros. Muitas soluções também fragmentam horas, edital, questões e revisões, obrigando o estudante a atualizar várias telas para entender seu próximo passo.

O problema central é: **o estudante gasta energia decidindo e registrando o estudo, mas ainda não enxerga com confiança qual ação gera mais progresso agora.**

## 4. Visão e proposta de valor

### Visão

Ser o painel pessoal e soberano do estudante: simples para registrar uma sessão hoje, completo para orientar uma preparação de longo prazo e previsível para hospedar por conta própria.

### Promessa

“Seus estudos, seus dados, seu servidor — e sempre uma próxima ação clara.”

### Princípios de produto

1. **Estudar antes de organizar:** ações rotineiras devem exigir poucos campos e poucos cliques.
2. **Dados portáveis:** tudo que entra pode sair em formato documentado.
3. **Offline é estado normal:** o produto deve continuar útil sem rede.
4. **Explicável:** recomendações mostram quais pesos e regras produziram o resultado.
5. **Sem culpa artificial:** atrasos replanejam a fila; não destroem o plano nem punem o usuário.
6. **Privado por padrão:** sem anúncios, rastreadores ou chamadas a terceiros na configuração padrão.
7. **Evolução segura:** atualizações preservam dados e têm caminho de rollback documentado.

## 5. Público e jobs to be done

### Persona primária — estudante individual de longo prazo

- prepara-se para um ou mais concursos;
- estuda em blocos variáveis e alterna computador e celular;
- quer controlar horas líquidas, questões e revisões;
- possui noções básicas de Docker ou recebe ajuda de quem administra o servidor;
- valoriza privacidade, baixo custo recorrente e independência.

### Persona secundária — administrador da instância

- instala para si, família ou pequeno grupo;
- precisa atualizar, fazer backup e diagnosticar a aplicação;
- não deve precisar conhecer o código-fonte para operar o serviço.

### Jobs principais

- Quando tenho tempo para estudar, quero receber uma próxima ação clara para começar sem indecisão.
- Quando termino uma sessão, quero registrar esforço e resultado uma única vez para atualizar todo meu plano.
- Quando erro questões, quero localizar os tópicos fracos para redistribuir meu tempo.
- Quando perco um dia, quero que o ciclo continue sem refazer um calendário inteiro.
- Quando troco de aparelho ou servidor, quero restaurar todo meu histórico sem depender de fornecedor.

## 6. Alternativas consideradas

| Abordagem | Vantagens | Limitações | Decisão |
|---|---|---|---|
| **PWA + servidor modular monolítico + PostgreSQL** | Uma base de interface; instalável; boa experiência mobile; sincroniza dispositivos; operação simples | Offline e notificações variam entre navegadores; exige cuidado com conflitos | **Recomendada** |
| Web + apps nativos desde o início | Melhor integração com SO e notificações | Três superfícies, publicação em lojas e custo elevado antes de validar o produto | Pós-MVP, somente com evidência |
| Aplicação exclusivamente local, sem servidor | Privacidade e instalação individual simples | Sincronização e uso multi-dispositivo ficam frágeis; não atende bem pequenos grupos | Não escolhida |

## 7. Escopo do MVP

### 7.1 Objetivo de estudo e edital

- criar objetivos, por exemplo “Banco do Brasil — Escriturário 2027”;
- definir data da prova opcional, banca, cargo, descrição e estado: ativo, pausado ou concluído;
- cadastrar matérias e tópicos hierárquicos por objetivo;
- informar peso, número estimado de questões, dificuldade percebida e domínio atual;
- ordenar, arquivar e mover tópicos sem perder histórico;
- importar a estrutura por CSV/JSON usando modelo documentado;
- mostrar cobertura: não iniciado, em estudo, revisando e dominado.

### 7.2 Sessões e cronômetro

- iniciar rapidamente pelo próximo item do ciclo ou escolher matéria/tópico;
- iniciar, pausar, retomar, descartar e concluir cronômetro;
- persistir o cronômetro localmente para sobreviver a recarga, fechamento e perda de rede;
- impedir dois cronômetros simultâneos para o mesmo usuário;
- registrar sessão manual com início e duração líquida;
- classificar tipo: teoria, lei seca, questões, revisão, simulado ou personalizado;
- registrar anotações, questões respondidas, corretas e anuladas;
- editar ou excluir com confirmação, lixeira recuperável e trilha mínima de alterações.

### 7.3 Ciclo e próxima ação

- criar ciclo manual com itens de matéria ou tópico e duração-alvo;
- sugerir distribuição explicável usando peso da prova, dificuldade, domínio e desempenho recente;
- permitir ao usuário alterar qualquer sugestão;
- avançar para o próximo item ao concluir ou pular uma sessão;
- manter a fila por sequência, não por agenda rígida;
- recalcular sem apagar o histórico;
- mostrar “por que isto vem agora?” em linguagem simples.

### 7.4 Revisões

- criar revisões ao concluir uma sessão ou diretamente no tópico;
- oferecer predefinições editáveis, como 1/7/30 dias, e dia fixo semanal;
- apresentar caixas “hoje”, “atrasadas” e “próximas”;
- concluir, adiar, pular ou iniciar sessão a partir da revisão;
- limitar a carga diária e redistribuir atrasos sem gerar avalanche;
- relacionar revisão, sessão de origem e tópico;
- no MVP, usar regras determinísticas. Adaptação algorítmica avançada fica posterior.

### 7.5 Histórico e análise

- painel inicial com próxima ação, revisões, meta semanal e resumo recente;
- calendário/heatmap de sessões;
- histórico pesquisável por período, objetivo, matéria, tópico e tipo;
- gráficos de horas líquidas, distribuição por matéria/tipo e evolução temporal;
- desempenho em questões por matéria e tópico, sempre exibindo tamanho da amostra;
- progresso de cobertura do edital separado de horas estudadas;
- comparação entre tempo planejado e realizado;
- exportar a visão filtrada em CSV.

### 7.6 PWA e offline

- interface responsiva e instalável em navegadores compatíveis;
- leitura de dados recentes e operações principais offline;
- armazenamento local das mutações pendentes;
- indicador visível: sincronizado, sincronizando, offline ou com conflito;
- sincronização automática quando possível e botão “sincronizar agora”;
- não depender exclusivamente da API de Background Sync, que não é uniforme entre plataformas;
- conflitos nunca devem descartar dados silenciosamente: preservar versões e solicitar decisão quando não houver regra segura;
- notificações de revisões são opcionais e usam capacidades do dispositivo quando disponíveis; a aplicação continua funcional sem elas.

### 7.7 Self-hosting e administração

- imagem de contêiner versionada e `docker-compose.yml` de referência;
- dois serviços obrigatórios no MVP: aplicação e PostgreSQL;
- frontend e API distribuídos na mesma imagem da aplicação;
- configuração por variáveis de ambiente e arquivo `.env.example` sem segredos reais;
- primeiro usuário criado como administrador por fluxo de bootstrap seguro;
- cadastro público desativado por padrão; administrador pode convidar usuários;
- SMTP opcional; sem SMTP, recuperação de senha é administrada localmente;
- endpoints de liveness/readiness e tela com versão, banco e migrações;
- migrações automáticas compatíveis com a versão, com instruções de backup pré-upgrade;
- comando documentado de backup e restauração, com teste automatizado de restauração;
- exportação por usuário e exportação integral administrativa em JSON versionado;
- nenhuma dependência de CDN, fonte, analytics ou API externa na configuração padrão;
- compatibilidade documentada com proxy reverso e HTTPS, sem incluir proxy obrigatório no Compose mínimo;
- retenção de logs configurável e logs estruturados sem conteúdo sensível.

## 8. Melhorias em relação ao produto de referência

| Área | Base observada no Aprovado | Estuda Livre |
|---|---|---|
| Dados | Sincronização e backup no serviço central | Instância própria, exportação integral, restore testado e esquema aberto |
| Edital | Matérias e conteúdos | Objetivo independente, hierarquia de tópicos, cobertura e importação |
| Planejamento | Metas e ciclo ponderado | Ciclo explicável, replanejamento leve e próxima ação unificada |
| Revisões | Intervalos e dia semanal | Mesmas opções essenciais, limite diário e redistribuição de atrasos |
| Exercícios | Quantidade, acertos e gráfico por matéria | Resultado por tópico, amostra visível e impacto opcional no ciclo |
| Exclusões | Algumas exclusões públicas são irreversíveis | Lixeira e recuperação antes da remoção definitiva |
| Offline | Aplicativos móveis com sincronização central | PWA offline-first, fila e conflitos visíveis |
| Operação | SaaS mantido pelo fornecedor | Docker Compose, health checks, upgrades e recuperação documentados |
| Privacidade | Conta central e publicidade no plano gratuito | Sem anúncios e sem telemetria externa por padrão |

O objetivo não é copiar layout, textos, marca, ícones, código ou ativos do Aprovado. O Estuda Livre resolverá problemas semelhantes com identidade, implementação e experiência próprias.

## 9. Fluxos críticos

### Fluxo A — primeiro valor em menos de três minutos

1. Usuário entra pela primeira vez.
2. Cria objetivo e uma matéria, ou importa um CSV.
3. Escolhe duração-alvo ou aceita o padrão.
4. Inicia o cronômetro.
5. Conclui com tipo, tópico e observação opcionais.
6. Vê a sessão no painel e recebe a próxima ação.

### Fluxo B — sessão de questões atualiza o plano

1. Usuário inicia “Questões — Pontuação”.
2. Conclui com 30 respondidas, 21 corretas e 1 anulada.
3. Sistema calcula 72,4% sobre 29 questões válidas.
4. Histórico e painel do tópico são atualizados.
5. Usuário pode criar revisão e aceitar ou rejeitar ajuste sugerido de prioridade.

### Fluxo C — estudo offline

1. Usuário abre a PWA sem rede e vê dados locais e estado offline.
2. Inicia e conclui uma sessão.
3. A sessão aparece imediatamente com marca “pendente”.
4. Ao recuperar conexão, a fila sincroniza.
5. Se houver conflito, ambas as versões são preservadas e o usuário resolve pela interface.

### Fluxo D — atualização self-hosted

1. Administrador consulta notas da versão e compatibilidade.
2. Executa backup documentado.
3. Atualiza a tag da imagem e sobe os serviços.
4. Readiness só fica saudável após migrações.
5. Em falha, restaura banco e versão anteriores conforme runbook.

## 10. Requisitos funcionais e critérios de aceite

### P0 — necessários para o MVP

| ID | Requisito | Critério de aceite resumido |
|---|---|---|
| FR-01 | Autenticação local | Admin cria convite; usuário entra, encerra sessões e troca senha; cadastro público começa desligado |
| FR-02 | Objetivos, matérias e tópicos | CRUD preserva histórico; arquivamento não apaga sessões; nomes duplicados são tratados explicitamente |
| FR-03 | Cronômetro resiliente | Continua correto após recarga/fechamento e usa timestamps, não contagem somente em memória |
| FR-04 | Lançamento manual | Aceita data, duração, objetivo, matéria, tópico e tipo; valida duração positiva e limites configurados |
| FR-05 | Questões | Corretas + anuladas não excedem total; percentual usa apenas questões válidas |
| FR-06 | Ciclo | Mantém ordem e posição; pular não conclui; recalcular não altera sessões passadas |
| FR-07 | Revisões | Cria recorrências configuráveis; conclui/adia/pula; atrasos podem ser redistribuídos |
| FR-08 | Histórico e filtros | Resultado consistente entre lista, totais e exportação CSV para o mesmo filtro |
| FR-09 | Painel | Mostra próxima ação, revisões vencidas/atuais, horas da semana e progresso do objetivo ativo |
| FR-10 | Offline e sync | Criar e concluir sessão offline; após reconexão, sincronizar exatamente uma vez |
| FR-11 | Exportação/restore | Exportar todos os dados do usuário em JSON versionado e importar em instância limpa sem perda |
| FR-12 | Docker Compose | Instância nova fica saudável com procedimento documentado e apenas variáveis obrigatórias |
| FR-13 | Backup | Backup restaurado em ambiente limpo passa verificação automatizada de contagens e integridade |
| FR-14 | Administração | Admin vê versão/saúde, gerencia convites e desativa usuário sem acessar telas pessoais dele por padrão |

### P1 — primeira evolução após o MVP

- importador de editais com assistente de mapeamento de colunas;
- metas flexíveis por semana e comparação planejado × realizado;
- web push quando a instalação e o navegador suportarem;
- integrações via webhooks e API tokens com escopos;
- OIDC para instâncias que já usam um provedor de identidade;
- presets compartilháveis de ciclos e revisões;
- anexos ou links de materiais por tópico;
- importadores dedicados somente para formatos públicos e autorizados;
- algoritmo adaptativo opcional de revisão, com parâmetros e justificativa visíveis.

### P2 — hipóteses que exigem validação

- aplicativo nativo;
- assistência de IA com provedor local ou chave do usuário;
- extração automática de edital em PDF;
- colaboração entre aluno e mentor;
- biblioteca comunitária de editais;
- gamificação além de indicadores discretos de consistência.

## 11. Fora de escopo do MVP

- hospedar cursos, PDFs ou videoaulas;
- banco próprio de questões e correção de alternativas;
- scraping de plataformas ou acesso a APIs privadas;
- pagamentos, planos ou marketplace;
- rede social, ranking público ou competição;
- chat, tutor de IA ou geração automática de conteúdo;
- múltiplas organizações/tenants SaaS na mesma instância;
- aplicativos publicados nas lojas;
- alta disponibilidade ou cluster distribuído.

## 12. Requisitos não funcionais

### Usabilidade e acessibilidade

- mobile-first a partir de 360 px, sem impedir uso completo em desktop;
- tema claro, escuro e preferência do sistema;
- navegação por teclado, foco visível, rótulos de formulário e contraste WCAG 2.2 AA nas jornadas críticas;
- iniciar sessão em até dois gestos a partir do painel quando o ciclo estiver configurado;
- nenhuma cor será a única forma de comunicar estado.

### Desempenho

- conteúdo principal do painel utilizável em até 2,5 s em dispositivo móvel intermediário e rede doméstica, após autenticação;
- resposta p95 inferior a 500 ms para operações comuns em uma instância de até 25 usuários, excluindo primeira carga e exportações;
- ações offline refletem na interface em até 100 ms;
- 10 anos de sessões de um usuário não devem exigir paginação manual para calcular totais agregados.

### Confiabilidade e integridade

- gravações idempotentes usam identificadores gerados no cliente;
- valores de tempo são armazenados em UTC e exibidos no fuso do usuário;
- duração líquida é armazenada como valor derivado auditável das transições do cronômetro;
- soft delete com retenção padrão de 30 dias para dados de estudo;
- migrações são transacionais quando o banco permitir;
- nenhuma atualização suportada pode exigir perda de dados.

### Segurança e privacidade

- senhas com algoritmo moderno de derivação e parâmetros atualizáveis;
- cookies de sessão `HttpOnly`, `Secure` sob HTTPS e `SameSite` adequado;
- proteção contra CSRF, rate limit de autenticação e invalidação de sessões;
- autorização por proprietário em toda consulta de dados de estudo;
- segredos nunca aparecem em logs, exportações ou tela de diagnóstico;
- exportações exigem reautenticação e expiram;
- imagem de contêiner executa como usuário sem privilégios e publica SBOM;
- política de reporte de vulnerabilidade e processo de atualização de dependências.

### Operabilidade

- tags imutáveis de versão; `latest` não é recomendado em produção;
- logs JSON para stdout e correlação por request ID;
- métricas operacionais locais opcionais, sem transmitir dados;
- documentação cobre instalação, atualização, backup, restore, proxy e solução de problemas;
- suporte às arquiteturas `amd64` e `arm64` é meta de release.

## 13. Arquitetura proposta

Esta seção orienta o produto, mas não substitui um ADR técnico.

```text
Navegador / PWA
  ├─ cache da aplicação
  ├─ IndexedDB: leituras recentes + fila de mutações
  └─ HTTPS / API versionada
          ↓
Aplicação modular monolítica
  ├─ identidade e administração
  ├─ catálogo de objetivos/editais
  ├─ sessões e cronômetro
  ├─ planejamento e ciclo
  ├─ revisões
  ├─ análises e exportações
  └─ sincronização
          ↓
PostgreSQL
```

Decisões:

- **modular monolítica:** reduz a carga de operação self-hosted e mantém limites internos que permitem extrair componentes no futuro;
- **uma imagem de aplicação:** serve frontend e API, evitando versões incompatíveis;
- **PostgreSQL como único estado obrigatório no servidor:** simplifica backup e recuperação;
- **jobs persistidos no banco:** revisões e exportações não justificam uma fila externa no MVP;
- **PWA com fila local:** a interface responde offline e envia mutações idempotentes depois;
- **API versionada:** suporta a PWA e futuras integrações sem acoplamento a telas;
- **sem Redis, S3 ou broker obrigatórios no MVP:** componentes opcionais só entram com necessidade comprovada.

Stack de referência, não contratual: frontend TypeScript/React, backend Java/Spring Boot e PostgreSQL. A escolha final deve considerar experiência da equipe, tamanho da imagem, migrações e suporte multiplataforma.

## 14. Modelo de domínio inicial

| Entidade | Papel |
|---|---|
| `User` | Identidade local, preferências, fuso e papel administrativo |
| `StudyGoal` | Concurso/prova/projeto com estado e data-alvo |
| `Subject` | Matéria pertencente a um objetivo |
| `Topic` | Tópico hierárquico com peso, dificuldade, domínio e cobertura |
| `StudySession` | Intervalo de estudo manual ou cronometrado, com duração líquida |
| `TimerEvent` | Início, pausa, retomada e conclusão auditáveis |
| `ExerciseResult` | Total, corretas, anuladas e fonte opcional de uma sessão |
| `StudyCycle` | Estratégia ativa e regras de ordenação |
| `CycleItem` | Item, posição, duração-alvo e pesos explicáveis |
| `ReviewPlan` | Regra de revisão associada a tópico/sessão |
| `ReviewOccurrence` | Ocorrência prevista, concluída, adiada ou pulada |
| `SyncMutation` | Operação idempotente e estado de sincronização |
| `AuditEntry` | Alterações relevantes e recuperação de exclusões |

Regras importantes:

- toda entidade de estudo pertence a um `User`; não há isolamento SaaS por tenant no MVP;
- um objetivo pode ser arquivado sem apagar matérias e sessões;
- o histórico de uma sessão mantém os rótulos necessários para relatórios mesmo após reorganização do edital;
- questões anuladas não entram no denominador de desempenho;
- domínio e prioridade podem receber sugestões, mas nunca são alterados silenciosamente pelo algoritmo;
- exclusão definitiva respeita dependências e só ocorre após retenção ou confirmação administrativa explícita.

## 15. Métricas de sucesso

As métricas são calculadas dentro da própria instância. Telemetria para mantenedores é opt-in e fora do MVP.

### Ativação

- instalação nova operacional em até 10 minutos, excluindo download de imagem;
- mediana entre primeiro login e primeira sessão concluída menor que 3 minutos;
- ao menos 80% dos testadores conseguem criar objetivo e iniciar sessão sem documentação externa.

### Uso e valor

- pelo menos 70% dos usuários-piloto ativos concluem sessões em três semanas consecutivas;
- pelo menos 60% das sessões iniciadas pelo cronômetro são concluídas sem correção manual;
- 90% dos usuários-piloto conseguem responder onde estão fracos por tópico usando o painel;
- a próxima ação é aceita ou deliberadamente substituída em 80% das visitas ao painel com ciclo ativo.

### Qualidade operacional

- zero perda conhecida de sessão em testes offline, concorrentes e de upgrade;
- 100% dos releases passam backup + restore automatizado da versão anterior suportada;
- nenhuma chamada de rede para terceiros em teste da configuração padrão;
- exportação e reimportação preservam 100% das entidades e relações suportadas.

## 16. Estratégia de entrega

### Marco 0 — fundação operável

Compose, autenticação, PostgreSQL, migrações, health checks, CI, backup/restore e esqueleto PWA. O marco só termina quando uma atualização e uma restauração forem exercitadas.

### Marco 1 — registrar

Objetivos, matérias, tópicos, cronômetro resiliente, sessão manual, histórico e exportação básica. Primeiro release utilizável diariamente.

### Marco 2 — decidir

Ciclo manual, sugestão explicável, próxima ação e meta semanal. O usuário passa a abrir o produto para saber o que fazer, não apenas para registrar.

### Marco 3 — aprender

Questões por tópico, revisões, painéis de desempenho e cobertura. Uma conclusão atualiza os demais módulos.

### Marco 4 — desconectar e recuperar

Operações offline completas, resolução de conflitos, exportação integral versionada, lixeira e teste de recuperação de desastre. Release candidato ao MVP.

Cada marco deve ser uma fatia vertical utilizável, com migrações, testes, documentação e critérios de operação incluídos.

## 17. Estratégia de testes

- testes unitários para ponderação do ciclo, cálculos de desempenho, intervalos e tempo líquido;
- testes de propriedade para invariantes de questões, durações e idempotência;
- integração real com PostgreSQL para constraints, migrações e concorrência;
- testes de contrato da API e compatibilidade com uma versão anterior suportada da PWA;
- E2E das quatro jornadas críticas em desktop e viewport móvel;
- cenários offline: fechamento abrupto, reconexão, reenvio, mutações fora de ordem e conflito;
- testes de autorização garantindo que um usuário nunca obtenha dados de outro;
- teste automatizado de upgrade com cópia anonimizada de um banco representativo;
- exercício de backup/restore em ambiente limpo a cada release;
- auditoria de acessibilidade automatizada e manual das jornadas críticas.

## 18. Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| Escopo crescer para curso/IA/banco de questões | MVP atrasado e interface pesada | Manter fora de escopo; validar depois do uso diário do núcleo |
| Offline introduzir conflitos e perda | Quebra de confiança | IDs no cliente, idempotência, fila visível, preservação de versões e suíte de caos de sync |
| Self-hosting ser difícil de operar | Abandono da instalação | Dois serviços obrigatórios, runbooks, health checks e restore testado |
| Algoritmo mandar no usuário | Rejeição e sensação de opacidade | Sugestões explicáveis, editáveis e nunca aplicadas silenciosamente |
| Revisões acumularem demais | Desmotivação | Limite diário, adiamento em lote e redistribuição de atraso |
| Métricas incentivarem horas vazias | Otimização errada | Separar tempo, cobertura e desempenho; evitar ranking e culpa |
| Confusão de marca ou cópia indevida | Risco jurídico e reputacional | Nome/identidade próprios; não copiar código, textos, ativos ou aparência |
| Atualização quebrar banco | Perda de dados | Compatibilidade declarada, backup prévio, migrações testadas e rollback documentado |

## 19. Decisões assumidas e pontos abertos

### Assumido neste PRD

- produto individual-first, mas uma instância pode ter vários usuários isolados;
- PWA é a única interface do MVP;
- Docker Compose é o caminho oficial de instalação;
- PostgreSQL é obrigatório;
- português brasileiro é o idioma inicial;
- open source e sem recursos pagos bloqueados, com licença a definir antes do primeiro release público.

### Decisões a validar antes da implementação

1. Nome definitivo e licença (`AGPL-3.0` favorece reciprocidade em serviços; `Apache-2.0` favorece adoção permissiva).
2. Stack final e convenções do repositório.
3. Profundidade máxima de tópicos ou hierarquia sem limite prático.
4. Se o MVP precisa importar algum formato existente além do CSV/JSON próprio.
5. Política de suporte a versões e janela de migração.
6. Se convites por email entram no MVP ou somente links/tokens administrados localmente.

## 20. Definition of Done do MVP

O MVP estará pronto quando:

- um usuário instalar a aplicação seguindo somente a documentação publicada;
- criar/importar um edital, executar sessões por duas semanas, seguir ciclo e concluir revisões em desktop e celular;
- trabalhar offline e sincronizar sem duplicação ou perda;
- localizar tempo, cobertura e desempenho por tópico;
- exportar seus dados, apagar a conta e restaurar a exportação em uma instância limpa;
- um administrador atualizar a aplicação e restaurar backup usando os runbooks;
- os critérios P0, segurança, acessibilidade e testes de recuperação estiverem aprovados;
- a instalação padrão não contiver anúncios, telemetria ou dependências externas ocultas.

## 21. Próximo passo recomendado

Validar este PRD com 3 a 5 concurseiros que usem Aprovado, planilha ou ferramenta semelhante. A entrevista deve testar principalmente: rapidez do registro, utilidade da próxima ação, nível ideal de detalhe do edital, tolerância ao modelo de revisões e capacidade real de instalar/operar a solução. Depois da validação, transformar os marcos em issues verticais, começando por um tracer bullet que percorra login → matéria → cronômetro → sessão → histórico → backup.
