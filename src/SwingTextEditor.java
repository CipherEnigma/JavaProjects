import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.awt.geom.Ellipse2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.io.File;
import java.util.ArrayList;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.*;
import javax.swing.text.*;


public class SwingTextEditor extends JFrame {
    private JTextArea textArea;
    private JTabbedPane tabbedPane;
    private JPanel canvasPanel;
    private EnhancedCanvas drawingCanvas;
    private JLabel statusLabel;
    private JComboBox<String> fontSelector;
    private JComboBox<Integer> fontSizeSelector;
    private File currentFile = null;
    private JToolBar shapeToolbar;
    private JColorChooser colorChooser;
    private Color currentColor = Color.BLUE;

    public SwingTextEditor() {
        setTitle("Enhanced Text Editor and Drawing Canvas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);

        setupMainComponents();
        setupMenuBar();
        setupToolbars();
        setupKeyboardShortcuts();
        setupAutosave();
        setupStatusBar();

        setVisible(true);
    }

    private void setupAutosave() {
        // Implement autosave functionality if needed
    }

    private void setupMainComponents() {
        tabbedPane = new JTabbedPane();
        textArea = createTextEditor();
        textArea.addCaretListener(e -> updateStatusBar());
        tabbedPane.addTab("Text Editor", new JScrollPane(textArea));
        setupCanvasTab();
        add(tabbedPane, BorderLayout.CENTER);
    }

    private JTextArea createTextEditor() {
        JTextArea area = new JTextArea();
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Monospaced", Font.PLAIN, 14));
        return area;
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");
        addMenuItem(fileMenu, "New", "Ctrl+N", e -> newFile());
        addMenuItem(fileMenu, "Open", "Ctrl+O", e -> openFile());
        addMenuItem(fileMenu, "Save", "Ctrl+S", e -> saveFile());
        addMenuItem(fileMenu, "Save As", "Ctrl+Shift+S", e -> saveFileAs());

        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        addMenuItem(editMenu, "Cut", "Ctrl+X", e -> textArea.cut());
        addMenuItem(editMenu, "Copy", "Ctrl+C", e -> textArea.copy());
        addMenuItem(editMenu, "Paste", "Ctrl+V", e -> textArea.paste());
        editMenu.addSeparator();
        addMenuItem(editMenu, "Find", "Ctrl+F", e -> showFindDialog());
        addMenuItem(editMenu, "Replace", "Ctrl+H", e -> showReplaceDialog());

        // Format Menu
        JMenu formatMenu = new JMenu("Format");
        addMenuItem(formatMenu, "Bold", null, e -> toggleBold());
        addMenuItem(formatMenu, "Italic", null, e -> toggleItalic());
        addMenuItem(formatMenu, "Text Color", null, e -> {
            Color newColor = JColorChooser.showDialog(this, "Choose Text Color", textArea.getForeground());
            if (newColor != null) {
                textArea.setForeground(newColor);
            }
        });
        addMenuItem(formatMenu, "Highlight", null, e -> {
            Color newColor = JColorChooser.showDialog(this, "Choose Highlight Color", Color.YELLOW);
            if (newColor != null) {
                DefaultHighlighter.DefaultHighlightPainter highlighter =
                        new DefaultHighlighter.DefaultHighlightPainter(newColor);
                try {
                    int start = textArea.getSelectionStart();
                    int end = textArea.getSelectionEnd();
                    textArea.getHighlighter().addHighlight(start, end, highlighter);
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        });
        formatMenu.addSeparator();
        addMenuItem(formatMenu, "Upper Case", "Ctrl+Shift+U", e -> changeCase(true));
        addMenuItem(formatMenu, "Lower Case", "Ctrl+Shift+L", e -> changeCase(false));

        // Theme Menu
        JMenu themeMenu = new JMenu("Theme");
        addMenuItem(themeMenu, "Light Mode", null, e -> switchTheme(new FlatLightLaf()));
        addMenuItem(themeMenu, "Dark Mode", null, e -> switchTheme(new FlatDarkLaf()));

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(formatMenu);
        menuBar.add(themeMenu);
        setJMenuBar(menuBar);
    }
    private void switchTheme(LookAndFeel laf) {
        try {
            UIManager.setLookAndFeel(laf);
            SwingUtilities.updateComponentTreeUI(this);
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
    }


    private void setupToolbars() {
        JToolBar mainToolbar = new JToolBar();
        mainToolbar.setFloatable(false);

        // Font controls
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fonts = ge.getAvailableFontFamilyNames();
        fontSelector = new JComboBox<>(fonts);
        fontSelector.setSelectedItem("Monospaced");
        fontSelector.addActionListener(e -> updateFont());
        fontSelector.setToolTipText("Select Font");

        Integer[] sizes = {8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 36, 48, 72};
        fontSizeSelector = new JComboBox<>(sizes);
        fontSizeSelector.setSelectedItem(14);
        fontSizeSelector.addActionListener(e -> updateFont());
        fontSizeSelector.setToolTipText("Select Font Size");

        mainToolbar.add(fontSelector);
        mainToolbar.add(fontSizeSelector);

        add(mainToolbar, BorderLayout.NORTH);

        // Shape toolbar
        shapeToolbar = new JToolBar(JToolBar.VERTICAL);
        shapeToolbar.setFloatable(false);

        String[] shapes = {"Rectangle", "Oval", "Line", "Triangle", "Pentagon"};
        for (String shape : shapes) {
            JButton shapeButton = new JButton(shape);
            shapeButton.addActionListener(e -> drawingCanvas.setCurrentShape(shape));
            shapeButton.setToolTipText("Draw " + shape);
            shapeToolbar.add(shapeButton);
        }

        JButton colorButton = new JButton("Color");
        colorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Choose Shape Color", currentColor);
            if (newColor != null) {
                currentColor = newColor;
                drawingCanvas.setCurrentColor(currentColor);
            }
        });
        colorButton.setToolTipText("Choose Shape Color");
        shapeToolbar.add(colorButton);

        canvasPanel.add(shapeToolbar, BorderLayout.WEST);
    }

    private void setupStatusBar() {
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void updateStatusBar() {
        int caretPosition = textArea.getCaretPosition();
        int lineNumber = 0, columnNumber = 0;
        try {
            lineNumber = textArea.getLineOfOffset(caretPosition);
            columnNumber = caretPosition - textArea.getLineStartOffset(lineNumber);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        statusLabel.setText("Line: " + (lineNumber + 1) + " Column: " + (columnNumber + 1));
    }

    private void setupKeyboardShortcuts() {
        InputMap im = textArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = textArea.getActionMap();

        // Add custom shortcuts
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "find");
        am.put("find", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                showFindDialog();
            }
        });
    }

    private void setupCanvasTab() {
        canvasPanel = new JPanel(new BorderLayout());
        drawingCanvas = new EnhancedCanvas();
        canvasPanel.add(drawingCanvas, BorderLayout.CENTER);
        tabbedPane.addTab("Canvas", canvasPanel);
    }

    private void showFindDialog() {
        JDialog dialog = new JDialog(this, "Find and Replace", true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JTextField findField = new JTextField(20);
        JTextField replaceField = new JTextField(20);

        gbc.gridx = 0;
        gbc.gridy = 0;
        dialog.add(new JLabel("Find: "), gbc);

        gbc.gridx = 1;
        dialog.add(findField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        dialog.add(new JLabel("Replace: "), gbc);

        gbc.gridx = 1;
        dialog.add(replaceField, gbc);

        JPanel buttonPanel = new JPanel();
        JButton findButton = new JButton("Find");
        JButton replaceButton = new JButton("Replace");
        JButton replaceAllButton = new JButton("Replace All");
        JButton countButton = new JButton("Count");

        findButton.addActionListener(e -> findText(findField.getText()));
        replaceButton.addActionListener(e -> replaceText(findField.getText(), replaceField.getText()));
        replaceAllButton.addActionListener(e -> replaceAllText(findField.getText(), replaceField.getText()));
        countButton.addActionListener(e -> countWords());

        buttonPanel.add(findButton);
        buttonPanel.add(replaceButton);
        buttonPanel.add(replaceAllButton);
        buttonPanel.add(countButton);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        dialog.add(buttonPanel, gbc);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void findText(String text) {
        String content = textArea.getText();
        int index = content.indexOf(text, textArea.getCaretPosition());
        if (index >= 0) {
            textArea.setSelectionStart(index);
            textArea.setSelectionEnd(index + text.length());
            textArea.requestFocus();
        } else {
            JOptionPane.showMessageDialog(this, "Text not found");
        }
    }

    private void replaceText(String findText, String replaceText) {
        if (textArea.getSelectedText() != null &&
                textArea.getSelectedText().equals(findText)) {
            textArea.replaceSelection(replaceText);
        }
        findText(findText);
    }

    private void replaceAllText(String findText, String replaceText) {
        String content = textArea.getText();
        content = content.replace(findText, replaceText);
        textArea.setText(content);
    }

    private void countWords() {
        String content = textArea.getText().trim();
        String[] words = content.split("\\s+");
        JOptionPane.showMessageDialog(this,
                "Word count: " + words.length + "\n" +
                        "Character count: " + content.length());
    }

    private void changeCase(boolean upper) {
        String selectedText = textArea.getSelectedText();
        if (selectedText != null) {
            textArea.replaceSelection(upper ?
                    selectedText.toUpperCase() :
                    selectedText.toLowerCase());
        }
    }



    private void updateFont() {
        String fontName = (String) fontSelector.getSelectedItem();
        int fontSize = (int) fontSizeSelector.getSelectedItem();
        Font currentFont = textArea.getFont();
        textArea.setFont(new Font(fontName, currentFont.getStyle(), fontSize));
    }

    private void toggleBold() {
        Font currentFont = textArea.getFont();
        textArea.setFont(currentFont.deriveFont(
                currentFont.getStyle() ^ Font.BOLD));
    }

    private void toggleItalic() {
        Font currentFont = textArea.getFont();
        textArea.setFont(currentFont.deriveFont(
                currentFont.getStyle() ^ Font.ITALIC));
    }

    private void addMenuItem(JMenu menu, String text, String accelerator,
                             ActionListener listener) {
        JMenuItem item = new JMenuItem(text);
        if (accelerator != null) {
            item.setAccelerator(KeyStroke.getKeyStroke(accelerator));
        }
        item.addActionListener(listener);
        menu.add(item);
    }

    private void newFile() {
        textArea.setText("");
        currentFile = null;
        setTitle("Enhanced Text Editor - New File");
    }

    private void saveFile() {
        if (currentFile == null) {
            saveFileAs();
        } else {
            try (FileWriter writer = new FileWriter(currentFile)) {
                writer.write(textArea.getText());
                statusLabel.setText("File saved: " + currentFile.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Error saving file: " + ex.getMessage());
            }
        }
    }

    private void saveFileAs() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(
                new FileNameExtensionFilter("Text Files", "txt"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            saveFile();
        }
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(
                new FileNameExtensionFilter("Text Files", "txt"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.FileReader(fileChooser.getSelectedFile()))) {
                textArea.read(reader, null);
                currentFile = fileChooser.getSelectedFile();
                setTitle("Enhanced Text Editor - " + currentFile.getName());
            } catch (java.io.IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Error opening file: " + ex.getMessage());
            }
        }
    }
        public static void main(String[] args) {
            FlatDarkLaf.setup();
            SwingUtilities.invokeLater(SwingTextEditor::new);
        }
    private void showReplaceDialog() {
        JDialog dialog = new JDialog(this, "Replace", true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JTextField findField = new JTextField(20);
        JTextField replaceField = new JTextField(20);

        gbc.gridx = 0;
        gbc.gridy = 0;
        dialog.add(new JLabel("Find: "), gbc);

        gbc.gridx = 1;
        dialog.add(findField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        dialog.add(new JLabel("Replace: "), gbc);

        gbc.gridx = 1;
        dialog.add(replaceField, gbc);

        JPanel buttonPanel = new JPanel();
        JButton replaceButton = new JButton("Replace");
        JButton replaceAllButton = new JButton("Replace All");

        replaceButton.addActionListener(e -> replaceText(findField.getText(), replaceField.getText()));
        replaceAllButton.addActionListener(e -> replaceAllText(findField.getText(), replaceField.getText()));

        buttonPanel.add(replaceButton);
        buttonPanel.add(replaceAllButton);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        dialog.add(buttonPanel, gbc);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    class EnhancedCanvas extends JPanel {
        private ArrayList<DrawingShape> shapes = new ArrayList<>();
        private DrawingShape currentShape;
        private Point startPoint;
        private String shapeType = "Rectangle";
        private Color currentColor = Color.BLUE;
        private boolean isResizing = false;
        private DrawingShape selectedShape;
        private Rectangle resizeHandle;

        public EnhancedCanvas() {
            setBackground(Color.WHITE);
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    handleMousePressed(e);
                }

                public void mouseReleased(MouseEvent e) {
                    handleMouseReleased(e);
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                public void mouseDragged(MouseEvent e) {
                    handleMouseDragged(e);
                }
            });
        }

        private void handleMousePressed(MouseEvent e) {
            startPoint = e.getPoint();
            selectedShape = getShapeAtPoint(startPoint);

            if (selectedShape != null) {
                isResizing = isPointNearResizeHandle(startPoint);
            } else {
                currentShape = createNewShape(startPoint);
                shapes.add(currentShape);
            }
        }

        private void handleMouseReleased(MouseEvent e) {
            if (isResizing) {
                isResizing = false;
            } else {
                currentShape = null;
            }
            repaint();
        }

        private void handleMouseDragged(MouseEvent e) {
            if (currentShape != null) {
                updateShapeOnDrag(e);
            } else if (isResizing && selectedShape != null) {
                resizeShape(e);
            }
            repaint();
        }

        private void updateShapeOnDrag(MouseEvent e) {
            Point endPoint = e.getPoint();
            if ("Rectangle".equals(shapeType) || "Oval".equals(shapeType) || "Triangle".equals(shapeType) || "Pentagon".equals(shapeType)) {
                int width = Math.abs(endPoint.x - startPoint.x);
                int height = Math.abs(endPoint.y - startPoint.y);
                int x = Math.min(startPoint.x, endPoint.x);
                int y = Math.min(startPoint.y, endPoint.y);
                currentShape.setBounds(x, y, width, height);
            } else if ("Line".equals(shapeType)) {
                currentShape.setEndPoint(endPoint);
            }
        }

        private void resizeShape(MouseEvent e) {
            Point endPoint = e.getPoint();
            if (selectedShape != null && resizeHandle != null) {
                int width = endPoint.x - selectedShape.getX();
                int height = endPoint.y - selectedShape.getY();
                selectedShape.setBounds(selectedShape.getX(), selectedShape.getY(), width, height);
            }
        }

        private DrawingShape createNewShape(Point startPoint) {
            return switch (shapeType) {
                case "Rectangle" -> new DrawingRectangle(startPoint.x, startPoint.y, 0, 0, currentColor);
                case "Oval" -> new DrawingOval(startPoint.x, startPoint.y, 0, 0, currentColor);
                case "Line" -> new DrawingLine(startPoint.x, startPoint.y, startPoint.x, startPoint.y, currentColor);
                case "Triangle" -> new DrawingTriangle(startPoint.x, startPoint.y, 0, 0, currentColor);
                case "Pentagon" -> new DrawingPentagon(startPoint.x, startPoint.y, 0, 0, currentColor);
                default -> null;
            };
        }

        private DrawingShape getShapeAtPoint(Point point) {
            for (DrawingShape shape : shapes) {
                if (shape.contains(point)) {
                    return shape;
                }
            }
            return null;
        }

        private boolean isPointNearResizeHandle(Point point) {
            if (selectedShape != null) {
                resizeHandle = selectedShape.getResizeHandle();
                return resizeHandle.contains(point);
            }
            return false;
        }

        public void setCurrentShape(String shapeType) {
            this.shapeType = shapeType;
        }

        public void setCurrentColor(Color color) {
            this.currentColor = color;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            for (DrawingShape shape : shapes) {
                shape.draw(g);
            }
        }
    }

    abstract class DrawingShape {
        protected int x, y, width, height;
        protected Color color;

        public DrawingShape(int x, int y, int width, int height, Color color) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.color = color;
        }

        public abstract void draw(Graphics g);

        public abstract boolean contains(Point point);

        public abstract void setBounds(int x, int y, int width, int height);

        public abstract Rectangle getResizeHandle();

        public abstract void setEndPoint(Point point);

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    class DrawingRectangle extends DrawingShape {
        public DrawingRectangle(int x, int y, int width, int height, Color color) {
            super(x, y, width, height, color);
        }

        @Override
        public void draw(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(x, y, width, height);
        }

        @Override
        public boolean contains(Point point) {
            Rectangle bounds = new Rectangle(x, y, width, height);
            return bounds.contains(point);
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            this.x = Math.min(x, x + width);
            this.y = Math.min(y, y + height);
            this.width = Math.abs(width);
            this.height = Math.abs(height);
        }

        @Override
        public Rectangle getResizeHandle() {
            return new Rectangle(x + width - 5, y + height - 5, 10, 10);
        }

        @Override
        public void setEndPoint(Point point) {
            width = point.x - x;
            height = point.y - y;
        }
    }

    class DrawingOval extends DrawingShape {
        public DrawingOval(int x, int y, int width, int height, Color color) {
            super(x, y, width, height, color);
        }

        @Override
        public void draw(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(x, y, width, height);
        }

        @Override
        public boolean contains(Point point) {
            Ellipse2D.Float oval = new Ellipse2D.Float(x, y, width, height);
            return oval.contains(point);
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            this.x = Math.min(x, x + width);
            this.y = Math.min(y, y + height);
            this.width = Math.abs(width);
            this.height = Math.abs(height);
        }

        @Override
        public Rectangle getResizeHandle() {
            return new Rectangle(x + width - 5, y + height - 5, 10, 10);
        }

        @Override
        public void setEndPoint(Point point) {
            width = point.x - x;
            height = point.y - y;
        }
    }

    class DrawingLine extends DrawingShape {
        private int x2, y2;

        public DrawingLine(int x1, int y1, int x2, int y2, Color color) {
            super(x1, y1, Math.abs(x2 - x1), Math.abs(y2 - y1), color);
            this.x2 = x2;
            this.y2 = y2;
        }

        @Override
        public void draw(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(x, y, x2, y2);
        }

        @Override
        public boolean contains(Point point) {
            Line2D.Float line = new Line2D.Float(x, y, x2, y2);
            return line.ptLineDist(point) < 5;
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            this.x2 = x + width;
            this.y2 = y + height;
        }

        @Override
        public Rectangle getResizeHandle() {
            return new Rectangle(x2 - 5, y2 - 5, 10, 10);
        }

        @Override
        public void setEndPoint(Point point) {
            x2 = point.x;
            y2 = point.y;
        }
    }

    class DrawingTriangle extends DrawingShape {
        private int[] xPoints = new int[3];
        private int[] yPoints = new int[3];

        public DrawingTriangle(int x, int y, int width, int height, Color color) {
            super(x, y, width, height, color);
            updatePoints();
        }

        private void updatePoints() {
            xPoints[0] = x + width / 2;
            xPoints[1] = x;
            xPoints[2] = x + width;
            yPoints[0] = y;
            yPoints[1] = y + height;
            yPoints[2] = y + height;
        }

        @Override
        public void draw(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(2));
            updatePoints();
            g2d.drawPolygon(xPoints, yPoints, 3);
        }

        @Override
        public boolean contains(Point point) {
            Polygon triangle = new Polygon(xPoints, yPoints, 3);
            return triangle.contains(point);
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            this.x = Math.min(x, x + width);
            this.y = Math.min(y, y + height);
            this.width = Math.abs(width);
            this.height = Math.abs(height);
            updatePoints();
        }

        @Override
        public Rectangle getResizeHandle() {
            return new Rectangle(x + width - 5, y + height - 5, 10, 10);
        }

        @Override
        public void setEndPoint(Point point) {
            width = point.x - x;
            height = point.y - y;
            updatePoints();
        }
    }

    class DrawingPentagon extends DrawingShape {
        private int[] xPoints = new int[5];
        private int[] yPoints = new int[5];

        public DrawingPentagon(int x, int y, int width, int height, Color color) {
            super(x, y, width, height, color);
            updatePoints();
        }

        private void updatePoints() {
            int centerX = x + width / 2;
            int centerY = y + height / 2;
            double radius = Math.min(width, height) / 2.0;

            for (int i = 0; i < 5; i++) {
                double angle = 2 * Math.PI * i / 5 - Math.PI / 2;
                xPoints[i] = (int) (centerX + radius * Math.cos(angle));
                yPoints[i] = (int) (centerY + radius * Math.sin(angle));
            }
        }

        @Override
        public void draw(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(2));
            updatePoints();
            g2d.drawPolygon(xPoints, yPoints, 5);
        }

        @Override
        public boolean contains(Point point) {
            Polygon pentagon = new Polygon(xPoints, yPoints, 5);
            return pentagon.contains(point);
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            this.x = Math.min(x, x + width);
            this.y = Math.min(y, y + height);
            this.width = Math.abs(width);
            this.height = Math.abs(height);
            updatePoints();
        }

        @Override
        public Rectangle getResizeHandle() {
            return new Rectangle(x + width - 5, y + height - 5, 10, 10);
        }

        @Override
        public void setEndPoint(Point point) {
            width = point.x - x;
            height = point.y - y;
            updatePoints();
        }
    }
}