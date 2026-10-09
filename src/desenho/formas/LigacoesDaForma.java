package desenho.formas;

import desenho.ElementarListener;
import desenho.linhas.Linha;
import desenho.linhas.PontoDeLinha;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Queries connected shapes and arranges their line endpoints and flow alignment. */
final class LigacoesDaForma {
    private LigacoesDaForma() {}

    static ArrayList<PontoDeLinha> getListaDePontosLigados(Forma alvo) {
        ArrayList<PontoDeLinha> res = new ArrayList<>();
        List<ElementarListener> lst = alvo.getListeners();
        if (lst != null) {
            for (ElementarListener el : lst) {
                if (el instanceof PontoDeLinha && ((PontoDeLinha) el).getEm() == alvo) {
                    res.add((PontoDeLinha) el);
                }
            }
        }
        return res;
    }

    static List<Linha> getListaDeLigacoes(Forma alvo) {
        return alvo.getListaDePontosLigados().stream().map(p -> p.getDono()).collect(Collectors.toList());
    }

    static ArrayList<Forma> getListaDeFormasLigadas(Forma alvo, Forma exceto) {
        if (exceto == null) {
            return alvo.getListaDeFormasLigadas();
        }
        ArrayList<PontoDeLinha> pontos = alvo.getListaDePontosLigados();
        ArrayList<Forma> outrasPontas = new ArrayList<>();
        for (PontoDeLinha pt : pontos) {
            Forma op = pt.getDono().getOutraPonta(alvo);
            if (op != null && op != exceto && outrasPontas.indexOf(op) == -1) {
                outrasPontas.add(op);
            }
        }
        return outrasPontas;
    }

    static ArrayList<Forma> getListaDeFormasLigadasNaoExclusiva(Forma alvo, Class destaClasse) {
        ArrayList<PontoDeLinha> pontos = alvo.getListaDePontosLigados();
        ArrayList<Forma> outrasPontas = new ArrayList<>();
        for (PontoDeLinha pt : pontos) {
            Forma op = pt.getDono().getOutraPonta(alvo);
            if (op != null) {
                if (destaClasse.isAssignableFrom(op.getClass())) {
                    outrasPontas.add(op);
                }
            }
        }
        return outrasPontas;
    }

    static ArrayList<Forma> getListaDeFormasLigadas(Forma alvo, Class destaClasse) {
        ArrayList<PontoDeLinha> pontos = alvo.getListaDePontosLigados();
        ArrayList<Forma> outrasPontas = new ArrayList<>();
        for (PontoDeLinha pt : pontos) {
            Forma op = pt.getDono().getOutraPonta(alvo);
            if (op != null) {
                if (destaClasse.isAssignableFrom(op.getClass())) {
                    if (outrasPontas.indexOf(op) == -1) {
                        outrasPontas.add(op);
                    }
                }
            }
        }
        return outrasPontas;
    }

    static ArrayList<Forma> getListaDeFormasLigadas(Forma alvo) {
        ArrayList<PontoDeLinha> pontos = alvo.getListaDePontosLigados();
        ArrayList<Forma> outrasPontas = new ArrayList<>();
        for (PontoDeLinha pt : pontos) {
            Forma op = pt.getDono().getOutraPonta(alvo);
            if (op != null && outrasPontas.indexOf(op) == -1) {
                outrasPontas.add(op);
            }
        }
        return outrasPontas;
    }

    static int MapaPosi(Forma A, Forma B) {
        Point posiA = A.getLocation();
        Point posiB = B.getLocation();

        //  1       2       3
        //  0       A       4
        //  7       6       5
        int dist = Math.max(A.getWidth(), B.getWidth());
        if (Math.abs(posiA.x - posiB.x) < dist) {
            //mesma coluna
            if (posiA.y > posiB.y) {
                return 2;
            } else {
                return 6;
            }
        }

        dist = Math.max(A.getHeight(), B.getHeight());
        if (Math.abs(posiA.y - posiB.y) < dist) {
            //mesma linha
            if (posiA.x > posiB.x) {
                return 0;
            } else {
                return 4;
            }
        }

        if (posiA.x < posiB.x) {
            // 3 ou 5
            if (posiA.y < posiB.y) {
                return 5;
            } else {
                return 3;
            }
        } else {
            // 1 ou 7
            if (posiA.y > posiB.y) {
                return 1;
            } else {
                return 7;
            }
        }
    }

