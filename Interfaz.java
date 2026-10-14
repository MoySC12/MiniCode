import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.Arrays;
import java.util.List;

public class Interfaz extends JFrame {
    private JTextArea editorCodigo;
    private JTextArea numerosLinea; 
    private JTextArea consola;
    
    // Tablas independientes
    private JTable tablaLexica;
    private DefaultTableModel modeloLexico;
    private JTable tablaSintactica;
    private DefaultTableModel modeloSintactico;
    
    // Control de vistas (CardLayout)
    private CardLayout cardLayout;
    private JPanel panelVistas;
    private JButton btnVistaLexico;
    private JButton btnVistaSemantico;
    private JButton btnVistaSalidas;
    
    // Variables de control de archivos
    private File archivoActual = null;
    private boolean cambiosSinGuardar = false;

    // Paleta de colores aplicada
    private final Color bgCream = Color.decode("#FDFBF7");
    private final Color btnTeal = Color.decode("#2A9D8F");
    private final Color btnCoral = Color.decode("#E76F51");
    private final Color textDark = Color.decode("#264653");
    private final Color bgGris = Color.decode("#E9ECEF"); 
    private final Color btnInactivo = Color.decode("#A8DADC"); // Color para botones no seleccionados

    public Interfaz() {
        setTitle("MiniCode");
        setSize(1100, 700);
        
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                salirDelPrograma();
            }
        });

        getContentPane().setBackground(bgCream);
        
        crearMenu();
        
        setLayout(new GridLayout(1, 2, 10, 10));

        // ==========================================
        // PANEL IZQUIERDO: EDITOR DE CÓDIGO
        // ==========================================
        JPanel panelIzquierdo = new JPanel(new BorderLayout());
        panelIzquierdo.setBackground(bgCream);
        
        editorCodigo = new JTextArea();
        editorCodigo.setFont(new Font("Consolas", Font.PLAIN, 14));
        
        numerosLinea = new JTextArea("1");
        numerosLinea.setBackground(bgGris);
        numerosLinea.setForeground(Color.GRAY);
        numerosLinea.setEditable(false);
        numerosLinea.setFont(new Font("Consolas", Font.PLAIN, 14));
        numerosLinea.setMargin(new Insets(0, 5, 0, 5));
        
        JScrollPane scrollEditor = new JScrollPane(editorCodigo);
        scrollEditor.setRowHeaderView(numerosLinea); 
        
        editorCodigo.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { marcarComoModificado(); actualizarLineas(); }
            public void removeUpdate(DocumentEvent e) { marcarComoModificado(); actualizarLineas(); }
            public void changedUpdate(DocumentEvent e) { marcarComoModificado(); actualizarLineas(); }
        });
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnPanel.setBackground(bgCream);
        
        JButton btnAnalizar = new JButton("Analizar Código");
        btnAnalizar.setBackground(btnTeal);
        btnAnalizar.setForeground(Color.WHITE);
        btnAnalizar.addActionListener(e -> {
            analizarCodigo();
            // Al analizar, saltar automáticamente a la vista de Salidas para ver el resultado
            cambiarVista("SALIDAS"); 
        });
        
        JButton btnLimpiar = new JButton("Limpiar");
        btnLimpiar.setBackground(btnCoral);
        btnLimpiar.setForeground(Color.WHITE);
        btnLimpiar.addActionListener(e -> limpiar());
        
        btnPanel.add(btnAnalizar);
        btnPanel.add(btnLimpiar);
        
        panelIzquierdo.add(new JLabel(" Editor de Código"), BorderLayout.NORTH);
        panelIzquierdo.add(scrollEditor, BorderLayout.CENTER);
        panelIzquierdo.add(btnPanel, BorderLayout.SOUTH);

        // ==========================================
        // PANEL DERECHO: VISTAS INTERCAMBIABLES
        // ==========================================
        JPanel panelDerecho = new JPanel(new BorderLayout(0, 10));
        panelDerecho.setBackground(bgCream);
        
        // 1. Barra de Botones Superiores
        JPanel panelBotonesVistas = new JPanel(new GridLayout(1, 3, 5, 0));
        panelBotonesVistas.setBackground(bgCream);
        
        btnVistaLexico = new JButton("Análisis Léxico");
        btnVistaSemantico = new JButton("Análisis  Sintáctico/Semántico");
        btnVistaSalidas = new JButton("Salidas");
        
        panelBotonesVistas.add(btnVistaLexico);
        panelBotonesVistas.add(btnVistaSemantico);
        panelBotonesVistas.add(btnVistaSalidas);
        
        panelDerecho.add(panelBotonesVistas, BorderLayout.NORTH);

        // 2. Configuración del CardLayout
        cardLayout = new CardLayout();
        panelVistas = new JPanel(cardLayout);
        panelVistas.setBackground(bgCream);

        // VISTA 1: Análisis Léxico
        String[] colLexico = {"Línea", "Token", "Tipo"};
        modeloLexico = new DefaultTableModel(colLexico, 0);
        tablaLexica = new JTable(modeloLexico);
        JScrollPane scrollLexico = new JScrollPane(tablaLexica);
        JPanel panelLexico = new JPanel(new BorderLayout());
        panelLexico.setBackground(bgCream);
        panelLexico.add(scrollLexico, BorderLayout.CENTER);
        
        // VISTA 2: Análisis Semántico (Usando la tabla sintáctica actual)
        String[] colSintactico = {"Línea", "Instrucción", "Validación"};
        modeloSintactico = new DefaultTableModel(colSintactico, 0);
        tablaSintactica = new JTable(modeloSintactico);
        JScrollPane scrollSintactico = new JScrollPane(tablaSintactica);
        JPanel panelSemantico = new JPanel(new BorderLayout());
        panelSemantico.setBackground(bgCream);
        panelSemantico.add(scrollSintactico, BorderLayout.CENTER);
        
        // VISTA 3: Salidas (Consola ocupa todo el espacio ahora)
        consola = new JTextArea();
        consola.setEditable(false);
        consola.setBackground(bgGris);
        consola.setForeground(textDark);
        consola.setFont(new Font("Consolas", Font.PLAIN, 14));
        consola.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane scrollConsola = new JScrollPane(consola);
        JPanel panelSalidas = new JPanel(new BorderLayout());
        panelSalidas.setBackground(bgCream);
        panelSalidas.add(scrollConsola, BorderLayout.CENTER);

        // Agregar las 3 vistas al CardLayout
        panelVistas.add(panelLexico, "LEXICO");
        panelVistas.add(panelSemantico, "SEMANTICO");
        panelVistas.add(panelSalidas, "SALIDAS");

        panelDerecho.add(panelVistas, BorderLayout.CENTER);

        add(panelIzquierdo);
        add(panelDerecho);
        
        // 3. Eventos de los botones para cambiar de vista
        btnVistaLexico.addActionListener(e -> cambiarVista("LEXICO"));
        btnVistaSemantico.addActionListener(e -> cambiarVista("SEMANTICO"));
        btnVistaSalidas.addActionListener(e -> cambiarVista("SALIDAS"));
        
        // Iniciar con la vista Léxica por defecto
        cambiarVista("LEXICO");
    }

    // Método para cambiar la tarjeta visible y pintar los botones
    private void cambiarVista(String vista) {
        cardLayout.show(panelVistas, vista);
        
        // Reiniciar colores de todos los botones
        btnVistaLexico.setBackground(btnInactivo);
        btnVistaLexico.setForeground(textDark);
        btnVistaSemantico.setBackground(btnInactivo);
        btnVistaSemantico.setForeground(textDark);
        btnVistaSalidas.setBackground(btnInactivo);
        btnVistaSalidas.setForeground(textDark);
        
        // Pintar el botón activo de color oscuro
        if (vista.equals("LEXICO")) {
            btnVistaLexico.setBackground(textDark);
            btnVistaLexico.setForeground(Color.WHITE);
        } else if (vista.equals("SEMANTICO")) {
            btnVistaSemantico.setBackground(textDark);
            btnVistaSemantico.setForeground(Color.WHITE);
        } else if (vista.equals("SALIDAS")) {
            btnVistaSalidas.setBackground(textDark);
            btnVistaSalidas.setForeground(Color.WHITE);
        }
    }

    private void crearMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menuArchivo = new JMenu("Archivo");
        
        JMenuItem itemNuevo = new JMenuItem("Nuevo");
        JMenuItem itemAbrir = new JMenuItem("Abrir...");
        JMenuItem itemGuardar = new JMenuItem("Guardar");
        JMenuItem itemGuardarComo = new JMenuItem("Guardar como...");
        JMenuItem itemSalir = new JMenuItem("Salir");

        itemNuevo.addActionListener(e -> nuevoArchivo());
        itemAbrir.addActionListener(e -> abrirArchivo());
        itemGuardar.addActionListener(e -> guardarArchivo());
        itemGuardarComo.addActionListener(e -> guardarComoArchivo());
        itemSalir.addActionListener(e -> salirDelPrograma());

        menuArchivo.add(itemNuevo);
        menuArchivo.add(itemAbrir);
        menuArchivo.addSeparator();
        menuArchivo.add(itemGuardar);
        menuArchivo.add(itemGuardarComo);
        menuArchivo.addSeparator();
        menuArchivo.add(itemSalir);

        menuBar.add(menuArchivo);
        setJMenuBar(menuBar);
    }
    
    private void actualizarLineas() {
        SwingUtilities.invokeLater(() -> {
            int totalLineas = editorCodigo.getLineCount();
            StringBuilder textoNumeros = new StringBuilder();
            for (int i = 1; i <= totalLineas; i++) {
                textoNumeros.append(i).append("\n");
            }
            numerosLinea.setText(textoNumeros.toString());
        });
    }
    
    private void marcarComoModificado() {
        if (!cambiosSinGuardar) {
            cambiosSinGuardar = true;
            actualizarTitulo();
        }
    }

    private void actualizarTitulo() {
        String nombre = (archivoActual != null) ? archivoActual.getName() : "Sin título";
        String indicador = cambiosSinGuardar ? "*" : "";
        setTitle("MiniCode" + nombre + indicador);
    }

    private boolean confirmarGuardado() {
        if (cambiosSinGuardar) {
            int opcion = JOptionPane.showConfirmDialog(this,
                    "Tienes cambios sin guardar. ¿Deseas guardarlos?",
                    "Cambios sin guardar",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            
            if (opcion == JOptionPane.YES_OPTION) {
                return guardarArchivo(); 
            } else if (opcion == JOptionPane.CANCEL_OPTION || opcion == JOptionPane.CLOSED_OPTION) {
                return false;
            }
        }
        return true;
    }

    private void salirDelPrograma() {
        if (confirmarGuardado()) {
            System.exit(0);
        }
    }

    private void nuevoArchivo() {
        if (!confirmarGuardado()) return; 
        
        editorCodigo.setText("");
        archivoActual = null;
        cambiosSinGuardar = false;
        limpiar(); 
        actualizarTitulo();
    }

    private void abrirArchivo() {
        if (!confirmarGuardado()) return;

        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filtro = new FileNameExtensionFilter("Archivos de texto (*.txt)", "txt");
        fileChooser.setFileFilter(filtro);
        
        int seleccion = fileChooser.showOpenDialog(this);
        if (seleccion == JFileChooser.APPROVE_OPTION) {
            File archivoSeleccionado = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(archivoSeleccionado))) {
                editorCodigo.read(reader, null);
                archivoActual = archivoSeleccionado;
                cambiosSinGuardar = false;
                actualizarTitulo();
                actualizarLineas();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error al abrir el archivo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private boolean guardarArchivo() {
        if (archivoActual != null) {
            return guardarEnArchivo(archivoActual);
        } else {
            return guardarComoArchivo();
        }
    }

    private boolean guardarComoArchivo() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filtro = new FileNameExtensionFilter("Archivos de texto (*.txt)", "txt");
        fileChooser.setFileFilter(filtro);
        
        int seleccion = fileChooser.showSaveDialog(this);
        if (seleccion == JFileChooser.APPROVE_OPTION) {
            File archivoSeleccionado = fileChooser.getSelectedFile();
            
            if (!archivoSeleccionado.getName().toLowerCase().endsWith(".txt")) {
                archivoSeleccionado = new File(archivoSeleccionado.getAbsolutePath() + ".txt");
            }
            
            archivoActual = archivoSeleccionado;
            return guardarEnArchivo(archivoActual);
        }
        return false; 
    }

    private boolean guardarEnArchivo(File archivo) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivo))) {
            editorCodigo.write(writer);
            cambiosSinGuardar = false;
            actualizarTitulo();
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al guardar el archivo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void analizarCodigo() {
        modeloLexico.setRowCount(0);
        modeloSintactico.setRowCount(0);
        consola.setText("");
        
        String texto = editorCodigo.getText().trim();
        if (texto.isEmpty()) {
            consola.setText("El editor está vacío. Ingresa código para analizar.");
            return;
        }

        List<String> lineas = Arrays.asList(texto.split("\\n"));
        List<AnalisisLinea> resultados = MiniCode.analizarCodigo(lineas);
        
        StringBuilder errores = new StringBuilder();

        for (AnalisisLinea linea : resultados) {
            int maxItems = Math.max(linea.tokens.size(), linea.tipos.size());
            for (int i = 0; i < maxItems; i++) {
                String token = i < linea.tokens.size() ? linea.tokens.get(i) : "";
                String tipo = i < linea.tipos.size() ? linea.tipos.get(i) : "";
                modeloLexico.addRow(new Object[]{linea.numeroLinea, token, tipo});
                
                if (tipo.equals("Token no reconocido")) {
                    errores.append("LÉXICO (Línea ").append(linea.numeroLinea).append("): Token no reconocido '").append(token).append("'\n");
                }
            }
            
            String estadoSintactico = (linea.error == null) ? "Correcto" : linea.error;
            
            if (!linea.contenido.trim().isEmpty()) {
                modeloSintactico.addRow(new Object[]{linea.numeroLinea, linea.contenido, estadoSintactico});
            }

            if (linea.error != null) {
                String tipoError = linea.error.startsWith("Error Sintáctico") ? "SINTÁCTICO" : "SEMÁNTICO";
                errores.append(tipoError + " (Línea ").append(linea.numeroLinea).append("): ").append(linea.error).append("\n");
            }
        }
        
        if (errores.length() == 0) {
            consola.setText("El programa es válido.\nNo se encontraron errores léxicos ni semánticos.");
        } else {
            consola.setText("Se encontraron errores:\n\n" + errores.toString());
        }
    }

    private void limpiar() {
        editorCodigo.setText(""); 
        modeloLexico.setRowCount(0);
        modeloSintactico.setRowCount(0);
        consola.setText("");
        actualizarLineas(); 
        cambiarVista("LEXICO"); // Regresa a la vista original al limpiar
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Interfaz().setVisible(true));
    }
}