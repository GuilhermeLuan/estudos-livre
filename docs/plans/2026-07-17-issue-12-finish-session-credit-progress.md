# Issue 12 — Finalizar sessão e creditar progresso parcial

## Objetivo

Permitir que o estudante encerre um cronômetro confirmando o tempo efetivamente estudado. A operação registra a sessão como `FINISHED`, conserva a medição original e credita a duração efetiva uma única vez à primeira etapa incompleta da mesma matéria na volta atual.

## Interface pública

- `POST /api/study-sessions/{id}/finish` recebe `effectiveSeconds` e `expectedVersion`.
- A resposta da sessão passa a expor `version`, `effectiveSeconds`, `finishedAt` e os créditos aplicados.
- Repetir a mesma finalização devolve a sessão finalizada e os créditos existentes.
- Tentar substituir uma duração já finalizada ou enviar uma versão obsoleta retorna `409` em `ProblemDetail`.

## Modelo e invariantes

- Criar um snapshot mínimo das etapas de cada volta com matéria, posição, meta e progresso creditado em segundos. Voltas já existentes são inicializadas pela migração e novas voltas recebem o snapshot ao serem criadas.
- A duração medida permanece derivada dos segmentos do cronômetro; a duração efetiva é persistida separadamente na sessão.
- Uma sessão `ACTIVE` fecha seu segmento aberto ao finalizar; uma sessão `PAUSED` pode ser finalizada sem criar novo segmento.
- O crédito é aplicado somente a uma etapa da mesma matéria e nunca excede o restante dessa etapa neste incremento.
- Quando não existe etapa elegível, a duração efetiva continua registrada na sessão e a lista de créditos fica vazia.
- A linha da sessão é bloqueada durante a finalização. A versão otimista detecta formulários obsoletos; o estado finalizado torna a repetição idempotente.

## Ciclos TDD

1. **Tracer bullet — crédito parcial**
   - Escrever um teste de integração HTTP que inicia uma sessão do ciclo, fixa uma medição conhecida, finaliza com duração efetiva menor que a meta e observa sessão `FINISHED`, cronômetro fechado e crédito parcial.
   - Implementar a migração, request/response, repository, service e endpoint mínimos para passar.
2. **Matéria sem etapa elegível**
   - Escrever teste para uma sessão livre cuja matéria não está na volta.
   - Persistir a duração efetiva sem criar crédito.
3. **Idempotência e edição obsoleta**
   - Escrever teste que repete exatamente a mesma finalização e recebe o resultado existente.
   - Escrever teste que tenta alterar a duração finalizada e recebe conflito.
4. **Concorrência**
   - Disparar duas finalizações simultâneas e provar, pela API, que há no máximo um efeito de crédito.
   - Manter a serialização na transação e traduzir divergências em conflito de domínio.
5. **Confirmação no frontend**
   - Escrever teste de componente que abre o modal pela mesa de estudo, mostra a duração medida, permite corrigir a duração efetiva e envia versão + segundos.
   - Implementar a mutação, fechar o cronômetro após sucesso e invalidar sessão/ciclos para exibir o progresso persistido.

## Direção de interface

- **Intenção:** concurseiro encerrando um bloco de estudo e corrigindo distrações sem perder confiança no registro.
- **Hierarquia:** duração medida como âncora; duração efetiva como decisão; impacto no ciclo como confirmação; ação `Finalizar sessão` como único comando primário.
- **Paleta:** papel para a superfície, fichário verde para confirmação e marcador amarelo para o resumo do crédito.
- **Profundidade:** modal nativo elevado com sombra sutil, campos no papel inset e divisões tonais suaves.
- **Tipografia:** relógio com numerais tabulares; título editorial; metadados e explicações em quatro níveis de tinta.
- **Espaçamento:** grade de 4 px, controles com ao menos 44 px e painel responsivo sem ações comprimidas.
- **Assinatura:** o resumo do crédito usa a silhueta de marcador da etapa, conectando o encerramento ao mapa da volta.

## Verificação

- Executar cada teste novo em RED antes da implementação mínima correspondente.
- Rodar a classe de integração da finalização após cada ciclo e a suíte de sessões ao concluir o backend.
- Rodar os testes de componente do modal, a suíte Vitest e o build TypeScript/Vite.
- Verificar visualmente o modal em desktop e mobile, incluindo sucesso, erro, estado pendente e foco de teclado.
