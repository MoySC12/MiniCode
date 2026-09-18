public class Lexer {
    
    public static final String OPERADORES_ASIGNACION = "=|:=";
    
    public static final String IDENTIFICADOR = "[a-zA-Z_][a-zA-Z0-9_]*";
    
    // PASO 6: Ampliación para aceptar Hexadecimales (ej. 0xFF) o números normales (ej. 25)
    public static final String ENTERO = "0[xX][0-9a-fA-F]+|\\d+"; 
    public static final String DECIMAL = "\\d+\\.\\d+";
    public static final String CADENA = "\"[^\"]*\"|'[^']*'|`[^`]*`";
    
    // PASO 5: Expansión del diccionario de Palabras Reservadas
    public static final String PAL_RESERVADAS = "^(if|else|for|func|var|package|import|return|true|false|switch|case|default|break|continue|const|type|struct)$";    
    
    public static final String OPERADORES_ARIT = "[+\\-*/%]";
    public static final String OPERADORES_REL = "==|!=|<=|>=|<|>";
    public static final String OPERADORES_LOG = "&&|\\|\\||!"; 
    public static final String OPERADORES_INC_DEC = "^(\\+\\+|--)$";
    public static final String OPERADORES_BITS = "^(\\||\\^|<<|>>)$";
    public static final String COMENTARIO = "^//.*$";

    public static boolean esIdentificador(String token) {
        return token.matches(IDENTIFICADOR) && !esPalabraReservada(token);
    }

    public static boolean esEntero(String token) {
        return token.matches(ENTERO);
    }

    public static boolean esDecimal(String token) {
        return token.matches(DECIMAL);
    }

    public static boolean esNumero(String token) {
        return esEntero(token) || esDecimal(token);
    }

    public static boolean esCadena(String token) {
        return token.matches(CADENA);
    }

    public static boolean esPalabraReservada(String token) {
        return token.matches(PAL_RESERVADAS);
    }

    public static boolean esComentario(String linea) {
        return linea.matches(COMENTARIO);
    }
}