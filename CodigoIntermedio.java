import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodigoIntermedio {

    public static class Cuadruplo {
        public int indice;
        public String operador;
        public String argumento1;
        public String argumento2;
        public String resultado;

        public Cuadruplo(int indice, String operador, String argumento1, String argumento2, String resultado) {
            this.indice = indice;
            this.operador = operador;
            this.argumento1 = argumento1;
            this.argumento2 = argumento2;
            this.resultado = resultado;
        }
    }

    public static class ExpresionExtraida {
        public int numeroLinea;
        public String instruccionOriginal;
        public String instruccionOptimizada;
        public String variableDestino;
        public String operadorAsignacion;
        public List<String> tokensExpresion;
        public List<String> tokensOptimizados;
        public List<String> tokensPosfijos;
        public String notacionPosfija;
        public String notacionPrefija;
        public String notacionPosfijaOpt;
        public String notacionPrefijaOpt;
        public List<Cuadruplo> cuadruplos;
        public List<Cuadruplo> cuadruplosOptimizados;
        public double resultadoExpresion;

        public ExpresionExtraida(int numeroLinea, String instruccionOriginal, String variableDestino,
                                 String operadorAsignacion, List<String> tokensExpresion,
                                 Map<String, Double> tablaValores, Map<String, String> constantesFijas) {
            this.numeroLinea = numeroLinea;
            this.instruccionOriginal = instruccionOriginal;
            this.variableDestino = variableDestino;
            this.operadorAsignacion = operadorAsignacion;
            this.tokensExpresion = tokensExpresion;

            // 1. Conversiones y cuádruplos de la expresión original
            this.tokensPosfijos = convertirAPosfijo(tokensExpresion);
            this.notacionPosfija = String.join(" ", this.tokensPosfijos);
            this.notacionPrefija = convertirAPrefijo(this.tokensPosfijos);
            this.cuadruplos = generarCuadruplos(this.tokensPosfijos);

            // 2. Construcción de la expresión optimizada (Propagación + Constant Folding)
            this.tokensOptimizados = optimizarTokensExpresion(tokensExpresion, constantesFijas);
            String ladoDerechoOpt = String.join(" ", this.tokensOptimizados);
            if ("return".equals(variableDestino)) {
                this.instruccionOptimizada = "return " + ladoDerechoOpt;
            } else {
                this.instruccionOptimizada = variableDestino + " " + operadorAsignacion + " " + ladoDerechoOpt;
            }

            List<String> posfijosOpt = convertirAPosfijo(this.tokensOptimizados);
            this.notacionPosfijaOpt = String.join(" ", posfijosOpt);
            this.notacionPrefijaOpt = convertirAPrefijo(posfijosOpt);
            this.cuadruplosOptimizados = generarCuadruplos(posfijosOpt);

            // 3. Cálculo del resultado numérico de la expresión
            this.resultadoExpresion = evaluarPosfijo(this.tokensPosfijos, tablaValores);
        }
    }

    // =========================================================
    // PASO 1: EXTRACCIÓN E IDENTIFICACIÓN DE EXPRESIONES
    // =========================================================
    public static List<ExpresionExtraida> extraerExpresiones(List<AnalisisLinea> lineasAnalizadas) {
        List<ExpresionExtraida> expresionesValidas = new ArrayList<>();
        Map<String, Double> tablaValores = new HashMap<>();
        Map<String, Integer> conteoAsignaciones = contarAsignacionesTotales(lineasAnalizadas);
        Map<String, String> constantesFijas = new HashMap<>();

        for (AnalisisLinea linea : lineasAnalizadas) {
            if (linea.tokens.isEmpty() || linea.error != null) {
                continue;
            }
            if (linea.tipos.get(0).equals("Comentario") || linea.tokens.get(0).equals("fmt")) {
                continue;
            }

            int indiceInicioExpresion = -1;
            String destino = "";
            String opAsig = "=";

            for (int i = 0; i < linea.tokens.size(); i++) {
                String token = linea.tokens.get(i);
                if (token.equals("=") || token.equals(":=")) {
                    if (i > 0) {
                        destino = linea.tokens.get(i - 1);
                    }
                    opAsig = token;
                    indiceInicioExpresion = i + 1;
                    break;
                }
            }

            if (indiceInicioExpresion == -1 && linea.tokens.get(0).equals("return") && linea.tokens.size() > 1) {
                destino = "return";
                opAsig = "";
                indiceInicioExpresion = 1;
            }

            if (indiceInicioExpresion != -1 && indiceInicioExpresion < linea.tokens.size()) {
                List<String> subTokens = new ArrayList<>(linea.tokens.subList(indiceInicioExpresion, linea.tokens.size()));
                List<String> subTipos = new ArrayList<>(linea.tipos.subList(indiceInicioExpresion, linea.tokens.size()));

                if (subTokens.size() == 1 && !destino.isEmpty() && !destino.equals("return")) {
                    String unicoToken = subTokens.get(0);
                    String unicoTipo = subTipos.get(0);
                    if (unicoTipo.equals("Entero") || unicoTipo.equals("Decimal") || unicoTipo.equals("Identificador")) {
                        double val = obtenerValorToken(unicoToken, tablaValores);
                        tablaValores.put(destino, val);
                        if (conteoAsignaciones.getOrDefault(destino, 0) == 1) {
                            constantesFijas.put(destino, Optimizador.formatearNumero(val));
                        }
                    }
                }

                if (esExpresionAritmeticaValida(subTokens, subTipos)) {
                    ExpresionExtraida nuevaExpresion = new ExpresionExtraida(
                            linea.numeroLinea,
                            linea.contenido,
                            destino,
                            opAsig,
                            subTokens,
                            tablaValores,
                            constantesFijas
                    );
                    expresionesValidas.add(nuevaExpresion);

                    if (!destino.isEmpty() && !destino.equals("return")) {
                        tablaValores.put(destino, nuevaExpresion.resultadoExpresion);
                        if (conteoAsignaciones.getOrDefault(destino, 0) == 1 && nuevaExpresion.tokensOptimizados.size() == 1) {
                            constantesFijas.put(destino, nuevaExpresion.tokensOptimizados.get(0));
                        }
                    }
                }
            }
        }

        return expresionesValidas;
    }

    private static Map<String, Integer> contarAsignacionesTotales(List<AnalisisLinea> lineas) {
        Map<String, Integer> conteo = new HashMap<>();
        for (AnalisisLinea l : lineas) {
            for (int i = 0; i < l.tokens.size(); i++) {
                String t = l.tokens.get(i);
                if ((t.equals("=") || t.equals(":=")) && i > 0) {
                    String var = l.tokens.get(i - 1);
                    conteo.put(var, conteo.getOrDefault(var, 0) + 1);
                } else if ((t.equals("++") || t.equals("--")) && i > 0) {
                    String var = l.tokens.get(i - 1);
                    conteo.put(var, conteo.getOrDefault(var, 0) + 1);
                }
            }
        }
        return conteo;
    }

    // Optimiza los tokens de una expresión aplicando Propagación y Plegado de Constantes
    private static List<String> optimizarTokensExpresion(List<String> tokensOriginales, Map<String, String> constantesFijas) {
        List<String> sustituida = new ArrayList<>();
        for (String t : tokensOriginales) {
            sustituida.add(constantesFijas.getOrDefault(t, t));
        }

        String exprTexto = String.join(" ", sustituida);
        boolean cambio = true;
        int iter = 0;
        Pattern pPar = Pattern.compile("\\(\\s*(-?\\d+(?:\\.\\d+)?)\\s*\\)");

        while (cambio && iter < 30) {
            cambio = false;
            iter++;

            Matcher mPar = pPar.matcher(exprTexto);
            if (mPar.find()) {
                exprTexto = mPar.replaceAll("$1");
                cambio = true;
                continue;
            }

            String plegada = Optimizador.plegarExpresionesEnLinea(exprTexto);
            if (!plegada.equals(exprTexto)) {
                exprTexto = plegada;
                cambio = true;
            }
        }

        // Tokenizar nuevamente la expresión optimizada
        List<String> resultado = new ArrayList<>();
        Matcher m = Pattern.compile("-?\\d+(?:\\.\\d+)?|[a-zA-Z_]\\w*|[()+\\-*/%^]").matcher(exprTexto);
        while (m.find()) {
            resultado.add(m.group());
        }
        return resultado.isEmpty() ? sustituida : resultado;
    }

    private static boolean esExpresionAritmeticaValida(List<String> tokens, List<String> tipos) {
        boolean tieneOperador = false;
        int balanceParentesis = 0;

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            String tipo = tipos.get(i);

            if (token.equals("(")) {
                balanceParentesis++;
            } else if (token.equals(")")) {
                balanceParentesis--;
                if (balanceParentesis < 0) return false;
            } else if (esOperadorAritmetico(token)) {
                tieneOperador = true;
            } else if (tipo.equals("Identificador")) {
                if (i + 1 < tokens.size() && tokens.get(i + 1).equals("(")) {
                    return false;
                }
            } else if (!tipo.equals("Entero") && !tipo.equals("Decimal")) {
                return false;
            }
        }

        return tieneOperador && (balanceParentesis == 0);
    }

    public static boolean esOperadorAritmetico(String token) {
        return token.equals("+") || token.equals("-") || token.equals("*") || 
               token.equals("/") || token.equals("%") || token.equals("^");
    }

    // =========================================================
    // PASO 2: CONVERSIÓN A NOTACIÓN POSFIJA Y PREFIJA
    // =========================================================
    public static int precedencia(String operador) {
        switch (operador) {
            case "^": return 3;
            case "*":
            case "/":
            case "%": return 2;
            case "+":
            case "-": return 1;
            default: return 0;
        }
    }

    public static List<String> convertirAPosfijo(List<String> tokensInfijos) {
        List<String> salidaPosfija = new ArrayList<>();
        Stack<String> pilaOperadores = new Stack<>();

        for (String token : tokensInfijos) {
            if (token.equals("(")) {
                pilaOperadores.push(token);
            } else if (token.equals(")")) {
                while (!pilaOperadores.isEmpty() && !pilaOperadores.peek().equals("(")) {
                    salidaPosfija.add(pilaOperadores.pop());
                }
                if (!pilaOperadores.isEmpty() && pilaOperadores.peek().equals("(")) {
                    pilaOperadores.pop();
                }
            } else if (esOperadorAritmetico(token)) {
                while (!pilaOperadores.isEmpty() && !pilaOperadores.peek().equals("(") &&
                       precedencia(pilaOperadores.peek()) >= precedencia(token)) {
                    salidaPosfija.add(pilaOperadores.pop());
                }
                pilaOperadores.push(token);
            } else {
                salidaPosfija.add(token);
            }
        }

        while (!pilaOperadores.isEmpty()) {
            salidaPosfija.add(pilaOperadores.pop());
        }

        return salidaPosfija;
    }

    public static String convertirAPrefijo(List<String> tokensPosfijos) {
        Stack<String> pila = new Stack<>();

        for (String token : tokensPosfijos) {
            if (esOperadorAritmetico(token)) {
                if (pila.size() >= 2) {
                    String operando2 = pila.pop();
                    String operando1 = pila.pop();
                    String subExpresionPrefija = token + " " + operando1 + " " + operando2;
                    pila.push(subExpresionPrefija);
                }
            } else {
                pila.push(token);
            }
        }

        return pila.isEmpty() ? "" : pila.pop();
    }

    // =========================================================
    // PASO 3: GENERACIÓN DE CUÁDRUPLOS, EVALUACIÓN Y REPORTE
    // =========================================================
    public static List<Cuadruplo> generarCuadruplos(List<String> tokensPosfijos) {
        List<Cuadruplo> listaCuadruplos = new ArrayList<>();
        Stack<String> pilaOperandos = new Stack<>();
        int contadorTemporal = 1;
        int numeroOperacion = 1;

        for (String token : tokensPosfijos) {
            if (esOperadorAritmetico(token)) {
                if (pilaOperandos.size() >= 2) {
                    String argumento2 = pilaOperandos.pop();
                    String argumento1 = pilaOperandos.pop();
                    String temporal = "T" + contadorTemporal++;

                    Cuadruplo nuevo = new Cuadruplo(numeroOperacion++, token, argumento1, argumento2, temporal);
                    listaCuadruplos.add(nuevo);

                    pilaOperandos.push(temporal);
                }
            } else {
                pilaOperandos.push(token);
            }
        }

        return listaCuadruplos;
    }

    private static double obtenerValorToken(String token, Map<String, Double> tablaValores) {
        if (tablaValores.containsKey(token)) {
            return tablaValores.get(token);
        }
        try {
            if (token.startsWith("0x") || token.startsWith("0X")) {
                return Long.decode(token).doubleValue();
            }
            return Double.parseDouble(token);
        } catch (NumberFormatException e) {
            return 1.0;
        }
    }

    public static double evaluarPosfijo(List<String> tokensPosfijos, Map<String, Double> tablaValores) {
        Stack<Double> pila = new Stack<>();

        for (String token : tokensPosfijos) {
            if (esOperadorAritmetico(token)) {
                if (pila.size() >= 2) {
                    double op2 = pila.pop();
                    double op1 = pila.pop();
                    double res = 0.0;

                    switch (token) {
                        case "+": res = op1 + op2; break;
                        case "-": res = op1 - op2; break;
                        case "*": res = op1 * op2; break;
                        case "/": res = (op2 != 0) ? (op1 / op2) : 0.0; break;
                        case "%": res = (op2 != 0) ? (op1 % op2) : 0.0; break;
                        case "^": res = Math.pow(op1, op2); break;
                    }
                    pila.push(res);
                }
            } else {
                pila.push(obtenerValorToken(token, tablaValores));
            }
        }

        return pila.isEmpty() ? 0.0 : pila.pop();
    }

    public static String generarReporte(List<AnalisisLinea> lineasAnalizadas) {
        List<ExpresionExtraida> expresiones = extraerExpresiones(lineasAnalizadas);
        StringBuilder sb = new StringBuilder();

        if (expresiones.isEmpty()) {
            sb.append("No se encontraron expresiones aritméticas válidas para generar código intermedio.\n");
            return sb.toString();
        }

        int contadorExp = 1;
        for (ExpresionExtraida exp : expresiones) {
            sb.append("====================================================================\n");
            sb.append("Línea ").append(exp.numeroLinea).append(". Expresión #").append(contadorExp++).append(":\n");
            sb.append("Expresión original   : ").append(exp.instruccionOriginal).append("\n");
            sb.append("Expresión optimizada : ").append(exp.instruccionOptimizada).append("\n\n");

            sb.append("1. Notación prefija  (Original)   : ").append(exp.notacionPrefija).append("\n");
            sb.append("   Notación prefija  (Optimizada) : ").append(exp.notacionPrefijaOpt).append("\n");
            sb.append("2. Notación posfija  (Original)   : ").append(exp.notacionPosfija).append("\n");
            sb.append("   Notación posfija  (Optimizada) : ").append(exp.notacionPosfijaOpt).append("\n\n");

            sb.append("3. Tabla de cuádruplos (Expresión Original):\n");
            sb.append("--------------------------------------------------------------------\n");
            sb.append(String.format("%-6s | %-10s | %-15s | %-15s | %-10s\n", 
                    "#", "Operador", "Argumento 1", "Argumento 2", "Resultado"));
            sb.append("--------------------------------------------------------------------\n");
            for (Cuadruplo c : exp.cuadruplos) {
                sb.append(String.format("%-6d | %-10s | %-15s | %-15s | %-10s\n",
                        c.indice, c.operador, c.argumento1, c.argumento2, c.resultado));
            }
            sb.append("--------------------------------------------------------------------\n\n");

            sb.append("4. Tabla de cuádruplos (Expresión Optimizada):\n");
            sb.append("--------------------------------------------------------------------\n");
            sb.append(String.format("%-6s | %-10s | %-15s | %-15s | %-10s\n", 
                    "#", "Operador", "Argumento 1", "Argumento 2", "Resultado"));
            sb.append("--------------------------------------------------------------------\n");
            if (exp.cuadruplosOptimizados.isEmpty()) {
                String valorDirecto = exp.tokensOptimizados.isEmpty() ? String.valueOf(exp.resultadoExpresion) : exp.tokensOptimizados.get(0);
                sb.append(String.format("%-6d | %-10s | %-15s | %-15s | %-10s\n",
                        1, exp.operadorAsignacion.isEmpty() ? "return" : exp.operadorAsignacion,
                        valorDirecto, "-", exp.variableDestino));
            } else {
                for (Cuadruplo c : exp.cuadruplosOptimizados) {
                    sb.append(String.format("%-6d | %-10s | %-15s | %-15s | %-10s\n",
                            c.indice, c.operador, c.argumento1, c.argumento2, c.resultado));
                }
            }
            sb.append("--------------------------------------------------------------------\n");
            sb.append("Resultado de la expresión: ").append(exp.resultadoExpresion).append("\n\n");
        }

        return sb.toString();
    }
}