# PRD — MVP Estuda Livre

Status: `ready-for-agent`  
Data: 15 de julho de 2026  
Produto: Estuda Livre  
Distribuição: open source e self-hosted

Este é o PRD autoritativo do MVP. O PRD de 13 de julho permanece como documento de pesquisa e visão ampliada; quando houver divergência de escopo, este documento prevalece.

## Problem Statement

Concurseiros e outros estudantes de longo prazo precisam alternar matérias, registrar o tempo realmente estudado, revisar conteúdos em datas adequadas e acompanhar o aproveitamento em exercícios. Hoje essas informações costumam ficar espalhadas entre cronômetros, planilhas, calendários e plataformas comerciais. Isso aumenta o trabalho administrativo e dificulta responder à pergunta mais importante: “o que devo estudar agora?”.

Ferramentas comerciais como o Aprovado validam o valor de ciclos, sessões, revisões e acompanhamento de exercícios, mas mantêm a aplicação e os dados sob controle de terceiros. O estudante que deseja hospedar a própria ferramenta precisa de uma alternativa simples de instalar, previsível, multiusuário e independente de serviços externos.

O MVP deve resolver o núcleo desse problema sem se transformar em curso, banco de questões, calendário semanal ou sistema competitivo. Ele deve permitir organizar matérias e conteúdos, criar um ciclo manual ou sugerido, executar sessões, distribuir automaticamente o tempo estudado, programar revisões e calcular a taxa de acerto.

## Solution

O Estuda Livre será uma aplicação web self-hosted, entregue em Docker Compose, com Spring Boot, React/TypeScript e PostgreSQL. O frontend e a API serão publicados no mesmo container da aplicação, mantendo autenticação por cookie no mesmo domínio e reduzindo a carga operacional.

Cada usuário poderá cadastrar matérias e conteúdos, manter vários ciclos e escolher um único ciclo ativo. Um ciclo poderá ser criado manualmente ou sugerido a partir da quantidade de questões, peso e dificuldade de cada matéria. A sugestão determinará automaticamente a duração total, a distribuição de tempo e a ordem intercalada das etapas, mas poderá ser transformada em um ciclo personalizado e editada livremente.

O ciclo será contínuo e executado em voltas. Sessões cronometradas, manuais e de revisão contarão como estudo e creditarão tempo às etapas da mesma matéria. Sessões parciais serão acumuladas; excedentes poderão preencher outras etapas da matéria na mesma volta. Ao concluir a volta, o sistema iniciará automaticamente a próxima e preservará o histórico.

Ao finalizar uma sessão com conteúdo, o usuário poderá criar uma sequência de revisões em 1, 7, 30, 60, 90 e 120 dias. O mesmo fechamento permitirá registrar questões realizadas e questões corretas, calculando automaticamente a taxa de acerto. Tudo será editável porque o produto é uma ferramenta pessoal; projeções de ciclo e métricas serão recalculadas quando necessário.

## User Stories

