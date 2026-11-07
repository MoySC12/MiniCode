import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class Interfaz extends JFrame {
    private JTextArea editorCodigo;
    private JTextArea numerosLinea; 
    private JTextArea consola;
    private JTextArea areaIntermedio;
    
    // Componentes para la vista de Optimización
    private JTextArea areaOptNuevo;
    private JTextArea numerosOptNuevo;
    private JTable tablaCambiosOpt;
    private DefaultTableModel modeloCambiosOpt;
    private JLabel lblMetricasOptimizacion;
    
    // Tablas independientes para Léxico, Sintáctico y Semántico
    private JTable tablaLexica;
    private DefaultTableModel modeloLexico;
    private JTable tablaSintactica;
    private DefaultTableModel modeloSintactico;
    private JTable tablaSemantica;
    private DefaultTableModel modeloSemantico;
    
    // Control de vistas (CardLayout de 6 pestañas)
    private CardLayout cardLayout;
    private JPanel panelVistas;
    private JButton btnVistaLexico;
    private JButton btnVistaSintactico;
    private JButton btnVistaSemantico;
    private JButton btnVistaIntermedio;
    private JButton btnVistaOptimizacion;
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
    private final Color btnInactivo = Color.decode("#A8DADC");

    public Interfaz() {
        setTitle("MiniCode");
        setSize(1320, 750);
        
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                salirDelPrograma();
            }
        });

        getContentPane().setBackground(bgCream);
        
        crearMenu();
        
        setLayout(new BorderLayout());

        // ==========================================
        // PANEL IZQUIERDO: EDITOR DE CÓDIGO
        // ==========================================
        JPanel panelIzquierdo = new JPanel(new BorderLayout());
        panelIzquierdo.setBackground(bgCream);
        panelIzquierdo.setMinimumSize(new Dimension(250, 400));
        
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
        // PANEL DERECHO: 6 VISTAS INTERCAMBIABLES
        // ==========================================
        JPanel panelDerecho = new JPanel(new BorderLayout(0, 10));
        panelDerecho.setBackground(bgCream);
        panelDerecho.setMinimumSize(new Dimension(400, 400));
        
        // 1. Barra de Botones Superiores (6 vistas independientes)
        JPanel panelBotonesVistas = new JPanel(new GridLayout(1, 6, 4, 0));
        panelBotonesVistas.setBackground(bgCream);
        
        btnVistaLexico = new JButton("Léxico");
        btnVistaSintactico = new JButton("Sintáctico");
        btnVistaSemantico = new JButton("Semántico");
        btnVistaIntermedio = new JButton("Cód. Intermedio");
        btnVistaOptimizacion = new JButton("Optimización");
        btnVistaSalidas = new JButton("Salidas");
        
        panelBotonesVistas.add(btnVistaLexico);
        panelBotonesVistas.add(btnVistaSintactico);
        panelBotonesVistas.add(btnVistaSemantico);
        panelBotonesVistas.add(btnVistaIntermedio);
        panelBotonesVistas.add(btnVistaOptimizacion);
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
        
        // VISTA 2: Análisis Sintáctico (Tabla independiente)
        String[] colSintactico = {"Línea", "Instrucción", "Validación Sintáctica"};
        modeloSintactico = new DefaultTableModel(colSintactico, 0);
        tablaSintactica = new JTable(modeloSintactico);
        JScrollPane scrollSintactico = new JScrollPane(tablaSintactica);
        JPanel panelSintactico = new JPanel(new BorderLayout());
        panelSintactico.setBackground(bgCream);
        panelSintactico.add(scrollSintactico, BorderLayout.CENTER);

        // VISTA 3: Análisis Semántico (Tabla independiente)
        String[] colSemantico = {"Línea", "Instrucción", "Validación Semántica"};
        modeloSemantico = new DefaultTableModel(colSemantico, 0);
        tablaSemantica = new JTable(modeloSemantico);
        JScrollPane scrollSemantico = new JScrollPane(tablaSemantica);
        JPanel panelSemantico = new JPanel(new BorderLayout());
        panelSemantico.setBackground(bgCream);
        panelSemantico.add(scrollSemantico, BorderLayout.CENTER);

        // VISTA 4: Código Intermedio (Original y Optimizado)
        areaIntermedio = new JTextArea();
        areaIntermedio.setEditable(false);
        areaIntermedio.setBackground(bgGris);
        areaIntermedio.setForeground(textDark);
        areaIntermedio.setFont(new Font("Consolas", Font.PLAIN, 14));
        areaIntermedio.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane scrollIntermedio = new JScrollPane(areaIntermedio);
        JPanel panelIntermedio = new JPanel(new BorderLayout());
        panelIntermedio.setBackground(bgCream);
        panelIntermedio.add(scrollIntermedio, BorderLayout.CENTER);

        // VISTA 5: Optimización de Código (Código + Tabla de Líneas Sustituidas + Guardar)
        JPanel panelOptimizacion = construirPanelOptimizacion();
        
        // VISTA 6: Salidas (Consola)
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

        // Agregar las 6 vistas al CardLayout
        panelVistas.add(panelLexico, "LEXICO");
        panelVistas.add(panelSintactico, "SINTACTICO");
        panelVistas.add(panelSemantico, "SEMANTICO");
        panelVistas.add(panelIntermedio, "INTERMEDIO");
        panelVistas.add(panelOptimizacion, "OPTIMIZACION");
        panelVistas.add(panelSalidas, "SALIDAS");

        panelDerecho.add(panelVistas, BorderLayout.CENTER);

        // Separador redimensionable entre el Editor de Código y las Vistas de Salida
        JSplitPane splitPrincipal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelIzquierdo, panelDerecho);
        splitPrincipal.setResizeWeight(0.42);
        splitPrincipal.setDividerLocation(520);
        splitPrincipal.setOneTouchExpandable(true);
        splitPrincipal.setContinuousLayout(true);
        splitPrincipal.setBackground(bgCream);

        add(splitPrincipal, BorderLayout.CENTER);
        
        // 3. Eventos de los botones para cambiar de vista
        btnVistaLexico.addActionListener(e -> cambiarVista("LEXICO"));
        btnVistaSintactico.addActionListener(e -> cambiarVista("SINTACTICO"));
        btnVistaSemantico.addActionListener(e -> cambiarVista("SEMANTICO"));
        btnVistaIntermedio.addActionListener(e -> cambiarVista("INTERMEDIO"));
        btnVistaOptimizacion.addActionListener(e -> cambiarVista("OPTIMIZACION"));
        btnVistaSalidas.addActionListener(e -> cambiarVista("SALIDAS"));
        
        cambiarVista("LEXICO");
    }

    private JPanel construirPanelOptimizacion() {
        JPanel panelOptimizacion = new JPanel(new BorderLayout(0, 5));
        panelOptimizacion.setBackground(bgCream);

        // Barra superior con título y botón para guardar el código optimizado
        JPanel barraAccionesOpt = new JPanel(new BorderLayout());
        barraAccionesOpt.setBackground(bgCream);
        barraAccionesOpt.add(new JLabel(" Código Optimizado"), BorderLayout.WEST);

        JButton btnGuardarOpt = new JButton("Guardar Optimizado...");
        btnGuardarOpt.setBackground(btnTeal);
        btnGuardarOpt.setForeground(Color.WHITE);
        btnGuardarOpt.addActionListener(e -> guardarCodigoOptimizado());

        JPanel panelBotonDerecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        panelBotonDerecha.setBackground(bgCream);
        panelBotonDerecha.add(btnGuardarOpt);
        barraAccionesOpt.add(panelBotonDerecha, BorderLayout.EAST);

        panelOptimizacion.add(barraAccionesOpt, BorderLayout.NORTH);

        // Área de Código Optimizado (Mitad superior)
        areaOptNuevo = new JTextArea();
        areaOptNuevo.setEditable(false);
        areaOptNuevo.setBackground(bgGris);
        areaOptNuevo.setForeground(textDark);
        areaOptNuevo.setFont(new Font("Consolas", Font.PLAIN, 13));
        areaOptNuevo.setMargin(new Insets(5, 5, 5, 5));

        numerosOptNuevo = new JTextArea("1");
        numerosOptNuevo.setBackground(Color.decode("#DEE2E6"));
        numerosOptNuevo.setForeground(Color.GRAY);
        numerosOptNuevo.setEditable(false);
        numerosOptNuevo.setFont(new Font("Consolas", Font.PLAIN, 13));
        numerosOptNuevo.setMargin(new Insets(5, 5, 5, 5));

        JScrollPane scrollOptUnico = new JScrollPane(areaOptNuevo);
        scrollOptUnico.setRowHeaderView(numerosOptNuevo);

        // Tabla inferior: Bitácora de líneas sustituidas y eliminadas (Mitad inferior)
        String[] colCambios = {"Línea Orig.", "Técnica Aplicada", "Original", "Sustituida"};
        modeloCambiosOpt = new DefaultTableModel(colCambios, 0);
        tablaCambiosOpt = new JTable(modeloCambiosOpt);
        tablaCambiosOpt.getColumnModel().getColumn(0).setMaxWidth(85);
        JScrollPane scrollTablaCambios = new JScrollPane(tablaCambiosOpt);

        JPanel panelInferiorTabla = new JPanel(new BorderLayout());
        panelInferiorTabla.setBackground(bgCream);
        panelInferiorTabla.add(new JLabel(" Líneas Optimizadas y Sustituidas:"), BorderLayout.NORTH);
        panelInferiorTabla.add(scrollTablaCambios, BorderLayout.CENTER);

        // Split vertical entre el Código Optimizado (arriba) y la Tabla de Sustituciones (abajo)
        JSplitPane splitVerticalOpt = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollOptUnico, panelInferiorTabla);
        splitVerticalOpt.setResizeWeight(0.58);
        splitVerticalOpt.setDividerLocation(330);
        splitVerticalOpt.setOneTouchExpandable(true);

        lblMetricasOptimizacion = new JLabel(" Tiempo de optimización: 0 ms | Tamaño original: 0 bytes | Optimizado: 0 bytes");
        lblMetricasOptimizacion.setFont(new Font("Consolas", Font.BOLD, 12));
        lblMetricasOptimizacion.setForeground(textDark);
        lblMetricasOptimizacion.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        panelOptimizacion.add(splitVerticalOpt, BorderLayout.CENTER);
        panelOptimizacion.add(lblMetricasOptimizacion, BorderLayout.SOUTH);

        return panelOptimizacion;
    }

    private void guardarCodigoOptimizado() {
        String codigoOpt = areaOptNuevo.getText().trim();
        if (codigoOpt.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Primero analiza un código válido para generar la versión optimizada.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filtro = new FileNameExtensionFilter("Archivos de texto (*.txt)", "txt");
        fileChooser.setFileFilter(filtro);

        String sugerido = (archivoActual != null)
                ? archivoActual.getName().replaceFirst("(\\.txt)?$", "_optimizado.txt")
                : "codigo_optimizado.txt";
        fileChooser.setSelectedFile(new File(sugerido));

        int seleccion = fileChooser.showSaveDialog(this);
        if (seleccion == JFileChooser.APPROVE_OPTION) {
            File destino = fileChooser.getSelectedFile();
            if (!destino.getName().toLowerCase().endsWith(".txt")) {
                destino = new File(destino.getAbsolutePath() + ".txt");
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(destino))) {
                writer.write(codigoOpt);
                JOptionPane.showMessageDialog(this, "Código optimizado guardado en:\n" + destino.getAbsolutePath(), "Guardado exitoso", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar el archivo optimizado: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void cambiarVista(String vista) {
        cardLayout.show(panelVistas, vista);
        
        JButton[] botones = {btnVistaLexico, btnVistaSintactico, btnVistaSemantico, btnVistaIntermedio, btnVistaOptimizacion, btnVistaSalidas};
        for (JButton b : botones) {
            b.setBackground(btnInactivo);
            b.setForeground(textDark);
        }
        
        if (vista.equals("LEXICO")) {
            btnVistaLexico.setBackground(textDark);
            btnVistaLexico.setForeground(Color.WHITE);
        } else if (vista.equals("SINTACTICO")) {
            btnVistaSintactico.setBackground(textDark);
            btnVistaSintactico.setForeground(Color.WHITE);
        } else if (vista.equals("SEMANTICO")) {
            btnVistaSemantico.setBackground(textDark);
            btnVistaSemantico.setForeground(Color.WHITE);
        } else if (vista.equals("INTERMEDIO")) {
            btnVistaIntermedio.setBackground(textDark);
            btnVistaIntermedio.setForeground(Color.WHITE);
        } else if (vista.equals("OPTIMIZACION")) {
            btnVistaOptimizacion.setBackground(textDark);
            btnVistaOptimizacion.setForeground(Color.WHITE);
        } else if (vista.equals("SALIDAS")) {
            btnVistaSalidas.setBackground(textDark);
            btnVistaSalidas.setForeground(Color.WHITE);
        }
    }

    private void crearMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menuArchivo = new JMenu("Archivo");
        
        JMenuItem itemNuevo = new JMenuItem("Nuevo");
        JMenuItem itemAbrir = new JMenuItem("Abrir");
        JMenuItem itemGuardar = new JMenuItem("Guardar");
        JMenuItem itemGuardarComo = new JMenuItem("Guardar como");
        JMenuItem itemGuardarOpt = new JMenuItem("Guardar código optimizado");
        JMenuItem itemSalir = new JMenuItem("Salir");

        itemNuevo.addActionListener(e -> nuevoArchivo());
        itemAbrir.addActionListener(e -> abrirArchivo());
        itemGuardar.addActionListener(e -> guardarArchivo());
        itemGuardarComo.addActionListener(e -> guardarComoArchivo());
        itemGuardarOpt.addActionListener(e -> guardarCodigoOptimizado());
        itemSalir.addActionListener(e -> salirDelPrograma());

        menuArchivo.add(itemNuevo);
        menuArchivo.add(itemAbrir);
        menuArchivo.addSeparator();
        menuArchivo.add(itemGuardar);
        menuArchivo.add(itemGuardarComo);
        menuArchivo.addSeparator();
        menuArchivo.add(itemGuardarOpt);
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

    private void actualizarLineasOptimizacion(JTextArea areaTexto, JTextArea areaNumeros) {
        int totalLineas = Math.max(1, areaTexto.getLineCount());
        StringBuilder textoNumeros = new StringBuilder();
        for (int i = 1; i <= totalLineas; i++) {
            textoNumeros.append(i).append("\n");
        }
        areaNumeros.setText(textoNumeros.toString());
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
        setTitle("MiniCode - " + nombre + indicador);
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
        modeloSemantico.setRowCount(0);
        modeloCambiosOpt.setRowCount(0);
        consola.setText("");
        areaIntermedio.setText("");
        areaOptNuevo.setText("");
        
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
            
            if (!linea.contenido.trim().isEmpty()) {
                // Poblar Tabla Sintáctica independiente
                String estadoSintactico = (linea.errorSintactico == null) ? "Correcto" : linea.errorSintactico;
                modeloSintactico.addRow(new Object[]{linea.numeroLinea, linea.contenido, estadoSintactico});

                // Poblar Tabla Semántica independiente
                String estadoSemantico;
                if (linea.errorSintactico != null) {
                    estadoSemantico = "No evaluado (Error Sintáctico previo)";
                } else {
                    estadoSemantico = (linea.errorSemantico == null) ? "Correcto" : linea.errorSemantico;
                }
                modeloSemantico.addRow(new Object[]{linea.numeroLinea, linea.contenido, estadoSemantico});
            }

            if (linea.errorSintactico != null) {
                errores.append("SINTÁCTICO (Línea ").append(linea.numeroLinea).append("): ").append(linea.errorSintactico).append("\n");
            }
            if (linea.errorSemantico != null) {
                errores.append("SEMÁNTICO (Línea ").append(linea.numeroLinea).append("): ").append(linea.errorSemantico).append("\n");
            }
        }
        
        if (errores.length() == 0) {
            consola.setText("El programa es válido.");
            
            // 1. Generar el reporte de Código Intermedio (Original y Optimizado)
            String reporteIntermedio = CodigoIntermedio.generarReporte(resultados);
            areaIntermedio.setText(reporteIntermedio);
            areaIntermedio.setCaretPosition(0);
            
            // 2. Generar Optimización con bitácora de líneas sustituidas
            long inicioTiempo = System.currentTimeMillis();
            Optimizador.ResultadoOptimizacion resOpt = Optimizador.optimizarConDetalle(texto);
            long finTiempo = System.currentTimeMillis();
            long tiempoTotal = finTiempo - inicioTiempo;
            
            String codigoOptimizado = resOpt.codigoOptimizado;
            areaOptNuevo.setText(codigoOptimizado);
            areaOptNuevo.setCaretPosition(0);
            actualizarLineasOptimizacion(areaOptNuevo, numerosOptNuevo);

            for (Optimizador.CambioOptimizacion c : resOpt.cambios) {
                modeloCambiosOpt.addRow(new Object[]{
                        c.numeroLinea,
                        c.tecnica,
                        c.lineaOriginal,
                        c.lineaOptimizada
                });
            }
            
            int bytesOriginal = texto.getBytes(StandardCharsets.UTF_8).length;
            int bytesOptimizado = codigoOptimizado.getBytes(StandardCharsets.UTF_8).length;
            int lineasOrig = texto.split("\\r?\\n").length;
            int lineasOpt = codigoOptimizado.isEmpty() ? 0 : codigoOptimizado.split("\\r?\\n").length;
            
            lblMetricasOptimizacion.setText(String.format(
                    " Tiempo: %d ms | Líneas: %d -> %d | Cambios: %d | Tamaño original: %d bytes | Optimizado: %d bytes",
                    tiempoTotal, lineasOrig, lineasOpt, resOpt.cambios.size(), bytesOriginal, bytesOptimizado));
        } else {
            consola.setText("Se encontraron errores:\n\n" + errores.toString());
            areaIntermedio.setText("No se puede generar el código intermedio debido a que existen errores en el código fuente.\nCorrige los errores.");
            areaOptNuevo.setText("No se puede optimizar el código debido a que existen errores en el código fuente.\nCorrige los errores.");
            actualizarLineasOptimizacion(areaOptNuevo, numerosOptNuevo);
            lblMetricasOptimizacion.setText(" Optimización cancelada por errores en el código fuente.");
        }
    }

    private void limpiar() {
        editorCodigo.setText(""); 
        modeloLexico.setRowCount(0);
        modeloSintactico.setRowCount(0);
        modeloSemantico.setRowCount(0);
        modeloCambiosOpt.setRowCount(0);
        consola.setText("");
        areaIntermedio.setText("");
        areaOptNuevo.setText("");
        numerosOptNuevo.setText("1");
        lblMetricasOptimizacion.setText(" Tiempo de optimización: 0 ms | Tamaño original: 0 bytes | Optimizado: 0 bytes");
        actualizarLineas(); 
        cambiarVista("LEXICO");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Interfaz().setVisible(true));
    }
}