package org.example;

// Importaciones para la GUI (Swing)
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

// Importaciones para manejo de archivos y streams
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;

// Importaciones de las clases generadas por JFlex y CUP
import com.example.jscompiler.JavaScriptLexer;
import com.example.jscompiler.JavaScriptParser;
import java_cup.runtime.Symbol;

public class Main extends JFrame {

    private JTextArea inputCodeArea;
    private JTextArea lexicalErrorOutputArea;
    private JTextArea syntaxAndGeneralOutputArea;
    private JButton openFileButton;
    private JButton compileButton;
    private JLabel statusLabel;

    private String currentSourceName = "EntradaEnVivo.js"; // Nombre por defecto para el código tipeado

    public Main() {
        super("Mini IDE Compilador JS - Paneles de Error");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 750);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(5, 5));

        // --- Panel de Botones Superior ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        openFileButton = new JButton("Abrir Archivo JS");
        compileButton = new JButton("Compilar Código Actual");
        topPanel.add(openFileButton);
        topPanel.add(compileButton);
        add(topPanel, BorderLayout.NORTH);

        // --- Área de Input de Código ---
        inputCodeArea = new JTextArea();
        inputCodeArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane inputScrollPane = new JScrollPane(inputCodeArea);

        // --- Panel Contenedor de Errores (dividido verticalmente) ---
        JPanel errorContainerPanel = new JPanel(new GridLayout(2, 1, 5, 5)); // 2 filas, 1 columna

        // Panel para Errores Léxicos
        JPanel lexicalPanel = new JPanel(new BorderLayout(2,2));
        lexicalPanel.setBorder(BorderFactory.createTitledBorder("Errores Léxicos"));
        lexicalErrorOutputArea = new JTextArea();
        lexicalErrorOutputArea.setEditable(false);
        lexicalErrorOutputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        lexicalErrorOutputArea.setBackground(new Color(255, 245, 245)); // Fondo ligeramente rosado para errores
        lexicalPanel.add(new JScrollPane(lexicalErrorOutputArea), BorderLayout.CENTER);
        errorContainerPanel.add(lexicalPanel);

        // Panel para Errores Sintácticos, Semánticos (futuro) y Salida General
        JPanel syntaxSemanticPanel = new JPanel(new BorderLayout(2,2));
        syntaxSemanticPanel.setBorder(BorderFactory.createTitledBorder("Errores Sintácticos / Semánticos y Salida General"));
        syntaxAndGeneralOutputArea = new JTextArea();
        syntaxAndGeneralOutputArea.setEditable(false);
        syntaxAndGeneralOutputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        syntaxAndGeneralOutputArea.setBackground(new Color(245, 245, 255)); // Fondo ligeramente azulado
        syntaxSemanticPanel.add(new JScrollPane(syntaxAndGeneralOutputArea), BorderLayout.CENTER);
        errorContainerPanel.add(syntaxSemanticPanel);

        // JSplitPane principal: input de código arriba, panel de errores abajo
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputScrollPane, errorContainerPanel);
        mainSplitPane.setResizeWeight(0.55);
        add(mainSplitPane, BorderLayout.CENTER);

        // --- Etiqueta de Estado Inferior ---
        statusLabel = new JLabel("Fuente actual: " + currentSourceName);
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(statusLabel);
        add(statusPanel, BorderLayout.SOUTH);

        // --- ActionListeners ---
        openFileButton.addActionListener(e -> openFileAction());
        compileButton.addActionListener(e -> compileAction());
    }

    private void openFileAction() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Seleccionar archivo JavaScript");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos JavaScript (*.js)", "js"));
        fileChooser.setCurrentDirectory(new File("."));

        int result = fileChooser.showOpenDialog(Main.this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                String content = new String(Files.readAllBytes(selectedFile.toPath()));
                inputCodeArea.setText(content);
                currentSourceName = selectedFile.getAbsolutePath();
                statusLabel.setText("Editando archivo: " + selectedFile.getName());
                lexicalErrorOutputArea.setText("");
                syntaxAndGeneralOutputArea.setText("Archivo '" + selectedFile.getName() + "' cargado.\nPresiona 'Compilar Código Actual'.");
            } catch (IOException ex) {
                syntaxAndGeneralOutputArea.setText("Error al leer el archivo: " + selectedFile.getName() + "\n" + ex.getMessage());
                currentSourceName = "ErrorAlCargar.js";
                statusLabel.setText("Error al cargar archivo.");
            }
        }
    }

    private void compileAction() {

        lexicalErrorOutputArea.setText("");
        syntaxAndGeneralOutputArea.setText("");
        String jsCode = inputCodeArea.getText();
        if (jsCode.trim().isEmpty()) {
            syntaxAndGeneralOutputArea.setText("El área de código está vacía. Escribe o abre un archivo JS.");
            lexicalErrorOutputArea.setText("");
            return;
        }
        processJavaScript(jsCode, currentSourceName);
    }

    private void processJavaScript(String jsCode, String sourceNameForReporting) {
        lexicalErrorOutputArea.setText("");
        syntaxAndGeneralOutputArea.setText("");

        String displayName = new File(sourceNameForReporting).getName();
        syntaxAndGeneralOutputArea.append("=== INICIANDO COMPILACIÓN ===\n");
        syntaxAndGeneralOutputArea.append("Archivo: " + displayName + "\n");
        syntaxAndGeneralOutputArea.append("Líneas de código: " + jsCode.split("\\R").length + "\n");
        syntaxAndGeneralOutputArea.append("=====================================\n\n");

        // Capturar tanto System.out como System.err
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        ByteArrayOutputStream baosOut = new ByteArrayOutputStream();
        ByteArrayOutputStream baosErr = new ByteArrayOutputStream();

        PrintStream newOut = new PrintStream(baosOut);
        PrintStream newErr = new PrintStream(baosErr);

        System.setOut(newOut);
        System.setErr(newErr);

        boolean hasLexicalErrors = false;
        boolean hasSyntaxErrors = false;
        boolean compilationSuccessful = false;

        try {
            // Análisis Léxico
            syntaxAndGeneralOutputArea.append("FASE 1: Análisis Léxico...\n");
            StringReader readerForLexer = new StringReader(jsCode);
            JavaScriptLexer lexer = new JavaScriptLexer(readerForLexer);

            // Verificar tokens básicos (opcional - para debug)
            syntaxAndGeneralOutputArea.append("Iniciando verificación de tokens...\n");

            // Análisis Sintáctico
            syntaxAndGeneralOutputArea.append("\nFASE 2: Análisis Sintáctico...\n");
            StringReader readerForParser = new StringReader(jsCode);
            JavaScriptLexer lexerForParser = new JavaScriptLexer(readerForParser);
            JavaScriptParser parser = new JavaScriptParser(lexerForParser);

            // Ejecutar el parser
            Symbol result = parser.parse();

            if (result != null) {
                compilationSuccessful = true;
                syntaxAndGeneralOutputArea.append("Análisis sintáctico completado exitosamente.\n");
            }

        } catch (Exception e) {
            syntaxAndGeneralOutputArea.append("ERROR DURANTE LA COMPILACIÓN:\n");
            syntaxAndGeneralOutputArea.append("Tipo: " + e.getClass().getSimpleName() + "\n");
            syntaxAndGeneralOutputArea.append("Mensaje: " + e.getMessage() + "\n");

            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("sintax")) {
                hasSyntaxErrors = true;
            }

            // Stack trace completo para errores críticos
            e.printStackTrace(newErr);
        } finally {
            // Restaurar streams originales
            System.setOut(originalOut);
            System.setErr(originalErr);

            // Procesar salida capturada
            String capturedOut = baosOut.toString();
            String capturedErr = baosErr.toString();

            // Procesar errores léxicos y sintácticos
            processErrorOutput(capturedOut, capturedErr);

            // Mostrar resultado final
            syntaxAndGeneralOutputArea.append("\n=====================================\n");
            syntaxAndGeneralOutputArea.append("=== RESULTADO DE LA COMPILACIÓN ===\n");

            if (compilationSuccessful && !hasLexicalErrors && !hasSyntaxErrors) {
                syntaxAndGeneralOutputArea.append("✓ COMPILACIÓN EXITOSA\n");
                syntaxAndGeneralOutputArea.append("El código JavaScript es sintácticamente correcto.\n");
            } else {
                syntaxAndGeneralOutputArea.append("✗ COMPILACIÓN CON ERRORES\n");
                if (hasLexicalErrors) {
                    syntaxAndGeneralOutputArea.append("- Se encontraron errores léxicos\n");
                }
                if (hasSyntaxErrors) {
                    syntaxAndGeneralOutputArea.append("- Se encontraron errores sintácticos\n");
                }
            }

            syntaxAndGeneralOutputArea.append("=====================================\n");

            // Limpiar streams
            try {
                baosOut.close();
                baosErr.close();
                newOut.close();
                newErr.close();
            } catch (IOException e) {
                // Ignorar errores de cierre
            }
        }
    }

    private void processErrorOutput(String capturedOut, String capturedErr) {
        StringBuilder lexicalErrors = new StringBuilder();
        StringBuilder syntaxErrors = new StringBuilder();
        StringBuilder generalOutput = new StringBuilder();

        // Procesar salida estándar
        if (!capturedOut.trim().isEmpty()) {
            String[] outLines = capturedOut.split("\\R");
            for (String line : outLines) {
                if (line.toLowerCase().contains("léxico") || line.toLowerCase().contains("lexico") ||
                        line.toLowerCase().contains("token") || line.contains("[LEXICO]")) {
                    lexicalErrors.append(line.replace("[LEXICO]", "").trim()).append("\n");
                } else if (line.toLowerCase().contains("sintáctic") || line.toLowerCase().contains("sintactic") ||
                        line.toLowerCase().contains("syntax") || line.contains("[SINTACTICO]")) {
                    syntaxErrors.append(line.replace("[SINTACTICO]", "").trim()).append("\n");
                } else if (!line.trim().isEmpty()) {
                    generalOutput.append(line).append("\n");
                }
            }
        }

        // Procesar salida de error
        if (!capturedErr.trim().isEmpty()) {
            String[] errLines = capturedErr.split("\\R");
            for (String line : errLines) {
                if (line.toLowerCase().contains("léxico") || line.toLowerCase().contains("lexico") ||
                        line.contains("[LEXICO]")) {
                    lexicalErrors.append(line.replace("[LEXICO]", "").trim()).append("\n");
                } else if (line.toLowerCase().contains("sintáctic") || line.toLowerCase().contains("sintactic") ||
                        line.toLowerCase().contains("syntax") || line.contains("[SINTACTICO]")) {
                    syntaxErrors.append(line.replace("[SINTACTICO]", "").trim()).append("\n");
                } else if (line.toLowerCase().contains("error")) {
                    syntaxErrors.append(line).append("\n");
                } else if (!line.trim().isEmpty()) {
                    generalOutput.append(line).append("\n");
                }
            }
        }

        // Actualizar áreas de texto
        if (lexicalErrors.length() > 0) {
            lexicalErrorOutputArea.setText("=== ERRORES LÉXICOS ENCONTRADOS ===\n" + lexicalErrors.toString());
        } else {
            lexicalErrorOutputArea.setText("✓ No se encontraron errores léxicos");
        }

        if (syntaxErrors.length() > 0) {
            syntaxAndGeneralOutputArea.append("\n=== ERRORES SINTÁCTICOS ===\n" + syntaxErrors.toString());
        }

        if (generalOutput.length() > 0) {
            syntaxAndGeneralOutputArea.append("\n=== INFORMACIÓN ADICIONAL ===\n" + generalOutput.toString());
        }
    }

    public static void main(String[] args) {
        // Configurar look and feel del sistema
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Usar look and feel por defectos
        }

        SwingUtilities.invokeLater(() -> {
            Main frame = new Main();
            frame.setVisible(true);
        });
    }
}