1. Como visitante de uma instalação nova, quero criar a primeira conta, para que eu possa iniciar o uso sem configuração administrativa prévia.
2. Como operador da instância, quero controlar novos cadastros por configuração, para que minha instalação não fique aberta ao público acidentalmente.
3. Como usuário, quero entrar e sair com e-mail e senha, para que meus dados permaneçam privados.
4. Como usuário, quero alterar minha senha, para que eu possa manter minha conta segura.
5. Como operador, quero gerar um link temporário de recuperação pelo servidor, para que uma senha possa ser redefinida sem SMTP.
6. Como usuário, quero ter um fuso horário próprio, para que sessões e revisões apareçam nas datas corretas para mim.
7. Como usuário, quero que meus dados sejam isolados dos demais usuários, para que ninguém veja ou altere meu planejamento.
8. Como usuário, quero cadastrar uma matéria, para que eu possa incluí-la nos meus estudos.
9. Como usuário, quero editar ou arquivar uma matéria, para que meu catálogo acompanhe mudanças na preparação.
10. Como usuário, quero cadastrar conteúdos dentro de uma matéria, para que eu possa registrar exatamente o assunto estudado.
11. Como usuário, quero editar ou arquivar conteúdos, para que a organização permaneça útil sem apagar o histórico.
12. Como usuário, quero impedir conteúdos duplicados na mesma matéria, para que métricas e revisões não sejam fragmentadas por acidente.
13. Como usuário, quero criar vários ciclos, para que eu possa guardar planejamentos diferentes.
14. Como usuário, quero manter somente um ciclo ativo, para que o sistema saiba inequivocamente onde creditar meu estudo.
15. Como usuário, quero desativar um ciclo e ativar outro, para que eu possa mudar de foco.
16. Como usuário, quero reativar um ciclo e retomar sua volta do ponto em que parei, para que a troca de foco não apague progresso.
17. Como usuário, quero criar um ciclo sugerido informando questões, peso e dificuldade das matérias, para que o sistema distribua meu tempo automaticamente.
18. Como usuário, quero entender os dados usados na sugestão, para que o planejamento seja explicável.
19. Como usuário, quero que todas as matérias recebam um tempo mínimo, para que nenhuma seja eliminada pela ponderação.
20. Como usuário, quero que matérias prioritárias apareçam mais vezes no ciclo, para que seu tempo seja distribuído sem blocos excessivamente longos.
21. Como usuário, quero que matérias iguais sejam intercaladas quando possível, para que o ciclo preserve variedade.
22. Como usuário, quero receber metas em múltiplos de cinco minutos, para que os blocos sejam simples de compreender.
23. Como usuário, quero criar um ciclo personalizado, para que eu possa definir livremente etapas, ordem e duração.
24. Como usuário, quero editar diretamente um ciclo sugerido, para que ele se torne um planejamento personalizado adequado à minha rotina.
25. Como usuário, quero regenerar uma sugestão mediante confirmação, para que minhas alterações manuais não sejam sobrescritas silenciosamente.
26. Como usuário, quero receber um alerta, e não um bloqueio, para etapas personalizadas acima de três horas, para que a decisão final continue sendo minha.
27. Como usuário, quero editar o ciclo atual sem criar uma versão nova visível, para que ajustes cotidianos sejam simples.
28. Como usuário, quero que uma edição do ciclo redistribua o tempo da volta atual, para que o progresso já conquistado seja preservado.
29. Como usuário, quero que voltas concluídas guardem o planejamento utilizado, para que o histórico continue compreensível após mudanças.
30. Como usuário, quero visualizar a etapa atual, a meta, o tempo realizado e o restante, para que eu saiba o que estudar agora.
31. Como usuário, quero iniciar uma sessão a partir da etapa do ciclo, para que o tempo seja associado à matéria correta.
32. Como usuário, quero selecionar opcionalmente um conteúdo ao iniciar uma sessão comum, para que eu possa estudar a matéria mesmo sem detalhar o assunto.
33. Como usuário, quero registrar no máximo um conteúdo por sessão, para que métricas e revisões continuem precisas.
34. Como usuário, quero iniciar, pausar, retomar e finalizar um cronômetro, para que apenas o tempo líquido seja contabilizado.
35. Como usuário, quero recuperar uma sessão após atualizar ou fechar o navegador, para que eu não perca o cronômetro.
36. Como usuário, quero ter somente uma sessão em andamento, para que dois cronômetros não contem o mesmo período.
37. Como usuário, quero realizar sessões parciais, para que eu possa completar uma etapa em vários momentos.
38. Como usuário, quero que o excedente de uma sessão preencha etapas futuras da mesma matéria e volta, para que todo o estudo realizado conte no ciclo.
39. Como usuário, quero que uma etapa antecipadamente preenchida seja pulada quando chegar sua vez, para que o sistema não cobre o mesmo tempo duas vezes.
40. Como usuário, quero que excedentes sem outra etapa da matéria permaneçam apenas nas métricas, para que não atravessem para a próxima volta.
41. Como usuário, quero que uma nova volta comece automaticamente após concluir a anterior, para que o ciclo seja contínuo.
42. Como usuário, quero consultar o histórico das voltas, para que eu acompanhe minha consistência ao longo do tempo.
43. Como usuário, quero registrar uma sessão manual concluída, para que estudos feitos sem cronômetro também contem.
44. Como usuário, quero corrigir a duração medida antes de finalizar, para que um cronômetro esquecido não distorça meus dados.
45. Como usuário, quero visualizar tempo medido e tempo efetivo, para que a correção fique transparente para mim.
46. Como usuário, quero editar ou excluir qualquer sessão posteriormente, para que eu continue dono dos meus registros.
47. Como usuário, quero que alterações históricas recalculem métricas e voltas afetadas, para que os dados derivados permaneçam coerentes.
48. Como usuário, quero que uma sessão sobre matéria fora do ciclo conte nas métricas, para que nenhum estudo seja perdido.
49. Como usuário, quero informar quantas questões realizei ao finalizar uma sessão, para que eu acompanhe o volume praticado.
50. Como usuário, quero informar quantas questões acertei, para que o sistema calcule minha taxa de acerto.
51. Como usuário, quero ser impedido de registrar mais acertos do que questões, para que o resultado permaneça válido.
52. Como usuário, quero finalizar sem exercícios, para que sessões teóricas não exijam dados artificiais.
53. Como usuário, quero editar posteriormente os números de exercícios, para que erros de digitação possam ser corrigidos.
54. Como usuário, quero visualizar taxa de acerto por sessão, conteúdo e matéria, para que eu identifique pontos fracos.
55. Como usuário, quero receber sugestões baseadas em baixo desempenho, para que eu considere ajustar a dificuldade ou regenerar o ciclo.
56. Como usuário, quero confirmar qualquer mudança sugerida por desempenho, para que o algoritmo não altere meu ciclo silenciosamente.
57. Como usuário, quero agendar revisões ao finalizar uma sessão com conteúdo, para que o calendário nasça do estudo real.
58. Como usuário, quero que a opção de agendar revisões venha ativada por padrão, para que eu não esqueça essa etapa.
59. Como usuário, quero receber revisões em 1, 7, 30, 60, 90 e 120 dias, para que eu siga a estratégia definida para o MVP.
60. Como usuário, quero manter apenas um plano de revisão ativo por conteúdo, para que sessões repetidas não dupliquem a fila.
61. Como usuário, quero ver revisões de hoje, futuras e atrasadas, para que eu priorize o que precisa ser revisto.
62. Como usuário, quero iniciar uma sessão a partir de uma revisão, para que matéria e conteúdo já venham preenchidos.
63. Como usuário, quero que o tempo de revisão conte no ciclo da mesma matéria, para que revisão também seja reconhecida como estudo.
64. Como usuário, quero que uma revisão atrasada preserve as demais datas originais, para que um atraso não desloque toda a sequência.
65. Como usuário, quero que uma única sessão consolide várias revisões atrasadas do mesmo conteúdo, para que eu não repita o assunto artificialmente no mesmo dia.
66. Como usuário, quero que a ocorrência atrasada mais recente seja concluída e as anteriores sejam marcadas como ignoradas, para que o histórico diferencie atraso de realização.
67. Como usuário, quero editar, cancelar ou reagendar um plano de revisão, para que ele tenha vida própria após sua criação.
68. Como usuário, quero que editar ou excluir a sessão original não apague revisões, para que meu histórico não desapareça em cascata.
69. Como usuário, quero uma interface responsiva para desktop e celular, para que eu consiga iniciar e finalizar estudos em qualquer tela.
70. Como operador, quero subir aplicação e PostgreSQL com Docker Compose, para que a instalação self-hosted seja simples.
71. Como operador, quero aplicar migrações automaticamente e verificar a saúde da aplicação, para que atualizações sejam previsíveis.
72. Como operador, quero fazer backup e restaurar o PostgreSQL por um procedimento documentado, para que os dados não dependam do container.