    static void OrganizeDiagrama(Forma alvo, boolean movA, boolean movB) {
        final Forma A = alvo;
        A.getListaDePontosLigados().stream().map(p -> p.getDono()).forEach(L -> {
            Forma B = L.getOutraPonta(A);
            if (B == null) {
                B = A;
            }
            int sp = 0;
            int m = Forma.MapaPosi(A, B);

            //  1.      2      .3
            //  0.      A      .4
            //  7.      6      .5
            Point ptA = new Point();
            Point ptB = new Point();
            int lpA = 0;
            int lpB = 0;
            switch (m) {
                case 1:
                case 0:
                case 7:
                    ptA = new Point(A.getLeft() + sp, A.getTop() + A.getHeight() / 2);
                    ptB = new Point(B.getLeftWidth() - sp, B.getTop() + B.getHeight() / 2);
                    lpA = 0;
                    lpB = 2;
                    break;

                case 2:
                    ptA = new Point(A.getLeft() + A.getWidth() / 2, A.getTop() + sp);
                    ptB = new Point(B.getLeft() + B.getWidth() / 2, B.getTopHeight() - sp);
                    lpA = 1;
                    lpB = 3;
                    break;

                case 3:
                case 4:
                case 5:
                    ptA = new Point(A.getLeftWidth() - sp, A.getTop() + A.getHeight() / 2);
                    ptB = new Point(B.getLeft() + sp, B.getTop() + B.getHeight() / 2);
                    lpA = 2;
                    lpB = 0;
                    break;

                case 6:
                    ptA = new Point(A.getLeft() + A.getWidth() / 2, A.getTopHeight() - sp);
                    ptB = new Point(B.getLeft() + B.getWidth() / 2, B.getTop() + sp);
                    lpA = 3;
                    lpB = 1;
                    break;
            }

            if (L.getPontaA().getEm() == A) {
                if (movA) {
                    L.getPontaA().setLado(lpA);
                    L.getPontaA().setCentro(ptA);
                }
                if (movB) {
                    L.getPontaB().setLado(lpB);
                    L.getPontaB().setCentro(ptB);
                }

            } else {
                if (movB) {
                    L.getPontaA().setLado(lpB);
                    L.getPontaA().setCentro(ptB);
                }
                if (movA) {
                    L.getPontaB().setLado(lpA);
                    L.getPontaB().setCentro(ptA);
                }
            }
        });
        organizeDiagramaRedistribuaLinhas(alvo);
        alvo.DoMuda();
    }

    static void organizeDiagramaRedistribuaLinhas(Forma alvo) {
        Forma tt = alvo;
        for (int lado = 0; lado < 4; lado++) {
            final int ld = lado;
            final boolean sn = ld % 2 == 0;
            int tl = Math.toIntExact(tt.getListaDePontosLigados().stream().filter(p -> p.getLado() == ld).count());
            if (tl == 0) {
                continue;
            }

            final int espaco = ((sn ? tt.getHeight() : tt.getWidth()) - 2 * alvo.INI_ORGDIAG) / (tl + 1);
            int ini = alvo.INI_ORGDIAG;

            if (sn) {
                ini += tt.getTop();
            } else {
                ini += tt.getLeft();
            }

            List<PontoDeLinha> ord = tt.getListaDePontosLigados().stream().filter(p -> p.getLado() == ld).sorted((p1, p2) -> {
                if (sn) {
                    return Integer.compare(p1.getDono().getOutraPonta(p1).getTop(), p2.getDono().getOutraPonta(p2).getTop());
                } else {
                    return Integer.compare(p1.getDono().getOutraPonta(p1).getLeft(), p2.getDono().getOutraPonta(p2).getLeft());
                }

            }).collect(Collectors.toList());

            for (PontoDeLinha p : ord) {
                ini += espaco;
                if (sn) {
                    p.setTop(ini);
                } else {
                    p.setLeft(ini);
                }
                p.getDono().OrganizeLinha();
                p.getDono().reSetBounds();
            }
        }

    }

    static void OrganizeFluxo(Forma alvo) {
        List<Forma> lst = new ArrayList<>();
        lst.add(alvo);
        alvo.getListaDeFormasLigadas().forEach(item -> {
            alvo.OrganizeFluxo(alvo, item, lst);
        });
    }

    static void OrganizeFluxo(Forma alvo, Forma origem, Forma dest, List<Forma> lstJA) {
        if (lstJA.indexOf(dest) > -1) {
            return; // já.
        }
        origem.getListaDeLigacoes().stream().filter(L -> L.getOutraPonta(origem) == dest).forEach(lin -> {
            int pa = lin.getPontaA().getLado();
            int pb = lin.getPontaB().getLado();

            boolean sim = false;

            switch (pa) {
                case 0:
                    sim = (pb == 2);
                    break;
                case 1:
                    sim = (pb == 3);
                    break;
                case 2:
                    sim = (pb == 0);
                    break;
                case 3:
                    sim = (pb == 1);
                    break;
            }

            if (sim) {
                Point PA = lin.getPontaA().getLocation();
                Point PB = lin.getPontaB().getLocation();

                int x = 0, y = 0;
                if (pa == 0 || pa == 2) {
                    y = PA.y - PB.y;
                    if (lin.getPontaB().getEm() == origem) {
                        y = PB.y - PA.y;
                    }
                } else {
                    x = PA.x - PB.x;
                    if (lin.getPontaB().getEm() == origem) {
                        x = PB.x - PA.x;
                    }
                }
                dest.DoMove(x, y);
                dest.Reenquadre();
            }
            lstJA.add(dest);

            dest.getListaDeFormasLigadas().forEach(f -> {
                alvo.OrganizeFluxo(dest, f, lstJA);
            });
        });
    }
}
