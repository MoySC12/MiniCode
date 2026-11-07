import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Optimizador {

    // Registro de cada línea que sufrió una sustitución o eliminación
    public static class CambioOptimizacion {
        public int numeroLinea;
        public String tecnica;
        public String lineaOriginal;
        public String lineaOptimizada;

        public CambioOptimizacion(int numeroLinea, String tecnica, String lineaOriginal, String lineaOptimizada) {
            this.numeroLinea = numeroLinea;
            this.tecnica = tecnica;
            this.lineaOriginal = lineaOriginal;
            this.lineaOptimizada = lineaOptimizada;
        }
    }

    // Resultado global que contiene el texto optimizado y la tabla de cambios
    public static class ResultadoOptimizacion {
        public String codigoOptimizado;
        public List<CambioOptimizacion> cambios;

        public ResultadoOptimizacion(String codigoOptimizado, List<CambioOptimizacion> cambios) {
            this.codigoOptimizado = codigoOptimizado;
            this.cambios = cambios;
        }
    }

    // Estructura interna para rastrear la evolución de cada línea del código
    private static class EstadoLinea {
        int numeroLinea;
        String original;
        String actual;
        Set<String> tecnicas = new LinkedHashSet<>();
        boolean eliminada = false;

        EstadoLinea(int numeroLinea, String original) {
            this.numeroLinea = numeroLinea;
            this.original = original;
            this.actual = original;
        }
    }

    public static String optimizarCodigo(String codigoFuente) {
        return optimizarConDetalle(codigoFuente).codigoOptimizado;
    }

    public static ResultadoOptimizacion optimizarConDetalle(String codigoFuente) {
        List<CambioOptimizacion> bitacora = new ArrayList<>();
        if (codigoFuente == null || codigoFuente.isEmpty()) {
            return new ResultadoOptimizacion("", bitacora);
        }

        String[] lineasCrudas = codigoFuente.split("\\r?\\n", -1);
        List<EstadoLinea> estados = new ArrayList<>();
        for (int i = 0; i < lineasCrudas.length; i++) {
            estados.add(new EstadoLinea(i + 1, lineasCrudas[i]));
        }

        // 1. MINIFICACIÓN: Eliminación de comentarios y líneas vacías
        for (EstadoLinea est : estados) {
            String trimOrig = est.actual.trim();
            if (trimOrig.isEmpty()) {
                est.eliminada = true;
                continue;
            }
            if (trimOrig.startsWith("//")) {
                est.actual = "";
                est.eliminada = true;
                est.tecnicas.add("Minificación (Comentario eliminado)");
            } else if (est.actual.contains("//")) {
                String sinCom = est.actual.replaceAll("//.*", "").replaceAll("[ \\t]+$", "");
                if (!sinCom.equals(est.actual)) {
                    est.actual = sinCom;
                    est.tecnicas.add("Minificación (Comentario inline)");
                }
            }
        }

        // 2. CICLO DE PROPAGACIÓN Y PLEGADO DE CONSTANTES (Constant Propagation & Folding)
        boolean huboCambios = true;
        int iteraciones = 0;

        while (huboCambios && iteraciones < 40) {
            huboCambios = false;
            iteraciones++;

            // 2.1 Simplificar paréntesis con un solo literal numérico: (15) -> 15
            Pattern pPar = Pattern.compile("\\(\\s*(-?\\d+(?:\\.\\d+)?)\\s*\\)");
            for (EstadoLinea est : estados) {
                if (est.eliminada) continue;
                Matcher mPar = pPar.matcher(est.actual);
                if (mPar.find()) {
                    String nuevo = mPar.replaceAll("$1");
                    if (!nuevo.equals(est.actual)) {
                        est.actual = nuevo;
                        est.tecnicas.add("Evaluación de constantes");
                        huboCambios = true;
                    }
                }
            }
            if (huboCambios) continue;

            // 2.2 Detectar constantes seguras (asignadas 1 sola vez con literal fijo)
            String codigoActivo = unirLineasActivas(estados);
            Map<String, String> constantes = new LinkedHashMap<>();
            Pattern pConst = Pattern.compile(
                    "^[ \\t]*(?:var|const)?[ \\t]*([a-zA-Z_]\\w*)[ \\t]*(?:int|float64|string|bool)?[ \\t]*(?::=|=)[ \\t]*(-?\\d+(?:\\.\\d+)?|\"[^\"]*\"|true|false)[ \\t]*$"
            );

            for (EstadoLinea est : estados) {
                if (est.eliminada) continue;
                Matcher mConst = pConst.matcher(est.actual);
                if (mConst.matches()) {
                    String nombre = mConst.group(1);
                    String valor = mConst.group(2);
                    int asignaciones = contarCoincidencias(codigoActivo, "\\b" + Pattern.quote(nombre) + "[ \\t]*(?::=|=|\\+\\+|--)");
                    if (asignaciones == 1 && !constantes.containsKey(nombre)) {
                        constantes.put(nombre, valor);
                    }
                }
            }

            // 2.3 Propagar constantes detectadas en el resto de las líneas
            boolean propago = false;
            for (Map.Entry<String, String> entry : constantes.entrySet()) {
                String nombre = entry.getKey();
                String valor = entry.getValue();

                for (EstadoLinea est : estados) {
                    if (est.eliminada) continue;
                    boolean esSuDeclaracion = est.actual.matches("^[ \\t]*(?:var|const)?[ \\t]*" + Pattern.quote(nombre) + "\\b.*");
                    if (!esSuDeclaracion) {
                        String nuevo = est.actual.replaceAll("\\b" + Pattern.quote(nombre) + "\\b", Matcher.quoteReplacement(valor));
                        if (!nuevo.equals(est.actual)) {
                            est.actual = nuevo;
                            est.tecnicas.add("Propagación de constantes (" + nombre + "=" + valor + ")");
                            propago = true;
                        }
                    }
                }
            }
            if (propago) {
                huboCambios = true;
                continue;
            }

            // 2.4 Plegado de constantes (Constant Folding) línea por línea respetando jerarquía
            for (EstadoLinea est : estados) {
                if (est.eliminada) continue;
                String plegada = plegarExpresionesEnLinea(est.actual);
                if (!plegada.equals(est.actual)) {
                    est.actual = plegada;
                    est.tecnicas.add("Evaluación de constantes (Constant Folding)");
                    huboCambios = true;
                }
            }
        }

        // 3. ELIMINACIÓN DE CÓDIGO MUERTO (Dead Code Elimination)
        boolean muertosEliminados = true;
        int iterMuertos = 0;
        Pattern pAsignacion = Pattern.compile("^[ \\t]*(?:var|const)?[ \\t]*([a-zA-Z_]\\w*)[ \\t]*(?:int|float64|string|bool)?[ \\t]*(?::=|=)[ \\t]*([^\\n]+)$");
        Pattern pDeclSola = Pattern.compile("^[ \\t]*var[ \\t]+([a-zA-Z_]\\w*)[ \\t]+(?:int|float64|string|bool)[ \\t]*$");

        while (muertosEliminados && iterMuertos < 30) {
            muertosEliminados = false;
            iterMuertos++;
            String codigoActivo = unirLineasActivas(estados);

            for (EstadoLinea est : estados) {
                if (est.eliminada) continue;

                Matcher mAsig = pAsignacion.matcher(est.actual);
                if (mAsig.matches()) {
                    String nombre = mAsig.group(1);
                    if (!nombre.equals("main") && contarLecturasVariable(codigoActivo, nombre) == 0) {
                        est.eliminada = true;
                        est.tecnicas.add("Eliminación de código muerto");
                        muertosEliminados = true;
                        codigoActivo = unirLineasActivas(estados);
                        continue;
                    }
                }

                Matcher mDecl = pDeclSola.matcher(est.actual);
                if (mDecl.matches()) {
                    String nombre = mDecl.group(1);
                    int apariciones = contarCoincidencias(codigoActivo, "\\b" + Pattern.quote(nombre) + "\\b");
                    if (apariciones <= 1) {
                        est.eliminada = true;
                        est.tecnicas.add("Eliminación de código muerto");
                        muertosEliminados = true;
                        codigoActivo = unirLineasActivas(estados);
                    }
                }
            }
        }

        // 4. CONSTRUCCIÓN DEL CÓDIGO FINAL Y DE LA BITÁCORA DE LÍNEAS SUSTITUIDAS
        StringBuilder sbFinal = new StringBuilder();
        for (EstadoLinea est : estados) {
            String origTrim = est.original.trim();
            String actTrim = est.actual.trim();

            if (!origTrim.isEmpty()) {
                if (est.eliminada) {
                    String detalleElim = actTrim.isEmpty() ? "[Línea eliminada]" : "[Eliminada tras optimizar: " + actTrim + "]";
                    bitacora.add(new CambioOptimizacion(
                            est.numeroLinea,
                            String.join(" + ", est.tecnicas),
                            origTrim,
                            detalleElim
                    ));
                } else if (!origTrim.equals(actTrim)) {
                    bitacora.add(new CambioOptimizacion(
                            est.numeroLinea,
                            String.join(" + ", est.tecnicas),
                            origTrim,
                            actTrim
                    ));
                }
            }

            if (!est.eliminada && !actTrim.isEmpty()) {
                sbFinal.append(est.actual).append("\n");
            }
        }

        return new ResultadoOptimizacion(sbFinal.toString().trim(), bitacora);
    }

    private static String unirLineasActivas(List<EstadoLinea> estados) {
        StringBuilder sb = new StringBuilder();
        for (EstadoLinea est : estados) {
            if (!est.eliminada && !est.actual.trim().isEmpty()) {
                sb.append(est.actual).append("\n");
            }
        }
        return sb.toString();
    }

    private static int contarLecturasVariable(String codigo, String nombre) {
        int total = contarCoincidencias(codigo, "\\b" + Pattern.quote(nombre) + "\\b");
        int declVar = contarCoincidencias(codigo, "\\b(?:var|const)[ \\t]+" + Pattern.quote(nombre) + "\\b");
        int asigIzq = contarCoincidencias(codigo, "(?<!\\b(?:var|const)[ \\t]{1,20})\\b" + Pattern.quote(nombre) + "[ \\t]*(?::=|=)(?!=)");
        return total - declVar - asigIzq;
    }

    private static int contarCoincidencias(String texto, String regex) {
        try {
            Matcher m = Pattern.compile(regex).matcher(texto);
            int count = 0;
            while (m.find()) count++;
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    public static String plegarExpresionesEnLinea(String linea) {
        String paso1 = plegarOperacionSegura(linea, "\\^", true);
        if (!paso1.equals(linea)) return paso1;

        String paso2 = plegarOperacionSegura(linea, "[*/%]", false);
        if (!paso2.equals(linea)) return paso2;

        return plegarSumaRestaSegura(linea);
    }

    private static String plegarOperacionSegura(String codigo, String regexOp, boolean esPotencia) {
        try {
            Pattern p = Pattern.compile("(?<![a-zA-Z0-9_.])(-?\\d+(?:\\.\\d+)?)[ \\t]*(" + regexOp + ")[ \\t]*(-?\\d+(?:\\.\\d+)?)(?![a-zA-Z0-9_.])");
            Matcher m = p.matcher(codigo);
            StringBuffer sb = new StringBuffer();

            while (m.find()) {
                String antes = codigo.substring(0, m.start()).trim();
                String despues = codigo.substring(m.end()).trim();

                boolean bloqueado = false;
                if (!esPotencia) {
                    if (antes.endsWith("/") || antes.endsWith("%") || antes.endsWith("^") || despues.startsWith("^")) {
                        bloqueado = true;
                    }
                }

                if (!bloqueado) {
                    double op1 = Double.parseDouble(m.group(1));
                    String op = m.group(2);
                    double op2 = Double.parseDouble(m.group(3));
                    Double res = calcular(op1, op, op2);
                    if (res != null) {
                        m.appendReplacement(sb, formatearNumero(res));
                        m.appendTail(sb);
                        return sb.toString();
                    }
                }
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
            }
            m.appendTail(sb);
            return sb.toString();
        } catch (Exception e) {
            return codigo;
        }
    }

    private static String plegarSumaRestaSegura(String codigo) {
        try {
            Pattern p = Pattern.compile("(?<![a-zA-Z0-9_.])(-?\\d+(?:\\.\\d+)?)[ \\t]*([+\\-])[ \\t]*(-?\\d+(?:\\.\\d+)?)(?![a-zA-Z0-9_.])");
            Matcher m = p.matcher(codigo);
            StringBuffer sb = new StringBuffer();

            while (m.find()) {
                String antes = codigo.substring(0, m.start()).trim();
                String despues = codigo.substring(m.end()).trim();

                boolean precedidoPorMayor = antes.endsWith("*") || antes.endsWith("/") || antes.endsWith("%") || antes.endsWith("^") || antes.endsWith("-");
                boolean seguidoPorMayor = despues.startsWith("*") || despues.startsWith("/") || despues.startsWith("%") || despues.startsWith("^");

                if (!precedidoPorMayor && !seguidoPorMayor) {
                    double op1 = Double.parseDouble(m.group(1));
                    String op = m.group(2);
                    double op2 = Double.parseDouble(m.group(3));
                    Double res = calcular(op1, op, op2);
                    if (res != null) {
                        m.appendReplacement(sb, formatearNumero(res));
                        m.appendTail(sb);
                        return sb.toString();
                    }
                }
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
            }
            m.appendTail(sb);
            return sb.toString();
        } catch (Exception e) {
            return codigo;
        }
    }

    private static Double calcular(double op1, String operador, double op2) {
        switch (operador) {
            case "^": return Math.pow(op1, op2);
            case "*": return op1 * op2;
            case "/": return (op2 != 0) ? (op1 / op2) : null;
            case "%": return (op2 != 0) ? (op1 % op2) : null;
            case "+": return op1 + op2;
            case "-": return op1 - op2;
            default: return null;
        }
    }

    public static String formatearNumero(double res) {
        return (res == Math.floor(res)) ? String.valueOf((long) res) : String.valueOf(res);
    }
}