## Implementation Decisions

### Escopo de domínio

- O MVP contém as features `identity`, `subject`, `studycycle`, `studysession` e `review`.
- `Subject` representa uma matéria pertencente a um usuário. `Content` representa um único conteúdo dentro de uma matéria.
- `StudyCycle` é o planejamento atual. `StudyCycleStage` representa uma aparição ordenada de uma matéria com duração-alvo. `StudyCycleRun` representa uma volta do ciclo.
- `StudySession` é a fonte da verdade do estudo realizado. Ela possui uma matéria, no máximo um conteúdo, origem `CYCLE`, `REVIEW` ou `FREE`, duração medida, duração efetiva e estado.
- `ExerciseResult` pertence à feature `studysession` e existe no máximo uma vez por sessão. Não será criada uma feature isolada de exercícios no MVP.
- `ReviewPlan` representa a sequência ativa de um conteúdo. `ReviewOccurrence` representa cada data individual, com intervalo, data agendada, estado e eventual conclusão.
- Todas as entidades de negócio pertencem direta ou indiretamente a um usuário. Consultas nunca aceitam apenas o identificador do recurso sem verificar o proprietário.
- Matérias e conteúdos com histórico são arquivados em vez de removidos fisicamente. Sessões podem ser excluídas pelo usuário e provocam recálculo das projeções dependentes.

