#!/usr/bin/env python3
"""Generates the SVG icon theme in src/imagens/svg/ (one SVG per interface PNG, same base name).

Usage: python3 dev/icones/gerar_svg.py <lucide-static>/icons

- Generic actions come from Lucide (https://lucide.dev, ISC license): the stroke color is
  set to one of the IntelliJ "Actions" colors below, which FlatLaf's FlatSVGIcon swaps for
  the matching dark-theme color automatically. No theme code is needed in the app.
- Diagram shapes (entity, relationship, attribute...) are drawn here in the same style:
  24x24 view box, 2px round stroke.
Diagram icons use the same SVG sources with fixed paper colors (util.Icones.noPapel),
rendered directly through JSVG rather than FlatLaf's theme filter.
"""
import os
import re
import sys

CINZA = "#6E6E6E"     # Actions.Grey  -> #AFB1B3 on dark
VERDE = "#59A869"     # Actions.Green -> #499C54
VERMELHO = "#DB5860"  # Actions.Red   -> #C75450
AZUL = "#389FD6"      # Actions.Blue  -> #3592C4
AMARELO = "#EDA200"   # Actions.Yellow

# PNG base name -> (Lucide icon, color)
LUCIDE = {
    # In-paper selection actions; disabled variants keep the same shape in muted grey.
    "ancorar": ("anchor", CINZA),
    "ancorabor": ("eraser", VERMELHO), # deletes the selected shape
    "orgat": ("list-tree", CINZA),
    "see": ("eye", CINZA),
    "ddl": ("database", CINZA),
    "upA": ("arrow-up", CINZA),
    "downA": ("arrow-down", CINZA),
    "excluirA": ("trash-2", VERMELHO),
    "upA0": ("arrow-up", "#B8BEC5"),
    "downA0": ("arrow-down", "#B8BEC5"),
    "excluirA0": ("trash-2", "#B8BEC5"),
    "pencil": ("pencil", CINZA),
    "crosshair": ("crosshair", CINZA),
    "sun-moon": ("sun-moon", CINZA),
    "chevron-right": ("chevron-right", CINZA),
    "chevron-down": ("chevron-down", CINZA),
    # status bar: unread entries use shape and count as well as action colors
    "log_lido": ("info", CINZA),
    "log_info": ("info", AZUL),
    "log_erro": ("circle-alert", VERMELHO),
    # toolbar: file
    "menu_novo": ("file-plus", CINZA),
    "menu_abrir": ("folder-open", AMARELO),
    "menu_fechar": ("file-x", CINZA),
    "menu_imprimir": ("printer", CINZA),
    "menu_exportar": ("file-output", CINZA),
    "menu_salvar": ("save", CINZA),
    "menu_salvarc": ("file-pen-line", CINZA),
    "menu_salvart": ("save-all", CINZA),
    # toolbar: edit
    "undo_16": ("undo-2", CINZA),
    "redo_16": ("redo-2", CINZA),
    "cut_16": ("scissors", CINZA),
    "copy": ("copy", CINZA),
    "cp": ("copy", CINZA),
    "pastex": ("clipboard-paste", CINZA),
    "copyimg": ("image-down", CINZA),
    "cpdim_cp": ("paintbrush", CINZA),
    "Pastef": ("clipboard-pen-line", CINZA),
    "Borracha": ("eraser", CINZA),
    "BBorracha": ("eraser", CINZA),
    "destaque": ("highlighter", CINZA),
    # toolbar: selection and arrangement
    "all": ("square-dashed-mouse-pointer", CINZA),
    "allt": ("boxes", CINZA),
    "prox": ("arrow-right-from-line", CINZA),
    "ant": ("arrow-left-from-line", CINZA),
    "bring": ("bring-to-front", CINZA),
    "send": ("send-to-back", CINZA),
    "ma0": ("chevron-left", CINZA),
    "ma1": ("chevron-up", CINZA),
    "ma2": ("chevron-down", CINZA),
    "ma3": ("chevron-right", CINZA),
    "cpdim_left": ("align-start-vertical", CINZA),
    "cpdim_top": ("align-start-horizontal", CINZA),
    "cpdim_right": ("align-end-vertical", CINZA),
    "cpdim_bottom": ("align-end-horizontal", CINZA),
    "cpdim_width": ("move-horizontal", CINZA),
    "cpdim_height": ("move-vertical", CINZA),
    "cpdim_h": ("align-center-horizontal", CINZA),
    "cpdim_v": ("align-center-vertical", CINZA),
    "zoom": ("zoom-in", CINZA),
    "zoommenos": ("zoom-out", CINZA),
    # diagram commands
    "orgtabs": ("layout-grid", CINZA),
    "sql": ("database", CINZA),
    "editar": ("square-pen", CINZA),
    "editarT": ("table-properties", CINZA),
    "fluxo": ("workflow", CINZA),
    "diagrama": ("shapes", CINZA),
    "eap": ("network", CINZA),
    # palette tools that are generic
    "Mouse": ("mouse-pointer-2", CINZA),
    "Imagem": ("image", CINZA),
    "drawer": ("pen-tool", CINZA),
    "Texto": ("type", CINZA),
    "TextoSimples": ("text", CINZA),
    "Legenda": ("list", CINZA),
    "Comentario": ("sticky-note", CINZA),
    "Tabela": ("table", CINZA),
    "Campo": ("rectangle-ellipsis", CINZA),
    "DiagramaLigacao": ("arrow-down", CINZA),
    # dialogs and menus
    "add_16": ("plus", VERDE),
    "mais": ("plus", VERDE),
    "menos": ("minus", VERMELHO),
    "busy": ("circle-minus", VERMELHO),
    "excluir": ("x", VERMELHO),
    "xis": ("x", CINZA),
    "check": ("check", VERDE),
    "ok": ("check", VERDE),
    "error": ("circle-x", VERMELHO),
    "ajuda": ("circle-question-mark", AZUL),
    "atualizar": ("refresh-cw", CINZA),
    "download": ("download", CINZA),
    "up": ("arrow-up", CINZA),
    "down": ("arrow-down", CINZA),
    "edit": ("pencil", CINZA),
    "mini_edit": ("pencil", CINZA),
    "green_edit": ("pencil", VERDE),
    "find": ("search", CINZA),
    "first": ("chevron-first", CINZA),
    "last": ("chevron-last", CINZA),
    "next": ("chevron-right", CINZA),
    "prior": ("chevron-left", CINZA),
    "sign-out": ("log-out", CINZA),
}

