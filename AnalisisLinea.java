import java.util.ArrayList;
import java.util.List;

public class AnalisisLinea {
    public int numeroLinea;
    public String contenido;
    public List<String> tokens;
    public List<String> tipos;
    public String error;

    public AnalisisLinea(int numeroLinea, String contenido) {
        this.numeroLinea = numeroLinea;
        this.contenido = contenido;
        this.tokens = new ArrayList<>();
        this.tipos = new ArrayList<>();
        this.error = null;
    }
}