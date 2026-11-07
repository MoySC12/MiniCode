import java.util.ArrayList;
import java.util.List;

public class AnalisisLinea {
    public int numeroLinea;
    public String contenido;
    public List<String> tokens;
    public List<String> tipos;
    public String error;           // Error general para compatibilidad
    public String errorSintactico; // Diagnóstico exclusivo del analizador sintáctico
    public String errorSemantico;  // Diagnóstico exclusivo del analizador semántico

    public AnalisisLinea(int numeroLinea, String contenido) {
        this.numeroLinea = numeroLinea;
        this.contenido = contenido;
        this.tokens = new ArrayList<>();
        this.tipos = new ArrayList<>();
        this.error = null;
        this.errorSintactico = null;
        this.errorSemantico = null;
    }
}