# Diagram shapes drawn for this theme: PNG base name -> SVG body (stroke style comes from the
# root element; "P" marks a filled shape in the ink color).
P = 'fill="%s" stroke="none"' % CINZA
CHAVE = '<circle cx="8" cy="8" r="4.5"/><path d="m11.2 11.2 9 9M17 17l2.5-2.5M14.5 14.5l2-2"/>'
DESENHADOS = {
    # Application icon uses the same ER vocabulary as the conceptual-diagram icon.
    "icone": '<rect x="2" y="9" width="7" height="6" rx="1"/><path d="M9 12h2.5"/><path d="M16.5 7l5 5-5 5-5-5Z"/>',
    # conceptual (ER)
    "Entidade": '<rect x="3" y="6" width="18" height="12" rx="1"/>',
    "Relacionamento": '<path d="M12 4 21 12 12 20 3 12Z"/>',
    "AutoRelacionamento": '<path d="M9 7 14 12 9 17 4 12Z"/><path d="M14 12h7V4H9v3"/>',
    "Entidade_Associativa": '<rect x="2" y="5" width="20" height="14" rx="1"/><path d="M12 8 17 12 12 16 7 12Z"/>',
    "Atributo": '<circle cx="12" cy="7" r="3.5"/><path d="M12 10.5V21"/>',
    "Atributo_Multivalorado": '<circle cx="7" cy="6" r="3"/><path d="M7 9v12M7 15h6.5"/><circle cx="16.5" cy="15" r="3"/>',
    "Especializacao": '<path d="M12 3v4"/><path d="M4 19 12 7l8 12Z"/>',
    "EspecializacaoE": '<path d="M12 2v4"/><path d="M5 17 12 6l7 11Z"/><path d="M12 17v5"/>',
    "EspecializacaoD": '<path d="M12 2v4"/><path d="M5 17 12 6l7 11Z"/><path d="M7 17v5M17 17v5"/>',
    "Uniao": '<path d="M3 5h18l-9 15Z"/><path d="M9.5 8v3a2.5 2.5 0 0 0 5 0V8"/>',
    "Uniao_Entidades": '<path d="M4 10h16l-8 10Z"/><path d="M8 10 5 3M16 10l3-7"/>',
    "Cardinalidade": '<path d="M4 8l2.5-2V18"/><circle cx="11" cy="9.5" r="1.2" %s/><circle cx="11" cy="15" r="1.2" %s/><path d="M15 18V6l5 12V6"/>' % (P, P),
    "mer": '<rect x="2" y="9" width="7" height="6" rx="1"/><path d="M9 12h2.5"/><path d="M16.5 7l5 5-5 5-5-5Z"/>',
    "logico": '<rect x="2" y="3" width="8" height="10" rx="1"/><path d="M2 6.5h8"/><rect x="14" y="11" width="8" height="10" rx="1"/><path d="M14 14.5h8"/><path d="M10 8h2v9h2"/>',
    # Shared logical symbols: palette, field rows, constraints and IR editors.
    # ir_pk aliases CampoK; ir_fk aliases CampoKFK in util.Icones (no duplicate sources).
    # Unique retains its U shape; composites reuse exactly the same key drawing.
    "ancorar2": '<circle cx="12" cy="5" r="3"/><path d="M12 8v13M5 12H2a10 10 0 0 0 20 0h-3M2 2l20 20"/>',
    "CampoUN": '<path d="M6 4v10a6 6 0 0 0 12 0V4"/>',
    "CampoUNFK": ('<g transform="translate(-1 -1) scale(.62)" stroke-width="3.2"><path d="M6 4v10a6 6 0 0 0 12 0V4"/></g>'
                  '<g transform="translate(8 8) scale(.62)" stroke-width="3.2" stroke="%s">%s</g>' % (VERDE, CHAVE)),
    "CampoK": CHAVE,
    "CampoFK": '<g stroke="%s">%s</g>' % (VERDE, CHAVE),
    "CampoKFK": ('<g transform="translate(-1 -1) scale(.62)" stroke-width="3.2">%s</g>'
                 '<g transform="translate(8 8) scale(.62)" stroke-width="3.2" stroke="%s">%s</g>' % (CHAVE, VERDE, CHAVE)),
    # lines
    "Linha": '<path d="M4 7h7v10h9"/><circle cx="4" cy="7" r="1.6" %s/><circle cx="20" cy="17" r="1.6" %s/>' % (P, P),
    "LinhaSimples": '<path d="M3 7h8v10h10"/>',
    "LinhaApenso": '<path d="M3 17h9V9"/><rect x="12" y="4" width="9" height="7" rx="1"/>',
    "EapBarraLigacao": '<path d="M5 3v18M5 5h14M5 12h14M5 19h14"/>',
    "Juncao": '<circle cx="12" cy="12" r="5"/><path d="M12 3v18M3 12h18"/>',
    # free shapes
    "Retangulo": '<rect x="3" y="6" width="18" height="12"/>',
    "RetanguloA": '<rect x="3" y="6" width="18" height="12" rx="4"/>',
    "Triangulo": '<path d="M12 4 21 20H3Z"/>',
    "Circulo": '<circle cx="12" cy="12" r="8"/>',
    "Losango": '<path d="M12 3 21 12 12 21 3 12Z"/>',
    "Notas": '<path d="M3 6c3-2 6 2 9 0s6-2 9 0v12c-3-2-6 2-9 0s-6-2-9 0Z"/>',
    "Documento": '<path d="M3 5h18v12c-3-2-6 2-9 0s-6-2-9 0Z"/>',
    "VDocumentos": '<path d="M7 3h14v11"/><path d="M5 5.5h14v11"/><path d="M3 8h14v11c-2.3-1.5-4.6 1.5-7 0s-4.7-1.5-7 0Z"/>',
    # flowchart
    "Processo": '<rect x="3" y="6" width="18" height="12" rx="1"/><path d="M6 6v12M18 6v12"/>',
    "IniFim": '<rect x="2" y="7" width="20" height="10" rx="5"/>',
    "Conector": '<circle cx="12" cy="12" r="6"/>',
    "DiagramaDecisao": '<path d="M12 3 21 12 12 21 3 12Z"/>',
    "FluxSeta": '<path d="M7 3v18M3 17l4 4 4-4"/><path d="M17 5l4 4-4 4-4-4Z"/>',
    # activity
    "atividade": '<circle cx="12" cy="3.5" r="2" %s/><path d="M12 5.5V8"/><rect x="5" y="8" width="14" height="6" rx="3"/><path d="M12 14v2.5"/><path d="M12 16.5l3.5 3.5-3.5 3.5-3.5-3.5Z"/>' % P,
    "Estado": '<rect x="2" y="6" width="20" height="12" rx="6"/>',
    "Inicio": '<circle cx="12" cy="12" r="7" %s/>' % P,
    "Fim": '<circle cx="12" cy="12" r="8.5"/><circle cx="12" cy="12" r="4.5" %s/>' % P,
    "ForkJoin": '<rect x="2" y="10.5" width="20" height="3" rx="1" %s/><path d="M8 3v6M5.5 6.5 8 9l2.5-2.5M16 15v6M13.5 18.5 16 21l2.5-2.5"/>' % P,
    "SetaAtividade": '<path d="M5 3v18M1.5 17.5 5 21l3.5-3.5"/><path d="M14 8h-2v7h2M18 8h2v7h-2"/>',
    "Raia": '<rect x="3" y="3" width="18" height="18" rx="1"/><path d="M3 8h18M9 8v13M15 8v13"/>',
}