### Planejador do ciclo

- Um usuário pode cadastrar vários ciclos, mas somente um pode estar ativo. Trocar de ciclo torna o anterior inativo; reativá-lo retoma sua volta incompleta.
- Ciclos têm modo `SUGGESTED` ou `CUSTOM`. Editar diretamente etapas de um ciclo sugerido muda seu modo para personalizado.
- No modo sugerido, a duração total é duas horas por matéria, limitada ao intervalo de 10 a 30 horas.
- A prioridade base de uma matéria é a quantidade de questões multiplicada pelo peso. O resultado é multiplicado pelo fator de dificuldade: fácil 1,00; média 1,25; difícil 1,50.
- Cada matéria recebe primeiro uma hora mínima. O restante da duração é distribuído proporcionalmente à prioridade.
- Metas são arredondadas para múltiplos de cinco minutos. Um método de maiores restos reconcilia o arredondamento para preservar exatamente a duração total calculada.
- Uma alocação acima de três horas é dividida em aparições equilibradas. A quantidade de aparições é o teto da divisão da carga por três horas; os minutos são distribuídos tão uniformemente quanto possível.
- As aparições são ordenadas por round-robin ponderado, priorizando a maior carga restante e evitando matérias iguais consecutivas quando existir alternativa.
- No modo personalizado, cada etapa exige duração positiva em múltiplos de cinco minutos. Não existe máximo bloqueante; etapas acima de três horas mostram aviso.
- Alterar entradas ou etapas recalcula a volta em andamento e redistribui os créditos já recebidos por matéria. Voltas concluídas preservam um snapshot do planejamento usado.
- O desempenho em exercícios pode produzir uma sugestão de dificuldade, mas nunca altera ou regenera o ciclo sem confirmação.

### Voltas, créditos e recálculo

- A volta é contínua. Concluir a última etapa encerra a volta e cria automaticamente a próxima.
- Sessões parciais acumulam duração na primeira etapa incompleta da mesma matéria.
- O excedente é aplicado às próximas etapas da mesma matéria, somente dentro da mesma volta. O restante que não couber permanece nas métricas, mas não avança a volta seguinte.
- Etapas preenchidas antecipadamente são consideradas concluídas e ignoradas automaticamente quando alcançadas pela ordem do ciclo.
- Sessões de revisão e sessões livres também geram crédito quando sua matéria pertence ao ciclo ativo.
- O progresso do ciclo é uma projeção recalculável a partir das sessões e de suas alocações. Não será adotado Event Sourcing.
- Editar ou excluir uma sessão recalcula cronologicamente as alocações da volta afetada e das voltas posteriores do mesmo ciclo. Snapshots de planejamento permanecem associados às respectivas voltas.
- Editar o planejamento recalcula somente a volta atual; não substitui snapshots de voltas concluídas.

### Sessões e exercícios

- O cronômetro segue `ACTIVE`, `PAUSED` e `FINISHED`. Apenas uma sessão `ACTIVE` ou `PAUSED` pode existir por usuário; uma restrição no PostgreSQL reforça a regra.
- O backend é a fonte oficial do tempo. Início, pausas e retomadas são persistidos; recarregar o navegador não perde a sessão.
- Na finalização, o usuário confirma a duração efetiva. A duração medida permanece armazenada para referência.
- Sessões manuais são criadas diretamente como finalizadas, com data, duração, matéria, conteúdo opcional, anotações e exercício opcional.
- O usuário pode editar ou excluir sessões finalizadas. Services específicos recalculam métricas, créditos e voltas afetadas.
- A taxa de acerto é `questões corretas / questões realizadas × 100`. Se nenhuma questão for informada, não existe `ExerciseResult` nem taxa. Questões corretas não podem exceder as realizadas.
- A finalização é idempotente. Uma trava curta na linha da sessão garante que requisições simultâneas não dupliquem exercícios, créditos ou revisões. Entidades mutáveis usam versionamento otimista para detectar edições concorrentes.

