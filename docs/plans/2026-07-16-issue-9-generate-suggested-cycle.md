# Issue 9 — Gerar ciclo sugerido

## Objetivo

Gerar um ciclo determinístico e explicável a partir da incidência, do peso e da dificuldade das matérias, preservando um piso de estudo por matéria e produzindo etapas prontas para ativação ou personalização.

## Contrato público

- `POST /api/study-cycles/suggestions` cria um ciclo em modo `SUGGESTED` para o usuário autenticado.
- A entrada aceita de 1 a 30 matérias ativas, cada uma com quantidade de questões, peso e dificuldade (`EASY`, `MEDIUM` ou `HARD`).
- Matérias repetidas, arquivadas, inexistentes ou pertencentes a outro usuário são rejeitadas.
- A resposta expõe o ciclo persistido, suas etapas e uma explicação por matéria com os dados de entrada, a prioridade calculada, a carga e o número de aparições.
- Consultas e listagens preservam a explicação; ao editar uma sugestão, o ciclo passa a ser `CUSTOM` e deixa de carregar os metadados do cálculo original.

## Algoritmo e invariantes

1. A duração total é de duas horas por matéria, limitada entre 10 e 30 horas.
2. Cada matéria recebe inicialmente 60 minutos.
3. A prioridade é `questões × peso × fator de dificuldade`, com fatores 1,00, 1,25 e 1,50.
4. O tempo restante é distribuído proporcionalmente, usando o método das maiores sobras para arredondamento exato em blocos de cinco minutos.
5. Cargas acima de três horas são divididas em aparições equilibradas, também em múltiplos de cinco minutos.
6. As aparições são ordenadas pela maior carga restante, evitando repetir a mesma matéria quando há outra disponível.
7. Para a mesma entrada e ordem de matérias, o resultado é sempre idêntico.

## Interface

### Direção

- **Pessoa:** concurseiro traduzindo a importância do edital em uma rotina executável.
- **Tarefa:** informar poucos parâmetros, entender a sugestão e decidir se quer ativá-la ou personalizá-la.
- **Sensação:** cálculo confiável apresentado como planejamento de caderno, não como uma caixa-preta estatística.

### Sistema aplicado

- **Domínio:** edital, incidência, peso, dificuldade, prioridade, carga e aparições.
- **Mundo de cor:** papel, tinta, fichário verde e marca-texto amarelo já definidos no produto.
- **Assinatura:** a duração estimada aparece como uma anotação destacada e a explicação abre dentro do próprio card do ciclo.
- **Composição:** o compositor é a superfície focal; cada matéria forma uma linha numerada em desktop e um bloco vertical em mobile.
- **Tipografia e profundidade:** serif editorial para títulos e horas; sans-serif para parâmetros; sombra sutil apenas na superfície focal.
- **Espaçamento:** escala de 4 px e alvos interativos de pelo menos 44 px.

## TDD em fatias verticais

1. **RED → GREEN:** o planejador distribui o exemplo representativo com piso de uma hora, prioridades e total exato.
2. **RED → GREEN:** cargas acima de três horas são divididas em aparições equilibradas.
3. **RED → GREEN:** a ordenação intercala matérias e permanece determinística.
4. **RED → GREEN:** entradas fora dos limites ou com valores não positivos são rejeitadas.
5. **RED → GREEN:** testes de invariantes cobrem de 1 a 30 matérias, múltiplos de cinco, soma exata e máximo de três horas por etapa.
6. **RED → GREEN:** a API cria e relê uma sugestão explicável atravessando HTTP, segurança, service, repository, Flyway e PostgreSQL.
7. **RED → GREEN:** editar o ciclo sugerido converte-o em personalizado e remove a explicação original.
8. **RED → GREEN:** a interface envia os parâmetros e renderiza a explicação retornada.
9. **Refactor:** remover duplicação somente depois de todas as fatias verdes.

## Validação

- Testes unitários puros do planejador cobrem exemplos e propriedades do algoritmo.
- Testes de integração cobrem criação, consulta e conversão para ciclo personalizado.
- Teste de componente cobre o fluxo de geração e a explicação exibida.
- `mvn -pl backend test`, `npm test`, `npm run build`, `git diff --check` e inspeção visual em desktop e mobile.