CABECALHO = ('<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" '
             'fill="none" stroke="{cor}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">')


def lucide(pasta, nome, cor):
    with open(os.path.join(pasta, nome + ".svg"), encoding="utf-8") as f:
        svg = f.read()
    corpo = svg[svg.index(">", svg.index("<svg")) + 1:svg.rindex("</svg>")]
    corpo = re.sub(r"\s+", " ", corpo).strip()
    return ("<!-- Lucide \"%s\" (ISC license, see LICENSE-lucide.txt) -->\n" % nome
            + CABECALHO.format(cor=cor) + corpo + "</svg>\n")


def desenhado(corpo):
    return CABECALHO.format(cor=CINZA) + corpo + "</svg>\n"


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    pasta = sys.argv[1]
    raiz = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    saida = os.path.join(raiz, "src", "imagens", "svg")
    os.makedirs(saida, exist_ok=True)
    repetidos = set(LUCIDE) & set(DESENHADOS)
    if repetidos:
        sys.exit("both Lucide and drawn: %s" % sorted(repetidos))
    for nome, (icone, cor) in LUCIDE.items():
        with open(os.path.join(saida, nome + ".svg"), "w", encoding="utf-8") as f:
            f.write(lucide(pasta, icone, cor))
    for nome, corpo in DESENHADOS.items():
        with open(os.path.join(saida, nome + ".svg"), "w", encoding="utf-8") as f:
            f.write(desenhado(corpo))
    with open(os.path.join(os.path.dirname(pasta), "LICENSE"), encoding="utf-8") as f:
        licenca = f.read()
    with open(os.path.join(saida, "LICENSE-lucide.txt"), "w", encoding="utf-8") as f:
        f.write(licenca)
    print("%d Lucide + %d drawn icons in %s" % (len(LUCIDE), len(DESENHADOS), saida))


if __name__ == "__main__":
    main()