### Revisões

- Ao finalizar uma sessão com conteúdo, a interface oferece “Agendar revisões deste conteúdo”, ativado por padrão.
- Só existe um plano ativo por conteúdo. A criação gera ocorrências para 1, 7, 30, 60, 90 e 120 dias, calculadas a partir da data local da sessão.
- As datas permanecem ancoradas no estudo original. Concluir uma ocorrência com atraso não move ocorrências posteriores.
- Quando várias ocorrências do mesmo conteúdo estiverem vencidas, a mais recente é concluída e as anteriores são marcadas como `SKIPPED`.
- Um plano passa a ter vida própria após ser criado. Editar ou excluir a sessão que o originou não altera suas ocorrências; mudanças exigem uma ação explícita no plano.
- Iniciar uma revisão cria uma `StudySession` de origem `REVIEW`, com matéria e conteúdo predefinidos. Sua duração conta no ciclo como qualquer outro estudo.
- Revisão por dia fixo da semana não faz parte do MVP.

### Backend e API

- O backend será um monólito modular em Spring Boot, organizado primeiro por feature e depois por camada: controller, service, repository, model e dto.
- Regras de negócio ficam nos services. Entidades JPA não orquestram casos de uso e não são expostas pela API.
- Operações simples permanecem no service principal da feature. Casos de uso transacionais complexos recebem services específicos, como `FinishStudySessionService` e `CycleProgressRecalculationService`.
- O service orquestrador acessa diretamente repositories da própria feature e chama services públicos das demais features. Isso mantém as regras na feature responsável e evita services acessando repositories alheios.
- Casos de uso públicos usam propagação transacional `REQUIRED`. Operações internas que só podem executar dentro de uma transação existente podem usar `MANDATORY`. `REQUIRES_NEW` não será usado nos fluxos de negócio do MVP.
- Controllers recebem e retornam DTOs. Inicialmente os mesmos DTOs podem atravessar controller e service dentro da feature; Commands e Results separados só serão introduzidos quando houver divergência real.
- Erros seguem `ProblemDetail` e RFC 9457, produzidos por tratamento global. Validações retornam 400, ausência retorna 404 e conflitos de regra retornam 409. Recursos de outro usuário também retornam 404.
- A API usa autenticação do Spring Security por sessão, armazenada no PostgreSQL com Spring Session JDBC. Cookies são `HttpOnly`, `SameSite=Lax` e `Secure` sob HTTPS; operações mutáveis têm proteção CSRF.
- Se ainda não houver usuários, o cadastro inicial é permitido. Depois disso, novos cadastros dependem de `APP_REGISTRATION_ENABLED`.
- A recuperação de senha sem SMTP usa um comando do container que gera um link temporário de uso único. Apenas o hash do token é persistido.
- Horários são persistidos como instantes UTC. Cada usuário possui um fuso IANA; revisões usam datas locais e não mudam retroativamente quando o fuso é alterado.

### Frontend e implantação

- O frontend será React, TypeScript e Vite, com React Router para navegação, TanStack Query para estado remoto e React Hook Form com validação de formulários.
- A direção visual parte do protótipo existente: interface limpa, responsiva, com próxima ação dominante, progresso do ciclo, fila de revisões e modal de finalização.
- O build do frontend será incorporado ao artefato Spring Boot. Em produção, a mesma aplicação serve SPA e API sob `/api`; em desenvolvimento, Vite encaminha `/api` ao backend.
- O Compose mínimo terá apenas `app` e `postgres`. Não haverá Redis, broker, armazenamento de objetos ou CDN obrigatórios.
- Flyway gerencia o schema. A aplicação expõe health checks e a documentação inclui backup e restauração do PostgreSQL antes de upgrades.
- A instalação padrão não depende de analytics, fontes, scripts ou APIs externas.

## Testing Decisions

