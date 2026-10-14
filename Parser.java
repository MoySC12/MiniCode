import java.util.Stack;
import java.util.List;

public class Parser {
    public static Stack<String> stackEstructuras = new Stack<>();
    
    public static String validarSintaxis(AnalisisLinea analisis) {
        List<String> tokens = analisis.tokens;
        List<String> tipos = analisis.tipos;

        if (tokens.isEmpty()) return null; 
        if (tipos.get(0).equals("Comentario")) return null; 

        String primerToken = tokens.get(0);
        String ultimoToken = tokens.get(tokens.size() - 1);

        // --- REGLA 0.1: BALANCE DE PARÉNTESIS ---
        int parentesisAbiertos = 0;
        for (String t : tokens) {
            if (t.equals("(")) parentesisAbiertos++;
            if (t.equals(")")) parentesisAbiertos--;
        }
        if (parentesisAbiertos > 0) return "Error Sintáctico: Falta cerrar uno o más paréntesis ')'.";
        if (parentesisAbiertos < 0) return "Error Sintáctico: Paréntesis de cierre ')' inesperado.";

        // --- REGLA 0.2: BALANCE DE CORCHETES ---
        int corchetesAbiertos = 0;
        for (String t : tokens) {
            if (t.equals("[")) corchetesAbiertos++;
            if (t.equals("]")) corchetesAbiertos--;
        }
        if (corchetesAbiertos > 0) return "Error Sintáctico: Falta cerrar uno o más corchetes ']'.";
        if (corchetesAbiertos < 0) return "Error Sintáctico: Corchete de cierre ']' inesperado.";

        // --- REGLA 1: CONTROL DE BLOQUES (LLAVES) ---
        if (primerToken.equals("}")) {
            if (stackEstructuras.isEmpty()) {
                return "Error Sintáctico: Llave de cierre '}' sin bloque de apertura previo.";
            }
            stackEstructuras.pop(); 
            
            if (ultimoToken.equals("{")) {
                stackEstructuras.push("{");
            } else if (tokens.contains("else")) {
                return "Error Sintáctico: La estructura 'else' o 'else if' debe terminar obligatoriamente con '{'";
            }
            return null; 
        }

        if (ultimoToken.equals("{")) {
            if (primerToken.equals("{") && tokens.size() == 1) {
                return "Error Sintáctico: La llave '{' no debe ir sola en una línea en Go.";
            }
            stackEstructuras.push("{");
        } else {
            if (primerToken.equals("if") || primerToken.equals("for") || primerToken.equals("func") || primerToken.equals("switch")) {
                return "Error Sintáctico: La estructura '" + primerToken + "' debe terminar obligatoriamente con '{'";
            }
        }

        // --- REGLA 2: IMPORTACIONES ---
        if (primerToken.equals("import")) {
            if (tokens.size() < 2 || !tipos.get(1).equals("Cadena")) {
                return "Error Sintáctico: 'import' debe estar seguido de una cadena (ej. import \"fmt\").";
            }
        }

        // --- REGLA 3: DECLARACIÓN DE VARIABLES CON 'var' --- (CORREGIDA PARA GO)
        if (primerToken.equals("var")) {
            if (tokens.size() < 2 || !tipos.get(1).equals("Identificador")) {
                return "Error Sintáctico: 'var' debe estar seguido de un nombre de variable (Identificador).";
            }
            if (tokens.size() == 2) {
                return "Error Sintáctico: La declaración de la variable '" + tokens.get(1) + "' debe especificar un tipo de dato o inicializarse con '='.";
            }
            String tercerToken = tokens.get(2);
            boolean esTipo = tercerToken.equals("int") || tercerToken.equals("float64") || tercerToken.equals("string") || tercerToken.equals("bool");
            
            if (!esTipo && !tercerToken.equals("=")) {
                return "Error Sintáctico: Se esperaba un tipo de dato (ej. int, float64) o una inicialización ('=') después de la variable '" + tokens.get(1) + "'.";
            }
        }

        // --- REGLA 10 (Movida arriba): ESTRUCTURA DE 'return' ---
        if (primerToken.equals("return")) {
            if (tokens.size() > 1) {
                String segundoToken = tokens.get(1);
                if (segundoToken.equals("=") || segundoToken.equals(":=")) {
                    return "Error Sintáctico: 'return' no debe ir seguido de un operador de asignación.";
                }
            }
        }

        // ====================================================================
        // --- SÚPER REGLA 4: ASIGNACIONES CONTEXTUALES := Y = ---
        // ====================================================================
        int parentesisActuales = 0;
        for (int i = 0; i < tokens.size(); i++) {
            String t = tokens.get(i);
            
            if (t.equals("(")) parentesisActuales++;
            if (t.equals(")")) parentesisActuales--;

            if (t.equals(":=") || t.equals("=")) {
                if (i == 0) {
                    return "Error Sintáctico: Asignación inválida. Falta la variable a la izquierda de '" + t + "'.";
                }
                String tipoAnterior = tipos.get(i - 1);
                if (!tipoAnterior.equals("Identificador") && !tokens.get(i - 1).equals("]")) {
                    return "Error Sintáctico: El lado izquierdo de '" + t + "' debe ser una variable o un índice de arreglo.";
                }
                if (i == tokens.size() - 1) {
                    return "Error Sintáctico: Falta el valor a asignar a la derecha de '" + t + "'.";
                }

                if (parentesisActuales > 0) {
                    return "Error Sintáctico: No se permiten asignaciones ('" + t + "') dentro de paréntesis.";
                }

                if (t.equals(":=") && tokens.contains("var")) {
                    return "Error Sintáctico: No se puede usar la asignación corta ':=' junto con 'var'. Usa '='.";
                }

                boolean enCondicion = false;
                for (int j = 0; j < i; j++) {
                    if (tokens.get(j).equals("if") || tokens.get(j).equals("for")) {
                        enCondicion = true;
                        break;
                    }
                }
                if (enCondicion) {
                    return "Error Sintáctico: Uso inválido de asignación '" + t + "' en una condición.";
                }
            }
        }
        
        // --- REGLA 5: FUNCIONES DEL PAQUETE 'fmt' ---
        if (primerToken.equals("fmt")) {
            if (tokens.size() < 4) {
                return "Error Sintáctico: Llamada a paquete 'fmt' incompleta.";
            }
            if (!tokens.get(1).equals(".")) {
                return "Error Sintáctico: Se esperaba '.' después de 'fmt'.";
            }
            if (!tipos.get(2).equals("Identificador")) {
                return "Error Sintáctico: Se esperaba el nombre de la función (ej. Println).";
            }
            if (!tokens.get(3).equals("(")) {
                return "Error Sintáctico: Se esperaba '(' después del nombre de la función.";
            }
        }

        // --- REGLA 6: ESTRUCTURA DE 'func' --- (CORREGIDA PARA GO)
        if (primerToken.equals("func")) {
            if (tokens.size() < 4) {
                return "Error Sintáctico: Declaración de función incompleta.";
            }
            if (!tipos.get(1).equals("Identificador") && !tokens.get(1).equals("main")) {
                return "Error Sintáctico: Se esperaba el nombre de la función después de 'func'.";
            }
            if (!tokens.get(2).equals("(")) {
                return "Error Sintáctico: Se esperaba '(' después del nombre de la función.";
            }
            
            // Buscar el cierre del paréntesis
            int indiceCierre = -1;
            for (int i = 2; i < tokens.size(); i++) {
                if (tokens.get(i).equals(")")) {
                    indiceCierre = i;
                    break;
                }
            }
            
            if (indiceCierre == -1) {
                return "Error Sintáctico: Falta cerrar el paréntesis ')' en la declaración de la función.";
            }
            
            // Validar que dentro de los paréntesis los parámetros tengan tipo
            boolean esperandoIdentificador = true;
            for (int i = 3; i < indiceCierre; i++) {
                String t = tokens.get(i);
                String tipoToken = tipos.get(i);
                
                if (esperandoIdentificador) {
                    if (!tipoToken.equals("Identificador")) {
                        return "Error Sintáctico: Se esperaba un identificador para el parámetro de la función.";
                    }
                    esperandoIdentificador = false;
                } else {
                    boolean esTipo = t.equals("int") || t.equals("float64") || t.equals("string") || t.equals("bool");
                    if (esTipo) {
                         if (i + 1 < indiceCierre && !tokens.get(i + 1).equals(",")) {
                             return "Error Sintáctico: Falta una coma ',' separando los parámetros.";
                         }
                    } else if (t.equals(",")) {
                         esperandoIdentificador = true;
                    } else {
                         return "Error Sintáctico: Se esperaba un tipo de dato válido (int, float64, string, bool) para el parámetro '" + tokens.get(i-1) + "'.";
                    }
                }
            }
            
            // Validar si existe un tipo de retorno explícito antes de abrir la llave {
            if (indiceCierre + 1 < tokens.size()) {
                String tokenSiguiente = tokens.get(indiceCierre + 1);
                if (!tokenSiguiente.equals("{")) {
                    boolean esTipoRetorno = tokenSiguiente.equals("int") || tokenSiguiente.equals("float64") || tokenSiguiente.equals("string") || tokenSiguiente.equals("bool");
                    if (!esTipoRetorno) {
                        return "Error Sintáctico: Tipo de retorno inválido '" + tokenSiguiente + "'. Se esperaba int, float64, string, bool o la llave '{'.";
                    }
                }
            }
        }

        // --- REGLA 7: OPERADORES CONSECUTIVOS INVÁLIDOS ---
        for (int i = 0; i < tipos.size() - 1; i++) {
            String tipoActual = tipos.get(i);
            String tipoSig = tipos.get(i + 1);
            
            if ((tipoActual.equals("Operador Aritmético") || tipoActual.equals("Operador Relacional") || tipoActual.equals("Operador de Asignación")) &&
                (tipoSig.equals("Operador Aritmético") || tipoSig.equals("Operador Relacional") || tipoSig.equals("Operador de Asignación"))) {
                
                if ((tokens.get(i).equals("=") || tokens.get(i).equals(":=") || tokens.get(i).equals("*") || tokens.get(i).equals("/")) && tokens.get(i+1).equals("-")) {
                    continue;
                }
                
                return "Error Sintáctico: Operadores consecutivos inválidos ('" + tokens.get(i) + "' y '" + tokens.get(i+1) + "').";
            }
        }

        // --- REGLA 8 (Simplificada): CONDICIONES VACÍAS EN 'if' Y 'for' ---
        if (primerToken.equals("if") || primerToken.equals("for")) {
            if (tokens.size() < 3) {
                return "Error Sintáctico: La estructura '" + primerToken + "' requiere una condición antes de abrir la llave '{'.";
            }
        }

        // --- REGLA 9: REGLA ESTRICTA DEL 'else' ---
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).equals("else")) {
                if (i == 0) {
                    return "Error Sintáctico: En Go, 'else' no puede iniciar una nueva línea. Debe ir junto a '}' (ej. '} else {').";
                }
                if (!tokens.get(i - 1).equals("}")) {
                    return "Error Sintáctico: 'else' debe ir inmediatamente después de una llave de cierre '}'.";
                }
            }
        }

        // --- REGLA 11: VALIDACIÓN DE OPERANDOS (EVITAR '== .') ---
        for (int i = 0; i < tokens.size(); i++) {
            String tipo = tipos.get(i);
            if (tipo.equals("Operador Relacional") || tipo.equals("Operador Aritmético") || tipo.equals("Operador Lógico")) {
                if (i + 1 < tokens.size()) {
                    String tokenSiguiente = tokens.get(i + 1);
                    if (tokenSiguiente.equals(".") || tokenSiguiente.equals("{") || tokenSiguiente.equals("}") || tokenSiguiente.equals(",") || tokenSiguiente.equals("]")) {
                        return "Error Sintáctico: Se esperaba un valor o identificador válido después del operador '" + tokens.get(i) + "'.";
                    }
                }
            }
        }

        return null; 
    }

    public static void reset() {
        stackEstructuras.clear();
    }
}