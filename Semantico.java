import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.ArrayList;

public class Semantico {
    public static Stack<HashMap<String, String>> pilaAmbitos = new Stack<>();
    public static HashMap<String, Integer> aridadFunciones = new HashMap<>();
    public static HashMap<String, String> tiposRetornoFunciones = new HashMap<>();
    public static String funcionActual = "";
    
    public static Stack<ArrayList<String>> pilaConstantes = new Stack<>();
    public static Stack<HashMap<String, Boolean>> pilaUsoVariables = new Stack<>();

    public static String mapearTipoGo(String tipoGo) {
        switch (tipoGo) {
            case "int": return "Entero";
            case "float64": return "Decimal";
            case "string": return "Cadena";
            case "bool": return "Booleano";
            default: return tipoGo;
        }
    }

    public static String validarSemantica(AnalisisLinea analisis) {
        List<String> tokens = analisis.tokens;
        List<String> tipos = analisis.tipos;

        if (tokens.isEmpty() || tipos.get(0).equals("Comentario")) return null;

        String primerToken = tokens.get(0);
        int parentesisActuales = 0;
        List<String[]> parametrosPendientes = new ArrayList<>(); 

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            String tipo = tipos.get(i);

            if (token.equals("(")) parentesisActuales++;
            if (token.equals(")")) parentesisActuales--;

            // 1. CONTROL DE ÁMBITO
            if (token.equals("func") && i + 1 < tokens.size() && tipos.get(i + 1).equals("Identificador")) {
                funcionActual = tokens.get(i + 1); 
            }

            if (token.equals("{")) {
                pilaAmbitos.push(new HashMap<>()); 
                pilaConstantes.push(new ArrayList<>()); 
                pilaUsoVariables.push(new HashMap<>()); 
                
                for (String[] param : parametrosPendientes) {
                    pilaAmbitos.peek().put(param[0], param[1]);
                }
                parametrosPendientes.clear();
                
            } else if (token.equals("}")) {
                if (pilaAmbitos.size() > 1) {
                    for (String varLocal : pilaUsoVariables.peek().keySet()) {
                        if (!pilaUsoVariables.peek().get(varLocal)) {
                            return "Error Semántico: La variable '" + varLocal + "' fue declarada pero nunca se utilizó.";
                        }
                    }
                    pilaAmbitos.pop(); 
                    pilaConstantes.pop(); 
                    pilaUsoVariables.pop(); 
                }
                if (pilaAmbitos.size() == 1) {
                    funcionActual = "";
                }
            }

            // 2. REGISTRO EN LA MEMORIA
            if (token.equals("var") && i + 1 < tokens.size() && tipos.get(i + 1).equals("Identificador")) {
                String nombreVar = tokens.get(i + 1);
                
                if (existeEnAmbitoActual(nombreVar)) {
                    return "Error Semántico: El identificador '" + nombreVar + "' ya fue declarado en este ámbito.";
                }
                
                String tipoAsignado = "Indefinido";
                if (i + 2 < tokens.size()) {
                    if (tokens.get(i + 2).equals("=")) {
                        tipoAsignado = obtenerTipoReal(i + 3, tokens, tipos, true); 
                    } else {
                        tipoAsignado = mapearTipoGo(tokens.get(i + 2)); 
                    }
                }
                pilaAmbitos.peek().put(nombreVar, tipoAsignado); 
                pilaUsoVariables.peek().put(nombreVar, false);
            }

            if (token.equals("const") && i + 1 < tokens.size() && tipos.get(i + 1).equals("Identificador")) {
                String nombreVar = tokens.get(i + 1);
                
                if (existeEnAmbitoActual(nombreVar)) {
                    return "Error Semántico: El identificador '" + nombreVar + "' ya fue declarado en este ámbito.";
                }
                
                String tipoAsignado = "Indefinido";
                if (i + 2 < tokens.size() && tokens.get(i + 2).equals("=")) {
                    tipoAsignado = obtenerTipoReal(i + 3, tokens, tipos, true); 
                }
                
                pilaAmbitos.peek().put(nombreVar, tipoAsignado); 
                pilaConstantes.peek().add(nombreVar); 
                pilaUsoVariables.peek().put(nombreVar, false);
            }

            if (token.equals(":=") && i > 0 && tipos.get(i - 1).equals("Identificador")) {
                String nombreVar = tokens.get(i - 1);
                if (existeEnAmbitoActual(nombreVar)) {
                    return "Error Semántico: La variable '" + nombreVar + "' ya existe en este ámbito. Usa '='.";
                }
                String tipoDerecha = obtenerTipoReal(i + 1, tokens, tipos, true);
                pilaAmbitos.peek().put(nombreVar, tipoDerecha);
                pilaUsoVariables.peek().put(nombreVar, false);
            }

            if (primerToken.equals("func") && parentesisActuales > 0 && tipo.equals("Identificador")) {
                String paramTipo = "Indefinido";
                if (i + 1 < tokens.size() && tipos.get(i + 1).equals("Palabra Reservada")) {
                    paramTipo = mapearTipoGo(tokens.get(i + 1));
                }
                parametrosPendientes.add(new String[]{token, paramTipo}); 
            }

            // 3. COMPROBACIÓN DE TIPOS Y CONSTANTES
            if (token.equals("=") && i > 0 && tipos.get(i - 1).equals("Identificador")) {
                String nombreVar = tokens.get(i - 1);
                
                if (existeEnCualquierAmbito(nombreVar)) {
                    if (esConstante(nombreVar) && !primerToken.equals("const")) {
                        return "Error Semántico: Inmutabilidad violada. No se puede reasignar un valor a la constante '" + nombreVar + "'.";
                    }

                    String tipoGuardado = buscarTipoVariable(nombreVar);
                    String tipoDerecha = obtenerTipoReal(i + 1, tokens, tipos, true);
                    
                    if (tipoGuardado.equals("Indefinido") || primerToken.equals("var") || primerToken.equals("const")) {
                        actualizarTipoVariable(nombreVar, tipoDerecha);
                    } else if (!tipoGuardado.equals(tipoDerecha) && !tipoDerecha.equals("Desconocido")) {
                        return "Error Semántico: Incompatibilidad de tipos. No puedes asignar un valor '" + tipoDerecha + "' a la variable '" + nombreVar + "' de tipo '" + tipoGuardado + "'.";
                    }
                }
            }
            
            // 4. USO DE VARIABLES Y VALIDACIÓN DE ARGUMENTOS
            if (tipo.equals("Identificador")) {
                boolean esDeclaracionVar = (i > 0 && tokens.get(i - 1).equals("var"));
                boolean esDeclaracionConst = (i > 0 && tokens.get(i - 1).equals("const")); 
                boolean esAsignacionCorta = (i + 1 < tokens.size() && tokens.get(i + 1).equals(":="));
                boolean esNombreDeFuncion = (i > 0 && tokens.get(i - 1).equals("func"));
                boolean esPaqueteFmt = token.equals("fmt") || token.equals("Println") || token.equals("Scan");
                boolean esPaquete = (i > 0 && tokens.get(i - 1).equals("package"));
                boolean esParametro = (primerToken.equals("func") && parentesisActuales > 0);
                boolean esTipoRetorno = (primerToken.equals("func") && i > 0 && (tokens.get(i - 1).equals(")") || tipos.get(i - 1).equals("Palabra Reservada")));

                if (!esDeclaracionVar && !esDeclaracionConst && !esAsignacionCorta && !esNombreDeFuncion && !esPaqueteFmt && !esPaquete && !esParametro && !esTipoRetorno) {
                    
                    if (!existeEnCualquierAmbito(token)) {
                        return "Error Semántico: La variable o función '" + token + "' no existe o está fuera de ámbito.";
                    }
                    
                    marcarVariableComoUsada(token);

                    if (i + 1 < tokens.size() && tokens.get(i + 1).equals("(")) {
                        if (aridadFunciones.containsKey(token)) {
                            int paramsEsperados = aridadFunciones.get(token);
                            int paramsEnviados = contarArgumentosEnParentesis(i + 1, tokens);
                            
                            if (paramsEsperados != paramsEnviados) {
                                return "Error Semántico: La función '" + token + "' espera " + paramsEsperados + " argumento(s), pero se enviaron " + paramsEnviados + ".";
                            }
                        }
                    }
                }
            }

            // 5. LÓGICA MATEMÁTICA Y RELACIONAL
            if (tipo.equals("Operador Aritmético")) {
                if (token.equals("/") || token.equals("%")) {
                    if (i + 1 < tokens.size() && (tokens.get(i + 1).equals("0") || tokens.get(i + 1).equals("0.0"))) {
                        return "Error Semántico: División estática por cero detectada. El denominador no puede ser 0.";
                    }
                }

                String tipoIzq = obtenerTipoReal(i - 1, tokens, tipos, false);
                String tipoDer = obtenerTipoReal(i + 1, tokens, tipos, false);
                
                if (tipoIzq != null && tipoDer != null) {
                    if (tipoIzq.equals("Cadena") || tipoDer.equals("Cadena") || tipoIzq.equals("Booleano") || tipoDer.equals("Booleano")) {
                        return "Error Semántico: Operación aritmética inválida entre '" + tipoIzq + "' y '" + tipoDer + "'. Solo se permiten números.";
                    }
                }
            }
            
            if (tipo.equals("Operador Relacional")) {
                String tipoIzq = obtenerTipoReal(i - 1, tokens, tipos, false);
                String tipoDer = obtenerTipoReal(i + 1, tokens, tipos, false);
                
                if (tipoIzq != null && tipoDer != null && !tipoIzq.equals("Desconocido") && !tipoDer.equals("Desconocido")) {
                    boolean izqEsNum = (tipoIzq.equals("Entero") || tipoIzq.equals("Decimal"));
                    boolean derEsNum = (tipoDer.equals("Entero") || tipoDer.equals("Decimal"));
                    
                    if (izqEsNum && derEsNum) continue; 
                    
                    if (!tipoIzq.equals(tipoDer)) {
                        return "Error Semántico: No se puede comparar '" + tipoIzq + "' con '" + tipoDer + "' mediante el operador '" + token + "'.";
                    }
                }
            }

            // 6. COMPROBACIÓN DE RETORNO
            if (token.equals("return")) {
                String tipoEsperado = tiposRetornoFunciones.getOrDefault(funcionActual, "Vacio");
                String tipoDevuelto = "Vacio";

                if (i + 1 < tokens.size() && !tokens.get(i + 1).equals("}")) {
                    tipoDevuelto = obtenerTipoReal(i + 1, tokens, tipos, true);
                }

                if (tipoEsperado.equals("Vacio") && !tipoDevuelto.equals("Vacio") && !tipoDevuelto.equals("Desconocido")) {
                    return "Error Semántico: La función '" + funcionActual + "' no tiene un tipo de retorno definido, pero se intentó retornar un '" + tipoDevuelto + "'.";
                }

                if (!tipoEsperado.equals("Vacio") && !tipoEsperado.equalsIgnoreCase(tipoDevuelto) && !tipoDevuelto.equals("Desconocido")) {
                    return "Error Semántico: La función '" + funcionActual + "' debe retornar '" + tipoEsperado + "', pero se intentó retornar '" + tipoDevuelto + "'.";
                }
            }
        }
        return null; 
    }

    // =========================================================
    // MÉTODOS AUXILIARES ACTUALIZADOS
    // =========================================================
    
    private static void marcarVariableComoUsada(String nombre) {
        for (int i = pilaUsoVariables.size() - 1; i >= 0; i--) {
            if (pilaUsoVariables.get(i).containsKey(nombre)) {
                pilaUsoVariables.get(i).put(nombre, true);
                return;
            }
        }
    }

    private static String buscarTipoVariable(String nombre) {
        for (int i = pilaAmbitos.size() - 1; i >= 0; i--) {
            if (pilaAmbitos.get(i).containsKey(nombre)) {
                return pilaAmbitos.get(i).get(nombre);
            }
        }
        return null;
    }

    private static boolean existeEnAmbitoActual(String nombre) {
        if(pilaAmbitos.isEmpty()) return false;
        return pilaAmbitos.peek().containsKey(nombre);
    }

    private static boolean existeEnCualquierAmbito(String nombre) {
        return buscarTipoVariable(nombre) != null;
    }
    
    private static boolean esConstante(String nombre) {
        for (int i = pilaConstantes.size() - 1; i >= 0; i--) {
            if (pilaConstantes.get(i).contains(nombre)) {
                return true;
            }
        }
        return false;
    }

    private static void actualizarTipoVariable(String nombre, String nuevoTipo) {
        for (int i = pilaAmbitos.size() - 1; i >= 0; i--) {
            if (pilaAmbitos.get(i).containsKey(nombre)) {
                pilaAmbitos.get(i).put(nombre, nuevoTipo);
                return;
            }
        }
    }

    // EL MOTOR DE EVALUACIÓN DE TIPOS CORREGIDO
    private static String obtenerTipoReal(int indice, List<String> tokens, List<String> tipos, boolean evaluarExpresionCompleta) {
        if (indice < 0 || indice >= tokens.size()) return "Desconocido";
        
        // 1. Detección profunda de operadores que transforman la expresión en booleana
        if (evaluarExpresionCompleta) {
            for (int j = indice; j < tokens.size(); j++) {
                String tipo = tipos.get(j);
                if (tipo.equals("Operador Relacional") || tipo.equals("Operador Lógico")) {
                    return "Booleano";
                }
            }
        }
        
        // 2. Omitir paréntesis para extraer la variable o función real
        int idx = indice;
        while (idx < tokens.size() && tokens.get(idx).equals("(")) {
            idx++;
        }
        if (idx >= tokens.size()) return "Desconocido";
        
        String token = tokens.get(idx);
        String tipo = tipos.get(idx);
        
        // 3. Resolución de Identificadores y extracción de tipo de retorno en llamadas a función
        if (tipo.equals("Identificador")) {
            String tipoGuardado = buscarTipoVariable(token);
            if ("Funcion".equals(tipoGuardado)) {
                return tiposRetornoFunciones.getOrDefault(token, "Vacio");
            }
            if (tipoGuardado != null) {
                return tipoGuardado;
            }
            return "Desconocido";
        }
        
        if (tipo.equals("Palabra Reservada") && (token.equals("true") || token.equals("false"))) {
            return "Booleano";
        }
        
        return tipo; 
    }

    private static int contarArgumentosEnParentesis(int indiceApertura, List<String> tokens) {
        int argumentos = 0;
        int nivelParentesis = 0;
        boolean contieneDatos = false;
        
        for (int j = indiceApertura; j < tokens.size(); j++) {
            String t = tokens.get(j);
            
            if (t.equals("(")) {
                nivelParentesis++;
            } else if (t.equals(")")) {
                nivelParentesis--;
                if (nivelParentesis == 0) {
                    if (contieneDatos) argumentos++; 
                    break;
                }
            } else if (nivelParentesis == 1) { 
                if (t.equals(",")) {
                    argumentos++;
                    contieneDatos = false;
                } else {
                    contieneDatos = true;
                }
            }
        }
        return argumentos;
    }

    public static void preCargarFunciones(List<AnalisisLinea> lineas) {
        if (pilaAmbitos.isEmpty()) {
            pilaAmbitos.push(new HashMap<>()); 
            pilaConstantes.push(new ArrayList<>());
            pilaUsoVariables.push(new HashMap<>());
        }
        for (AnalisisLinea linea : lineas) {
            if (linea.tokens.size() >= 2 && linea.tokens.get(0).equals("func") && linea.tipos.get(1).equals("Identificador")) {
                String nombreFunc = linea.tokens.get(1);
                pilaAmbitos.get(0).put(nombreFunc, "Funcion");
                
                int numParametros = contarArgumentosEnParentesis(2, linea.tokens); 
                aridadFunciones.put(nombreFunc, numParametros);

                String tipoRetorno = "Vacio";
                int indiceCierreParentesis = -1;
                for (int k = 2; k < linea.tokens.size(); k++) {
                    if (linea.tokens.get(k).equals(")")) {
                        indiceCierreParentesis = k;
                        break;
                    }
                }

                if (indiceCierreParentesis != -1 && indiceCierreParentesis + 1 < linea.tokens.size()) {
                    String tokenSiguiente = linea.tokens.get(indiceCierreParentesis + 1);
                    if (!tokenSiguiente.equals("{")) {
                        tipoRetorno = mapearTipoGo(tokenSiguiente);
                    }
                }
                tiposRetornoFunciones.put(nombreFunc, tipoRetorno);
            }
        }
    }
    
    public static void reset() {
        pilaAmbitos.clear();
        pilaAmbitos.push(new HashMap<>()); 
        pilaConstantes.clear();
        pilaConstantes.push(new ArrayList<>());
        pilaUsoVariables.clear();
        pilaUsoVariables.push(new HashMap<>());
        aridadFunciones.clear();
        tiposRetornoFunciones.clear();
        funcionActual = ""; 
    }
}