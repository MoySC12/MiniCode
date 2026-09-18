import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiniCode {

    public static List<AnalisisLinea> analizarCodigo(List<String> lineasCodigo) {
        List<AnalisisLinea> resultados = new ArrayList<>();
        Parser.reset();
        Semantico.reset();
        
        // PASADA 1: Análisis Léxico de todo el documento
        for (int i = 0; i < lineasCodigo.size(); i++) {
            String linea = lineasCodigo.get(i).trim();
            AnalisisLinea analisis = new AnalisisLinea(i + 1, linea);
            analizarLexico(linea, analisis);
            resultados.add(analisis);
        }

        // PASADA 1.5: Pre-cargar funciones en la Tabla de Símbolos
        Semantico.preCargarFunciones(resultados);

        // PASADA 2: Análisis Sintáctico y Semántico
        for (AnalisisLinea analisis : resultados) {
            if (!analisis.contenido.isEmpty() && !Lexer.esComentario(analisis.contenido)) {
                
                String errorSintactico = Parser.validarSintaxis(analisis);
                
                if (errorSintactico != null) {
                    analisis.error = errorSintactico;
                } else {
                    String errorSemantico = Semantico.validarSemantica(analisis);
                    if (errorSemantico != null) {
                        analisis.error = errorSemantico;
                    }
                }
            }
        }
        
        if (!Parser.stackEstructuras.isEmpty()) {
            resultados.get(resultados.size() - 1).error = "Error Sintáctico Crítico: Falta cerrar " + Parser.stackEstructuras.size() + " llave(s) '}'.";
        }
        return resultados;
    }
     
    public static void analizarLexico(String linea, AnalisisLinea analisis) {
        if (Lexer.esComentario(linea)) {
            analisis.tokens.add(linea);
            analisis.tipos.add("Comentario");
            return;
        }
        
        String regex = "//.*|\"[^\"]*\"|'[^']*'|`[^`]*`|0[xX][0-9a-fA-F]+(?=[\\s(){}\\[\\],.=&<>!%+\\-*/|^:\"'`]|$)|\\d+\\.\\d+(?=[\\s(){}\\[\\],.=&<>!%+\\-*/|^:\"'`]|$)|\\d+(?=[\\s(){}\\[\\],.=&<>!%+\\-*/|^:\"'`]|$)|[a-zA-Z_][a-zA-Z0-9_]*(?=[\\s(){}\\[\\],.=&<>!%+\\-*/|^:\"'`]|$)|==|!=|<=|>=|:=|&&|\\|\\||\\+\\+|--|<<|>>|[(){}\\[\\],.=&<>!%+\\-*/|^]|[^\\s(){}\\[\\],.=&<>!%+\\-*/|^:\"'`]+";
        
        Pattern patron = Pattern.compile(regex);
        Matcher matcher = patron.matcher(linea);
        
        while (matcher.find()) {
            String token = matcher.group();
            analisis.tokens.add(token);
            analisis.tipos.add(determinarTipoToken(token));
        }
    }

    public static String determinarTipoToken(String token) {
        if (Lexer.esPalabraReservada(token)) return "Palabra Reservada";
        if (Lexer.esEntero(token)) return "Entero"; 
        if (Lexer.esDecimal(token)) return "Decimal";
        if (Lexer.esCadena(token)) return "Cadena";
        if (Lexer.esIdentificador(token)) return "Identificador";
        if (token.matches(Lexer.OPERADORES_INC_DEC)) return "Operador de Incremento/Decremento";
        if (token.matches(Lexer.OPERADORES_BITS)) return "Operador a Nivel de Bits";
        if (token.matches(Lexer.OPERADORES_ARIT)) return "Operador Aritmético";
        if (token.matches(Lexer.OPERADORES_REL)) return "Operador Relacional";
        if (token.equals("=") || token.equals(":=")) return "Operador de Asignación";
        if (token.matches(Lexer.OPERADORES_LOG)) return "Operador Lógico";
        if (token.equals("(") || token.equals(")")) return "Paréntesis";
        if (token.equals("{") || token.equals("}")) return "Llaves";
        if (token.equals("[") || token.equals("]")) return "Corchetes";
        if (token.equals(",")) return "Coma";
        if (token.equals(".")) return "Punto";
        if (token.equals("&")) return "Operador de Puntero (Referencia)";
        if (Lexer.esComentario(token)) return "Comentario";
        
        return "Token no reconocido";
    }
}