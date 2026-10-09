# brModelo JSON (`.brMj`), versão 1

`.brMj` é o formato opcional de texto do brModelo NG. Ele armazena o grafo do diagrama
em JSON legível, com referências explícitas, para revisão de diferenças e uso em
Git. O brModelo oficial continua recebendo `.brM3`: escolha esse formato em
**Salvar como** para converter de volta. O oficial não abre `.brMj`.

**Abrir** aceita `.brM3`, `.brMj` e o XML existente. O filtro “Todos os arquivos
brModelo” inclui os três. **Salvar como** oferece binário primeiro e selecionado
por padrão, JSON em segundo e XML em terceiro. **Salvar** e **Salvar todos** usam
a extensão do arquivo atual; assim um arquivo aberto como JSON continua JSON.
Novos diagramas seguem o padrão `.brM3`. Não há um segundo comando de exportação
JSON. Autosave, recuperação e clipboard continuam usando seus formatos anteriores.

## Documento

O arquivo é UTF-8, sem BOM, com recuo de dois espaços e uma quebra de linha final.
As chaves de todos os objetos JSON são ordenadas lexicograficamente. A estrutura é:

| Campo | Conteúdo |
| --- | --- |
| `formato` | Literal `brModelo-json` |
| `versao` | Inteiro `1`, versão deste formato |
| `app` | Identificador legado do aplicativo no documento, atualmente `3.2.0`; independente da versão do pacote |
| `envelope` | `versao`, `versaoDiagrama`, `autor`, `data` e `Tag` de `GuardaPadraoBrM` |
| `raiz` | Referência ao objeto que é o diagrama |
| `objetos` | Lista de registros com `id`, `classe` e a representação do conteúdo |

O `byte[] diagrama` do envelope binário é substituído por esse grafo; não há um
stream Java oculto no JSON. `data` é o metadado histórico do envelope, não um
horário gerado a cada gravação. Metadados podem ser strings ou `null`. O codec
aceita um envelope existente e o preserva; gravações pelo editor criam os mesmos
metadados padrão que a gravação binária. `Documento.paraBrM3()` reconstrói o
stream binário e conserva os metadados recebidos.

## Campos e valores

Cada objeto do modelo tem `campos`: os campos de instância não estáticos e não
transientes em toda a hierarquia serializável, incluindo os privados e finais.
Um nome que ocorre em mais de uma classe é qualificado pelo nome completo da
classe que o declara (`controlador.Diagrama.classesDoDiagrama`, por exemplo).
Nomes únicos permanecem simples. Não são persistidos getters, propriedades
calculadas, valores estáticos ou o estado transiente do editor.

| Valor | Representação |
| --- | --- |
| Nulo, booleano, string | JSON nativo |
| Campo ou elemento de array numérico de tipo conhecido | Número JSON; inteiros de 64 bits não passam por `double` |
| Número em posição sem tipo declarado, como uma lista ou um campo `Object` | `{"numero":"java.lang.Long","valor":9223372036854775807}`; a marca conserva o tipo do wrapper |
| Float/double não finito | Marca `numero` com `valor` igual a `NaN`, `Infinity` ou `-Infinity` como string |
| Zero negativo | Número `-0.0` em posições tipadas; marca `numero` com string `-0.0` em posições sem tipo |
| Caractere | `{"caractere":"ç"}`; uma unidade UTF-16, inclusive surrogate isolado |
| Enum | `{"enum":"pkg.Tipo","nome":"CONSTANTE"}` |
| Referência | `{"ref":17}`; compartilhamento e ciclos são preservados |
| `Class` | `{"class":"pkg.Classe"}`; admite também os nomes JVM dos arrays autorizados |
| `TextAttribute` | `{"atributoTexto":"WEIGHT"}`, identificado por sua constante pública |

Números finitos usam a representação decimal Java que permite recuperar o mesmo
`long`, `float` ou `double`. O parser conserva números como `BigDecimal` até a
conversão para o tipo do campo, verifica limites e conserva o sinal do zero.

Arrays, listas, mapas e valores AWT também recebem ids; referências compartilhadas
a esses objetos são preservadas. Os registros mantêm a classe concreta:

