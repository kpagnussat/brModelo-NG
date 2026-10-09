# Changelog

As alterações do [brModelo NG](https://github.com/kpagnussat/brModelo-NG) são
registradas neste arquivo, no formato [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).
Esta seção reúne as mudanças do fork em relação ao brModelo oficial 3.3.2.
O histórico anterior pertence ao
[projeto original](https://github.com/chcandido/brModelo/commits/master/).

## [1.0.0] - 2026-10-09

Primeira versão do **brModelo NG**.

### Added

- **Interface:** 18 temas FlatLaf (7 claros e 11 escuros) no diálogo
  **Editar → Aparência…**, com prévias; ao seguir o sistema, um tema preferido
  para cada modo e troca automática quando o desktop muda; opção
  `-Dbrmodelo.tema` com `claro`, `escuro`, `sistema` ou o identificador de um
  tema; ícones SVG para ações, paleta, abas e árvores, com cores adaptadas ao tema.
- **Edição:** encaixe na grade ao mover, criar e redimensionar (ligado por
  padrão, Alt solta; setas movem um passo da grade); **Organizar ligações**
  escolhe o lado de cada linha, ordena os pontos sem cruzamentos e os distribui
  por igual, em um único passo de desfazer.
- **Ajuda:** tópicos em Markdown (`docs/ajuda/`) gerados como site offline,
  incluído no jar e nos pacotes e aberto no navegador por **F1**; no Flatpak,
  uma página HTML única aberta pelo portal do sistema, sem permissão de rede.
- **Status:** indicador de mensagens e erros não lidos com contador, símbolo e
  acesso ao log; mensagens temporárias com expiração em cinco segundos.
- **Arquivos:** formato JSON opcional `.brMj`, determinístico e versionado,
  com abertura, salvamento e conversão de volta para `.brM3`.
- **Integração:** seletor de arquivos do sistema em cada plataforma (XDG Desktop
  Portal no Linux/BSD, diálogo do Windows e painel do macOS), com a lista de tipos
  de arquivo, e alternativas AWT e Swing; memória da última pasta e confirmação
  de substituição para o caminho final.
- **Distribuição:** identidade brModelo NG, ícone próprio, abertura de arquivos
  por argumentos e associações `.brM3`/`.brMj` (*Diagrama brModelo* e *Diagrama
  JSON brModelo*); tarefas para app-image, DEB, RPM,
  MSI, DMG e bundle Flatpak com ambiente Java incluído; versões portáteis
  para Linux (`.tar.gz`) e Windows (`.zip`), que dispensam instalação.
- **Desenvolvimento:** build Gradle com Java 21, jar portátil com dependências,
  catálogo de versões e verificação de dependências por checksum; CI para Linux,
  Windows e macOS e geração de releases em rascunho a partir de tags.
- **Verificação:** testes de persistência, segurança, interface e utilitários;
  seis diagramas de exemplo inventados com dumps canônicos; teste da forma
  serializada e teste de compatibilidade bidirecional com o jar oficial isolado,
  inclusive após conversão por JSON; tarefas de capturas e auditoria de arquivos.

### Changed

- **Versão:** identidade NG 1.0.0 derivada do Gradle no Sobre e nos pacotes;
  formato de diagramas preservado em 3.2.0. Releases acessíveis pelo menu Ajuda,
  sem consultas automáticas ao site do brModelo oficial.
- **Dicas:** tooltips descritivos nas propriedades, controles, editores e impressão;
  valores cortados aparecem por inteiro e as dicas informam a ação disponível.

- **Layout:** barras, Inspector, editores, conversor, ajuda e diálogos de impressão
  dimensionados pelo conteúdo e pela fonte; coluna esquerda ajustada às abas,
  abas do Inspector em uma linha rolável e cabeçalho vazio da paleta oculto.
- **Aparência:** Inspector como grade de propriedades, cabeçalhos com chevrons,
  ações compactas, dicas integradas com placeholder, paleta de ferramentas
  que se ajustam à altura disponível em uma coluna de largura fixa e mesa temática
  com a folha delimitada por uma borda fina. Barras alinhadas, grupos com
  espaçamento, abas com destaque e fechar ao passar o mouse, rolagem fina,
  cantos e padding consistentes nos diálogos. Cores e superfícies acompanham a
  troca ao vivo; a folha, o conteúdo, a impressão e a exportação são preservados.
- **Janelas:** moldura FlatLaf no Windows e moldura do sistema no Linux e no
  macOS (`-Dbrmodelo.decoracoes=modernas|nativas` muda o padrão); no macOS, os
  menus ficam na barra do sistema.
- **Diagrama:** ícones de ação da seleção e chaves das tabelas vetoriais, nítidos
  em qualquer zoom, em HiDPI e na exportação; a faixa de ícones vai para um lado
  livre da forma. Inspector com aparência própria para cada tipo de propriedade;
  editores de IR em layout mestre/detalhe.
- **Desktop:** configurações, recuperação e modelos reutilizáveis em pastas
  próprias do NG: XDG no Linux/Unix, Application Support no macOS e LocalAppData
  no Windows; cópia inicial dos arquivos legados da pasta de execução.
- **Navegação:** roda do mouse rola verticalmente por padrão; Shift permite
  rolagem horizontal.
- **Desempenho e estrutura:** diálogo e descoberta de impressoras adiados até o
  primeiro uso (janela visível em 0,9 s, contra 3,1 s no oficial 3.3.2); responsabilidades de classes
  grandes extraídas para auxiliares, preservando a forma serializada. A tela
  pinta só as linhas visíveis da grade, os ícones SVG da interface são
  renderizados uma vez por escala e tema, e no Linux o Java2D usa padrões que
  evitam leituras de volta do servidor X ao redimensionar.
- **Documentação:** README em português com resumo em inglês, capturas dos
  exemplos versionados, instruções de uso, compilação e formatos, e as outras
  versões do brModelo.

### Fixed

- Prévia de impressão e pintura externa alterando dimensões, posições ou cache
  de texto do diagrama; posicionamento inicial do texto nas formas retangulares.
- Salvamento de texto acrescentando conteúdo ao arquivo existente em vez de
  substituí-lo após a confirmação.
- Salvamento automático concorrendo com a edição do diagrama; gravações agora
  usam snapshots capturados na thread da interface e uma fila de escrita, com
  espera pela conclusão ao sair.
- Atualizações de status fora da thread da interface e criação de threads de
  temporizador que permaneciam ativas.
- Campos e botões cortados com fontes maiores, colunas de PK/FK/Unique
  desalinhadas, barras sobrepostas na impressão e coluna do Inspector estreita.
- Cores e separadores incompatíveis com temas escuros; barras de ferramentas
  que podiam se soltar em janelas flutuantes; grafia de “Mostrar”.
- Diagramas grandes e muito interligados (cerca de 2.000 itens) estouravam a
  pilha ao abrir, salvar e desfazer, como no oficial; a serialização roda numa
  thread com pilha maior, sem mudar o formato.
- Visualizador de código cortando a última letra de uma palavra-chave no início
  da linha.
- Seleção do visual GTK em desktops Linux além do GNOME, ao optar pelo visual
  do sistema, com alternativa quando GTK não está disponível.

### Security

- Leitura de diagramas binários, streams internos, modelos reutilizáveis,
  recuperação e área de transferência com validação de classes antes da
  resolução e rejeição de proxies; entradas inválidas de recuperação são
  ignoradas individualmente.
- JSON usa a mesma regra de classes permitidas e valida campos, tipos,
  referências, números e versão antes de construir objetos do modelo.

### Removed

- Código morto e trechos comentados sem uso, sem alterar campos persistidos.
- Visualizador de ajuda legado e o arquivo `Ajuda.brMh`, substituídos pelo site
  gerado de `docs/ajuda/`.
- Build Ant/NetBeans e jars de dependências versionados; o projeto usa o Gradle
  Wrapper e Maven Central, preservando os arquivos `.form` para edição visual.

[1.0.0]: https://github.com/kpagnussat/brModelo-NG/releases/tag/v1.0.0