- Bons testes verificam comportamento observável, persistência e contratos, não chamadas internas, quantidade de métodos ou detalhes de implementação.
- Todo endpoint e caso de uso relevante terá teste de integração atravessando HTTP, controller, service, repository, Flyway e PostgreSQL real.
- Testes de integração usarão Spring Boot, MockMvc e PostgreSQL via Testcontainers. H2 não será usado.
- Uma anotação global `IntegrationTest` comporá o bootstrap Spring, MockMvc, profile de teste e a configuração compartilhada. A configuração global fornecerá o container PostgreSQL por `ServiceConnection`, sem herança por classe-base abstrata.
- Testes de integração cobrirão autenticação e isolamento entre usuários, CRUD de matérias e conteúdos, criação/edição/ativação de ciclos, lifecycle do cronômetro, finalização idempotente, lançamento manual, revisão e recálculo após edições.
- Concorrência será testada no banco real: duas finalizações da mesma sessão, duas tentativas de iniciar cronômetros e edições com versão obsoleta.
- Testes unitários serão reservados a regras puras e determinísticas: taxa de acerto, duração total sugerida, prioridade, arredondamento, divisão equilibrada, ordenação ponderada, distribuição de excedentes e recálculo de datas de revisão.
- O planejador receberá testes de invariantes: toda matéria aparece, nenhuma recebe menos que o mínimo, o total é preservado, durações são múltiplos de cinco minutos e matérias consecutivas são evitadas quando possível.
- A distribuição de créditos receberá testes para sessões parciais, excedente entre etapas da mesma matéria, limite da volta, preenchimento antecipado, revisão contando no ciclo e recálculo após edição/exclusão.
- Revisões receberão testes para os seis intervalos, mudança de fuso, conclusão atrasada sem deslocamento, consolidação de atrasos e independência em relação à sessão original.
- O frontend terá testes de componente para estados críticos e poucos testes ponta a ponta das jornadas: criar matéria e conteúdo; gerar/editar ciclo; executar/finalizar sessão; concluir revisão; corrigir sessão histórica.
- Não serão criados testes unitários para controllers, repositories ou services que apenas delegam. Esses comportamentos serão cobertos na seam de integração mais alta.

## Out of Scope

- Objetivos de concurso, edital verticalizado, importação de edital e hierarquia profunda de tópicos.
- Revisão por dia da semana, intervalos adaptativos e reagendamento automático baseado em desempenho.
- Mudança automática do ciclo com base na taxa de acerto.
- Banco de questões, alternativas, gabaritos, simulados completos ou integração com plataformas de questões.
- PWA offline-first, sincronização de mutações offline e resolução de conflitos entre dispositivos.
- Aplicativos nativos, notificações push e publicação em lojas.
- Painel administrativo, papéis complexos, organizações, convites por e-mail e SMTP obrigatório.
- Metas semanais, gamificação, ranking, competição e recursos sociais.
- IA, tutor, geração de conteúdo ou recomendação por modelo probabilístico.
- Hospedagem de PDFs, videoaulas, anexos ou materiais de curso.
- API pública, webhooks, OIDC e integrações externas.
- Alta disponibilidade, múltiplas instâncias da aplicação, Redis, filas ou microsserviços.
- Exportação/importação completa de dados pela interface; o MVP terá backup e restauração operacional do PostgreSQL.

## Further Notes

- “Ciclo atual” significa o ciclo `ACTIVE`. A troca para outro ciclo o torna inativo, não concluído; reativá-lo retoma seu progresso.
- “Volta” é uma execução completa e contínua das etapas de um ciclo. Não corresponde a uma semana do calendário.
- “Etapa” sempre referencia uma matéria. O conteúdo é escolhido no início da sessão e nunca participa diretamente da ordenação do ciclo.
- A edição livre dos dados é intencional. O produto não é uma competição nem um sistema de certificação; integridade significa manter os cálculos coerentes com o que o usuário decidiu registrar.
- O algoritmo sugerido deve ser explicável e determinístico: as mesmas entradas geram o mesmo planejamento.
- O protótipo HTML existente é uma referência de direção e fluxo, não um contrato visual definitivo.
- Antes da implementação, este PRD deve ser decomposto em issues verticais pequenas, começando por autenticação, matéria/conteúdo e uma sessão mínima persistida, sem tentar construir todas as camadas isoladamente.