| Classe/conteúdo | Campo no registro |
| --- | --- |
| Array, incluindo arrays de arrays | `itens`, na ordem original; classe no nome JVM, como `[I` |
| `byte[]` | `base64`, Base64 padrão |
| `ArrayList` | `itens`, na ordem original |
| `HashMap`, `Hashtable` | `entradas`, lista de pares `chave`/`valor` ordenada pelas chaves antes de atribuir ids |
| `Color` | `valor.rgba`, inteiro ARGB de `getRGB()` com alpha |
| `Point`, `Rectangle`, `Dimension` | `valor` com coordenadas/dimensões públicas |
| `Font` | `valor` com `nome`, `estilo`, `tamanho` e `atributos`, preservando também tamanho fracionário, tracking etc. |
| `Cursor` | `valor.tipo`, um dos cursores predefinidos `0..13` |
| `Polygon` | `valor.pontos`, pares `[x,y]` dos pontos utilizados |
| `Path2D.Float`, `Path2D.Double`, `GeneralPath` | `valor.regra` e `valor.segmentos`, obtidos pelo `PathIterator`, sem achatar curvas |
| `Ellipse2D.Float`, `Rectangle2D.Float`, `RoundRectangle2D.Float` | `valor` com os campos públicos, incluindo `arcwidth`/`archeight` no último |

Um segmento de caminho começa pelo código do `PathIterator` (`0` move, `1` line,
`2` quad, `3` cubic, `4` close), seguido por 2, 2, 4, 6 ou 0 coordenadas,
respectivamente. A regra é `0` (even-odd) ou `1` (non-zero).

Os ids começam em 1 e são atribuídos na primeira visita, percorrendo os campos
ordenados e preservando a ordem das sequências. Mapas com chaves de objetos usam
sua representação estrutural para ordenar; valores e ids já visitados desempatam
chaves estruturalmente iguais. Uma gravação repetida do mesmo grafo produz bytes
idênticos. Ao salvar pelo editor, o marcador `mudou` é gravado como falso para que
salvar novamente o documento já salvo não crie uma diferença; em caso de falha,
o marcador original é restaurado. O codec isolado conserva esse campo como
qualquer outro campo serializável.

## Leitura e rejeição

A regra de classes é **a mesma** de `util.LeitorSeguro.permitido`, incluindo a
verificação dos componentes de arrays. O nome é verificado antes de resolver a
classe; não há uma segunda lista de classes autorizadas no codec JSON. Os
adaptadores acima definem as representações dos valores JDK aceitos.

O documento inteiro passa por validação de classes, campos, tipos e referências
antes de alocar objetos do modelo. Campos desconhecidos ou ausentes, ids
repetidos, referências inexistentes, enums inválidos, números fora dos limites,
chaves JSON repetidas e versões desconhecidas são erros. Strings devem ser UTF-8
válido. O parser limita o aninhamento sintático a 128 níveis; a profundidade dos
ciclos do modelo não aumenta o aninhamento do JSON, pois usa referências.

A reconstrução usa `ReflectionFactory.newConstructorForSerialization` de
`jdk.unsupported` no Java 21. Assim, roda o construtor sem argumentos da primeira
superclasse não serializável, sem rodar o construtor da classe serializável.
Campos transientes permanecem nos defaults da desserialização Java. O diagrama
só é instalado no editor depois de uma leitura completa; uma rejeição retorna um
erro claro e não instala um diagrama parcial. Não é preciso `--add-opens`.

A versão 1 representa cores em RGBA e cursores predefinidos; não representa
ColorSpaces personalizados, cursores customizados nem coordenadas AWT não
finitas. Os campos numéricos do modelo e os wrappers aceitam os valores não
finitos acima.

## Exemplo extraído de fixture

Estes são recortes reais da conversão de
`test-resources/fixtures/livre.brM3` (o exemplo inventado da Escola/Biblioteca
Aurora). O cabeçalho, sem a lista `objetos`, é:

```json
{
  "app": "3.2.0",
  "envelope": {
    "Tag": "3.2.0",
    "autor": "Carlos Henrique Cândido",
    "data": "13-02-2012",
    "versao": "1.0.0",
    "versaoDiagrama": "3.2.0"
  },
  "formato": "brModelo-json",
  "raiz": {
    "ref": 1
  },
  "versao": 1
}
```

O objeto 1 é `diagramas.livre.DiagramaLivre`. Seu campo `DiagramaDownPos`
referencia o seguinte registro da lista `objetos`:

```json
{
  "classe": "java.awt.Point",
  "id": 2,
  "valor": {
    "x": 0,
    "y": 0
  }
}
```

Esses recortes não são um documento completo carregável. Para gerar os exemplos
completos dos seis fixtures e verificar a conversão em ambas as direções:

```sh
export JAVA_HOME="/caminho/para/jdk21"
./gradlew verificaBrMj -Pargs="test-resources/fixtures" \
  -PoficialJar="/caminho/para/brModelo-oficial.jar"
```

Os arquivos `.brMj`, `.roundtrip.brM3` e o relatório por arquivo são escritos em
`build/verificaBrMj/conversoes`. A tarefa usa o dump canônico dos testes, verifica
igualdade dos bytes em gravações repetidas e após releitura e, com `oficialJar`,
verifica também leitura/escrita pelo jar oficial isolado. Os arquivos de entrada
são somente lidos e checados novamente ao final de cada conversão.
