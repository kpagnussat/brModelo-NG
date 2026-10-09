<p align="center">
  <img src="packaging/brModelo.svg" alt="Ícone do brModelo NG" width="96" height="96">
</p>

# brModelo NG

**Modelagem de bancos de dados para aprender e ensinar.**

> O README do projeto original termina com um convite: *"Copie, altere, publique."*
> O brModelo NG nasceu desse convite.

[![Versão](https://img.shields.io/github/v/release/kpagnussat/brModelo-NG?label=vers%C3%A3o)](https://github.com/kpagnussat/brModelo-NG/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/kpagnussat/brModelo-NG/total?label=downloads)](https://github.com/kpagnussat/brModelo-NG/releases)
[![Build e testes](https://github.com/kpagnussat/brModelo-NG/actions/workflows/ci.yml/badge.svg)](https://github.com/kpagnussat/brModelo-NG/actions/workflows/ci.yml)
[![Estrelas](https://img.shields.io/github/stars/kpagnussat/brModelo-NG?label=estrelas&style=flat)](https://github.com/kpagnussat/brModelo-NG/stargazers)
[![Licença GPL-3.0](https://img.shields.io/badge/licen%C3%A7a-GPL--3.0-blue.svg)](LICENSE)

[![Linux](https://img.shields.io/badge/Linux-.deb%20%C2%B7%20.rpm%20%C2%B7%20Flatpak%20%C2%B7%20port%C3%A1til-FCC624?logo=linux&logoColor=black)](https://github.com/kpagnussat/brModelo-NG/releases/latest)
[![Windows](https://img.shields.io/badge/Windows-.msi%20%C2%B7%20port%C3%A1til-0078D6?logo=data:image/svg%2bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZmlsbD0id2hpdGUiIGQ9Ik0wIDBoMTEuNHYxMS40SDB6TTEyLjYgMEgyNHYxMS40SDEyLjZ6TTAgMTIuNmgxMS40VjI0SDB6TTEyLjYgMTIuNkgyNFYyNEgxMi42eiIvPjwvc3ZnPg==)](https://github.com/kpagnussat/brModelo-NG/releases/latest)
[![macOS](https://img.shields.io/badge/macOS-.dmg-000000?logo=apple&logoColor=white)](https://github.com/kpagnussat/brModelo-NG/releases/latest)
[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](BUILDING.md)

O **brModelo NG** é um fork **não oficial** do brModelo,
mantido por [Kristofer Pagnussat](https://github.com/kpagnussat). Os arquivos
**`.brM3` continuam compatíveis com o brModelo oficial**, com testes para
proteger essa compatibilidade.

O brModelo original é uma ferramenta livre e gratuita criada por **Carlos Henrique
Cândido**, voltada ao ensino de modelagem de bancos de dados relacionais. O projeto
oficial está em [chcandido/brModelo](https://github.com/chcandido/brModelo) e
[sis4.com](https://www.sis4.com/). O NG preserva esse propósito e atualiza a
interface e a integração com o desktop. Este fork foi desenvolvido com a
assistência de ferramentas de inteligência artificial.

Você pode criar modelos conceituais e lógicos, converter do conceitual para o
lógico, gerar SQL, exportar imagens e imprimir diagramas. Também estão disponíveis
fluxogramas, diagramas de atividades, estruturas analíticas de projetos (EAP)
e diagramas livres.

## Interface

As dicas ao passar o mouse descrevem propriedades e ações; valores cortados e
caminhos das abas aparecem por inteiro. O menu **Ajuda → Releases do brModelo NG**
abre os lançamentos no GitHub, sem consultas automáticas de atualização.

Modelo conceitual no tema claro:

![Janela principal clara com o modelo conceitual da Escola Aurora](docs/img/principal-claro.png)

O mesmo modelo no tema escuro; a folha do diagrama permanece branca:

![Janela principal escura com o mesmo modelo conceitual](docs/img/principal-escuro.png)

## O que muda em relação ao brModelo

O **NG** parte do **brModelo oficial 3.3.2 publicado no [sis4.com](https://www.sis4.com/)**:

- **18 temas FlatLaf, claros e escuros.** Por padrão, o NG segue o modo do
  sistema e alterna sozinho quando o desktop muda. Em **Editar → Aparência…**,
  com prévia de cada tema, você escolhe um preferido para cada modo ou fixa um
  tema. A troca é imediata, sem reiniciar.
- **Ícones SVG na interface**, adaptados ao tema e à escala da tela, além de um
  ícone próprio para o aplicativo.
- **Layouts para HiDPI e fontes maiores.** Barras, campos e diálogos se ajustam
  ao conteúdo; a coluna do Inspector acomoda os títulos das abas, que ficam em
  uma única linha rolável. As barras de ferramentas permanecem fixas, sem se
  desprenderem em janelas flutuantes.
- **Edição mais precisa.** As formas encaixam na grade ao mover, criar e
  redimensionar (ligado por padrão; segure Alt para soltar). **Organizar
  ligações** escolhe o lado de cada linha, ordena os pontos para as linhas não
  se cruzarem e os distribui por igual. Os ícones de ação ao lado da forma
  selecionada e as chaves das tabelas são vetoriais e ficam nítidos em qualquer
  zoom e na exportação. O Inspector virou uma grade de propriedades com aparência
  própria para cada tipo de valor.
- **Barra de status com indicador de mensagens não lidas.** Mensagens temporárias
  desaparecem após cinco segundos; o contador permanece até você abrir o log.
  Erros têm um símbolo próprio, além da cor. Clique no indicador para ler o log.
- **Seletor de arquivos integrado ao desktop.** No Linux, o XDG Desktop Portal
  permite usar os favoritos e locais do ambiente gráfico, quando disponível.
  Sem portal, há alternativas nativa e Swing; no Windows e macOS, a primeira
  opção é o seletor nativo. O aplicativo lembra a última pasta e confirma a
  substituição do arquivo considerando a extensão final.
- **JSON opcional (`.brMj`)**, legível e adequado à revisão de diferenças no Git.
  Escolha JSON em **Salvar como**; para compartilhar com quem usa o oficial,
  salve como `.brM3`. O oficial não abre `.brMj`.
- **Pastas próprias para configurações, recuperação e modelos reutilizáveis.**
  No Linux, seguem o padrão XDG, em vez de criar arquivos soltos na pasta pessoal
  ou de execução. Windows e macOS também usam pastas específicas do NG.
  Arquivos antigos encontrados na pasta de execução são copiados na primeira
  utilização, sem apagar os originais.
- **Ajuda incluída no jar e em todos os pacotes.** Funciona mesmo ao iniciar de
  uma pasta vazia. Os tópicos em Markdown de `docs/ajuda/` geram um site HTML
  local, aberto no navegador pelo menu **Ajuda** (F1). No Flatpak, a ajuda abre
  como uma página única, sem precisar de permissão de rede.
- **Abrir um arquivo não executa código.** Diagramas recebidos de outras
  pessoas, modelos reutilizáveis, recuperação e área de transferência passam por
  uma lista de classes permitidas antes de qualquer objeto ser criado. Veja
  [Por dentro do código](#por-dentro-do-código).
- **Correções de uso diário.** A prévia de impressão e a exportação de imagens
  preservam a geometria e o texto do diagrama; salvar texto sobre um arquivo
  existente substitui o conteúdo. A rolagem pela roda do mouse é vertical por
  padrão, e horizontal com Shift. Cores, separadores e textos da interface têm
  correções para temas escuros e fontes maiores.
- **Desempenho e diagramas grandes.** A tela desenha só a parte visível da
  grade e, no Linux, usa padrões mais rápidos do Java2D, então rolar e
  redimensionar continua fluido com milhares de itens. Abrir, salvar e desfazer
  funcionam em diagramas grandes e muito interligados, que no oficial estouram
  a pilha. Detalhes em [Por dentro do código](#por-dentro-do-código).
- **Distribuição e execução.** O projeto inclui empacotamento para Linux,
  Windows e macOS, versões portáteis para Linux e Windows (sem instalação),
  bundle Flatpak e jar portátil; arquivos passados ao iniciar
  abrem no editor, inclusive vários arquivos e caminhos com espaços. O jar usa
  Java 21; os pacotes incluem o ambiente Java necessário.

**O que não muda:** o formato padrão continua sendo **`.brM3`**, e esses arquivos
abrem no brModelo oficial. O
[teste de compatibilidade](test/brmodelo/OfficialCompatibilityTest.java) carrega
o jar oficial em um ambiente de classes isolado e verifica leitura e gravação
nos dois sentidos para os seis tipos de diagrama, inclusive após conversão por
JSON. O [teste da forma serializada](test/brmodelo/SerializedFormTest.java)
detecta alterações em classes, identificadores e campos persistidos. O teste com
o oficial exige fornecer seu jar; sem ele, os seis casos são explicitamente
ignorados. Veja como executá-lo em [BUILDING.md](BUILDING.md#automated-tests).

Editor de campos de uma tabela no tema escuro:

![Editor de campos mostrando chave primária e campo único](docs/img/editor-campos.png)

Exemplo da barra de status com um erro ainda não lido:

![Barra de status com uma mensagem de erro e contador de uma mensagem não lida](docs/img/status-erro.png)

As capturas vêm dos [exemplos de teste versionados](test-resources/fixtures/),
com dados inventados da Escola/Biblioteca Aurora, renderizados pela tarefa
`snapDialogs`. A mensagem de erro é um exemplo gerado pelo renderizador.

## Por dentro do código

Além da interface, o NG reorganiza o código herdado, sempre preservando o
formato `.brM3`:

- **Segurança ao abrir arquivos.** O `.brM3` é serialização Java, e o oficial
  o lê com um `ObjectInputStream` comum. Assim, um arquivo malicioso recebido
  de outra pessoa pode executar código no computador só por ser aberto, um
  ataque conhecido como *desserialização insegura*. O NG lê por uma lista de
  classes permitidas: só os pacotes do brModelo e os tipos de valor do JDK que
  o modelo usa. O resto é recusado antes de a classe ser carregada ou de
  qualquer objeto ser construído, e proxies dinâmicos são sempre rejeitados. A
  regra vale para diagramas, modelos reutilizáveis, recuperação e área de
  transferência; o JSON segue a mesma lista e ainda valida campos, tipos e
  referências. Os testes usam os pontos de entrada clássicos desse ataque e
  verificam que são recusados sem que nenhum código deles rode. O formato não
  mudou, e os arquivos continuam abrindo no oficial.
- **Sem acesso à rede.** O oficial verifica atualizações no site por HTTP, sem
  criptografia. O NG não faz nenhuma chamada de rede: o menu de releases só abre
  a página no navegador, a ajuda é local e o Flatpak não tem permissão de rede.
- **Build moderno.** O build Ant/NetBeans deu lugar ao Gradle com Java 21. As
  dependências vêm do Maven Central com verificação de checksum, e a integração
  contínua compila e testa no Linux, no Windows e no macOS.
- **Classes grandes divididas.** `Editor` (2.093 → 1.659 linhas), `Diagrama`
  (2.541 → 1.996) e `Forma` (1.187 → 933) tiveram persistência, pintura,
  seleção, área de transferência, salvamento automático, configurações e
  ligações extraídas para classes auxiliares. Os métodos originais continuam
  existindo e delegam a elas, então subclasses e o formato salvo não mudam.
- **Código morto removido.** Cerca de 1.830 linhas de código comentado, imports
  e variáveis sem uso saíram, sem tocar em campos persistidos nem em membros
  usados por reflexão.
- **Threads corrigidas.** O salvamento automático serializava o diagrama numa
  thread de timer enquanto você editava; agora captura um snapshot na thread da
  interface e grava por uma fila única. Cada mensagem de status criava uma thread
  que nunca terminava (mais de 200 após 100 mensagens); hoje são temporizadores
  do Swing. A barra de status deixou de ser atualizada fora da thread da
  interface.
- **Inicialização e pintura mais rápidas.** A descoberta de impressoras ficou
  para o primeiro uso do diálogo de impressão: a abertura caiu de 3,3 s para
  1,4 s. A tela redesenha só as linhas visíveis da grade (4 vezes mais rápido),
  e os ícones SVG são renderizados uma vez por escala e tema, em vez de a cada
  pintura.
- **Diagramas grandes.** A serialização Java percorre o diagrama de forma
  recursiva, e com cerca de 2.000 itens interligados estourava a pilha da thread
  da interface. Abrir, salvar e cada passo de desfazer agora rodam numa thread
  com pilha maior, sem mudar o formato do arquivo.
- **Testes.** 349 testes automatizados cobrem persistência, segurança, interface
  e utilitários. Um teste compara a forma serializada de mais de 200 classes com
  uma referência, para que nenhuma refatoração quebre a compatibilidade em
  silêncio.

## Instalação e execução

Baixe os pacotes na
[página de Releases](https://github.com/kpagnussat/brModelo-NG/releases). Também é
possível [compilar a partir do código-fonte](BUILDING.md).

Na página de Releases, escolha o arquivo correspondente ao seu sistema e à sua
arquitetura:

| Sistema | Arquivo | Como executar |
| --- | --- | --- |
| Linux | `.deb` ou `.rpm` | Instale pelo gerenciador de pacotes e abra **brModelo NG** no menu de aplicativos. |
| Linux portátil | `.tar.gz` com o aplicativo e Java | Extraia e execute `brmodelo-ng/bin/brmodelo-ng`. |
| Linux com Flatpak | `brmodelo-ng.flatpak` | Instale o bundle conforme os comandos abaixo. |
| Windows | `.msi` | Execute o instalador e abra **brModelo NG** pelo menu Iniciar. |
| Windows portátil | `.zip` com o aplicativo e Java | Extraia e execute `brModelo NG\brModelo NG.exe`. Não precisa instalar nem ter Java. |
| macOS | `.dmg` | Abra a imagem, copie **brModelo NG** para Aplicativos e inicie por essa pasta. |
| Qualquer um desses sistemas com Java 21 | `brModelo.jar` | Execute `java -jar brModelo.jar`. |

O aplicativo **não está no Flathub**. Com Flatpak e os runtimes necessários
disponíveis, instale o bundle baixado da página de Releases:

```sh
flatpak install --user ./brmodelo-ng.flatpak
flatpak run io.github.kpagnussat.brModeloNG
```

Para o jar portátil, instale Java 21 e execute na pasta do download. Você também
pode passar um ou mais diagramas como argumentos:

```sh
java -jar brModelo.jar
java -jar brModelo.jar "modelo.brM3" "outro modelo.brMj"
```

Use **Arquivo → Abrir** para abrir seus diagramas e **Salvar como** para escolher
o formato. Mantenha `.brM3` ao trocar arquivos com colegas que usam o oficial.
Os detalhes do formato JSON estão em [FORMATO-BRMJ.md](docs/FORMATO-BRMJ.md).

### Opções de tema

O botão **Aparência** (sol e lua), no fim da barra principal, abre um menu com
**Seguir o sistema**, **Claro**, **Escuro** e **Mais temas…**. Em
**Editar → Aparência…** estão os 18 temas (7 claros e 11 escuros), com prévia.
Ao seguir o sistema, você escolhe um tema preferido para o modo claro e outro
para o escuro, e o NG troca sozinho quando o desktop muda de modo. A escolha
fica em `config.chc`.

Na inicialização, a opção da linha de comando tem prioridade sobre a escolha
salva. As opções vão antes de `-jar`:

```sh
java -Dbrmodelo.tema=claro -jar brModelo.jar
java -Dbrmodelo.tema=escuro -jar brModelo.jar
java -Dbrmodelo.tema=sistema -jar brModelo.jar
java -Dbrmodelo.tema=nord -jar brModelo.jar
```

`claro` e `escuro` fixam o FlatLaf Light e o FlatLaf Dark; `sistema` (ou
`auto`) segue o desktop; o identificador de outro tema, como `nord` ou
`dracula`, fixa esse tema.
Uma seleção feita dentro do aplicativo vale imediatamente; a opção de linha de
comando volta a ter prioridade na próxima inicialização.

Para usar o visual nativo da plataforma em vez do FlatLaf, informe a classe com
`-Dswing.systemlaf`, por exemplo
`-Dswing.systemlaf=com.sun.java.swing.plaf.gtk.GTKLookAndFeel` no Linux. Nesse
modo, os seletores de tema ficam ocultos.

## Código-fonte e contribuições

[BUILDING.md](BUILDING.md) explica a compilação com JDK 21 e Gradle, os testes,
as capturas de tela e a geração dos pacotes. Para começar:

```sh
./gradlew run
./gradlew build
```

Relate problemas e envie contribuições pelo
[repositório do brModelo NG](https://github.com/kpagnussat/brModelo-NG).
Inclua passos para reproduzir o problema, sistema operacional e, quando
necessário, um diagrama pequeno com dados inventados.

Ao alterar o código, execute `./gradlew test`; para mudanças de interface, confira
as capturas de `snapDialogs` nos temas claro e escuro e com fontes maiores.
Mudanças de persistência também exigem testar com o jar oficial usando
`-PoficialJar="/caminho/para/brModelo.jar"`. As tarefas `snapshotFixtures` e
`snapshotSerializedForm` atualizam as referências dos testes somente quando isso
é intencional: revise as diferenças e a compatibilidade antes de aceitá-las.

## Outras versões do brModelo

O brModelo tem outros ramos, mantidos por outras equipes. O NG não tem relação
com eles:

| Versão | O que é |
| --- | --- |
| [brModelo 3](https://www.sis4.com/) | O original de Carlos Henrique Cândido, em Java, base do NG. |
| [brModelo Next](https://github.com/gbd-ufsc/brModelo-Official-Versions) | Versão desktop do Grupo de Banco de Dados da UFSC, com esquema agregado para bancos NoSQL. |
| [brModelo Web](https://www.brmodeloweb.com) | Aplicação no navegador, de código aberto ([brmodelo-app](https://github.com/brmodeloweb/brmodelo-app)), com formato próprio. |

O NG é para quem quer o programa no computador, sem internet, trabalhando com os
mesmos arquivos `.brM3` do brModelo 3.

## Origem e licença

O brModelo nasceu em 2005 como trabalho de especialização em banco de dados na
UFSC e na UNIVAG, sob orientação do Prof. Dr. Ronaldo dos Santos Mello. Seu foco
é o ensino técnico e acadêmico da modelagem relacional, com base na metodologia
apresentada por Carlos A. Heuser em *Projeto de Banco de Dados*.

O brModelo NG preserva os créditos de Carlos Henrique Cândido e dos colaboradores
do projeto original. É software livre sob a **GNU GPL versão 3 ou posterior**:
você pode usar, estudar, modificar e redistribuir conforme a [licença](LICENSE).
As licenças e informações das dependências e dos ícones estão em
[THIRD-PARTY.md](THIRD-PARTY.md). Consulte também o [CHANGELOG](CHANGELOG.md).

## English

**brModelo NG** is an unofficial fork of
[brModelo](https://github.com/chcandido/brModelo), maintained by Kristofer
Pagnussat. Carlos Henrique Cândido created the original tool for teaching
relational database modeling; its README closes with an invitation, "Copy,
change, publish", and this fork answers it. It was developed with assistance
from AI tools. It is not related to brModelo Next (UFSC) or brModelo Web; it is
for people who want a desktop app that works offline on the same `.brM3` files
as brModelo 3.

It provides 18 light and dark FlatLaf themes that can follow the desktop, snap
to grid, line organizing, SVG interface icons, layouts for HiDPI and
larger fonts, desktop file selection and an optional JSON format. Opening a file
cannot run code: Java deserialization goes through a class allowlist, closing the
insecure-deserialization hole of the official reader, and the app makes no
network calls. Under the hood, the build moved to Gradle and Java 21, the largest
classes were split into helpers, dead code and thread leaks were removed, startup
and painting got faster, and large interlinked diagrams no longer overflow the
stack. **`.brM3` remains the default and compatible with official brModelo**, with
serialized-form and isolated official-jar compatibility tests; the latter require
the official jar. Official brModelo does not read `.brMj` JSON files.

Download packages from the
[Releases page](https://github.com/kpagnussat/brModelo-NG/releases), or see
[BUILDING.md](BUILDING.md) to build, test and run from source.
Portable Linux (`.tar.gz`) and Windows (`.zip`) builds include Java and need no
installation. The app is not on Flathub. The portable jar requires Java 21.
Licensed under GPL-3.0-or-later; see [LICENSE](LICENSE) and
[THIRD-PARTY.md](THIRD-PARTY.md).

---

<p align="center"><strong>Copie, altere, publique.</strong></p>
