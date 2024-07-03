package phrasekey;
/**
 * PhraseKeyApp
 * 
 * Kameron Dangleben 7/1/24.
 * 
 * This application is designed to allow users to assign phrases
 * to keywords. Primarily focused on quick job applications or 
 * website sign ups.
 * 
 * FEATURES:
 * - You may add and delete hotkeys. If multiple phrases are assigned to a key, it will open a dialog.
 * - Saving and loading configuration files with the .pk extension.
 * - Custom theme colors.
 * - Auto-initialization.
 * - Unsaved changes warning
 * 
 * This application uses FlatLaf for the modern look and feel and JNativeHook for 
 * global hotkey support.
 * 
 * Developed by Kameron Dangleben. */

// FlatLaf and JNativeHook imports
import com.formdev.flatlaf.FlatLightLaf;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
//Other Imports
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class PhraseKeyApp extends JFrame {
    private JTextField phraseInput;
    private JTable hotkeyTable;
    private HotkeyTableModel tableModel;

public PhraseKeyApp() {
    setTitle("PhraseKey");
    setSize(400, 300);
    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Prevent automatic closing
    setAlwaysOnTop(true);
    setLocationRelativeTo(null);

    // FlatLaf theme
    FlatLightLaf.install();
   // Set application icon
    try {
        setIconImage(ImageIO.read(getClass().getResource("/pk.png")));
    } catch (IOException e) {
        e.printStackTrace();
    }

    // Initialize BorderLayout
    setLayout(new BorderLayout());

    // Phrase input panel
    JPanel inputPanel = new JPanel(new BorderLayout());
    phraseInput = new JTextField();
    JButton addButton = new JButton("Add Phrase");

    inputPanel.add(phraseInput, BorderLayout.CENTER);
    inputPanel.add(addButton, BorderLayout.EAST);

    add(inputPanel, BorderLayout.NORTH);

    // Hotkey table model - Left column is the key, right is the phrase
    tableModel = new HotkeyTableModel();
    hotkeyTable = new JTable(tableModel);

    add(new JScrollPane(hotkeyTable), BorderLayout.CENTER);

    // Add button action listener
    addButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            addPhrase();
        }
    });

    // Add popup for deleting phrases
    JPopupMenu contextMenu = new JPopupMenu();
    JMenuItem deleteItem = new JMenuItem("Delete");
    deleteItem.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            int selectedRow = hotkeyTable.getSelectedRow();
            if (selectedRow != -1) {
                tableModel.removePhrase(selectedRow);
            }
        }
    });
    contextMenu.add(deleteItem);

    hotkeyTable.setComponentPopupMenu(contextMenu);

    // Theme Button
    JButton themeButton = new JButton("Choose Theme");
    themeButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            chooseTheme();
        }
    });

    JButton saveButton = new JButton("Save");
    saveButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            saveConfigurations();
        }
    });

    JButton loadButton = new JButton("Load");
    loadButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            loadConfigurations();
        }
    });

    // Add buttons to bottom panel
    JPanel configPanel = new JPanel(new GridLayout(1, 3, 10, 10)); // 1 row, 3 columns, with gaps
    configPanel.add(themeButton);
    configPanel.add(saveButton);
    configPanel.add(loadButton);

    // Add bottom buttom panel to the main frame
    add(configPanel, BorderLayout.SOUTH);

    // Register global key listener
    try {
        GlobalScreen.registerNativeHook();
    } catch (Exception ex) {
        ex.printStackTrace();
    }
    GlobalScreen.addNativeKeyListener(new GlobalKeyListener());

    // Default theme
    applyTheme(Color.WHITE);

    // Load last configuration upon startup IF it exists
    loadLastConfiguration();

    // Check for unsaved changes
    addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
            if (hasUnsavedChanges()) {
                int option = JOptionPane.showConfirmDialog(
                    PhraseKeyApp.this,
                    "You have unsaved changes. Do you really want to exit?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                if (option == JOptionPane.YES_OPTION) {
                    dispose();
                }
            } else {
                dispose();
            }
        }
    });
}

    // Load the last configuration
    private void loadLastConfiguration() {
        File configFile = new File("lastConfigPath.txt");
        if (configFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                String lastConfigPath = reader.readLine();
                if (lastConfigPath != null) {
                    loadConfiguration(new File(lastConfigPath));
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void chooseTheme() {
        Color selectedColor = JColorChooser.showDialog(this, "Choose a Theme Color", getBackground());
        if (selectedColor != null) {
            applyTheme(selectedColor);
        }
    }

    private void applyTheme(Color color) {
        Color lighterColor = ColorUtils.lightenColor(color, 0.2);
        boolean isDark = ColorUtils.isDark(color);

        getContentPane().setBackground(color);
        phraseInput.setBackground(lighterColor);
        phraseInput.setForeground(isDark ? Color.WHITE : Color.BLACK);
        hotkeyTable.setBackground(lighterColor);
        hotkeyTable.setForeground(isDark ? Color.WHITE : Color.BLACK);
        hotkeyTable.setSelectionBackground(color.darker());
        hotkeyTable.setSelectionForeground(Color.WHITE);

        JTableHeader tableHeader = hotkeyTable.getTableHeader();
        tableHeader.setBackground(color.darker());
        tableHeader.setForeground(Color.WHITE);

        //Buttons and bottom panel
        Component[] components = getContentPane().getComponents();
        for (Component component : components) {
            if (component instanceof JPanel) {
                Component[] panelComponents = ((JPanel) component).getComponents();
                for (Component panelComponent : panelComponents) {
                    if (panelComponent instanceof JButton) {
                        panelComponent.setBackground(color.darker());
                        panelComponent.setForeground(Color.WHITE);
                    }
                }
            }
        }
    }
    private boolean changesMade = false;

    private void saveConfigurations() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".pk");
            }

            @Override
            public String getDescription() {
                return "PhraseKey Configuration Files (*.pk)";
            }
        });
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".pk")) {
                file = new File(file.getAbsolutePath() + ".pk");
            }
            try (FileOutputStream fos = new FileOutputStream(file);
                 ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                oos.writeObject(tableModel.getData());
                oos.writeObject(getContentPane().getBackground()); // Save the theme color
                saveLastConfigPath(file.getAbsolutePath());
                changesMade = false;
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error saving configurations: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadConfigurations() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".pk");
            }

            @Override
            public String getDescription() {
                return "PhraseKey Configuration Files (*.pk)";
            }
        });
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadConfiguration(fileChooser.getSelectedFile());
        }
    }


    private void loadConfiguration(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            List<String[]> data = (List<String[]>) ois.readObject();
            Color themeColor = (Color) ois.readObject(); // Load theme color
            tableModel.setData(data);
            applyTheme(themeColor); 
            saveLastConfigPath(file.getAbsolutePath());
            changesMade = false;
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading configurations: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveLastConfigPath(String path) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("lastConfigPath.txt"))) {
            writer.write(path);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private boolean hasUnsavedChanges() {
        return changesMade;
    }

    //Action after pressing add phrase button
    public void addPhrase() {
        String phrase = phraseInput.getText();
        if (phrase.isEmpty()) {
            JOptionPane.showMessageDialog(this, "You must first enter a phrase!", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            HotkeySelectionDialog dialog = new HotkeySelectionDialog(this, phrase, tableModel, getContentPane().getBackground());
            dialog.setVisible(true);
            phraseInput.setText("");
            changesMade = true;
        }
    }


    private int getKeyCode(String hotkey) {
        return KeyEvent.VK_F1 + Integer.parseInt(hotkey.substring(1)) - 1;
    }

    private void pastePhrase(String phrase) {
        try {
            Robot robot = new Robot();
            for (char c : phrase.toCharArray()) {
                int keyCode = KeyEvent.getExtendedKeyCodeForChar(c);
                if (KeyEvent.CHAR_UNDEFINED == keyCode) {
                    continue; // Skip characters without a key code
                }
                robot.keyPress(keyCode);
                robot.keyRelease(keyCode);
            }
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }


    private class GlobalKeyListener implements NativeKeyListener {
        @Override
        public void nativeKeyPressed(NativeKeyEvent e) {
            List<String> phrases = new ArrayList<>();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String hotkey = (String) tableModel.getValueAt(i, 0);
                String phrase = (String) tableModel.getValueAt(i, 1);

                if (hotkey != null && !hotkey.isEmpty() && e.getKeyCode() == getNativeKeyCode(hotkey)) {
                    phrases.add(phrase);
                }
            }

            if (phrases.size() == 1) {
                pastePhrase(phrases.get(0));
            } else if (phrases.size() > 1) {
                showPhraseSelectionPopup(phrases, MouseInfo.getPointerInfo().getLocation());
            }
        }

        @Override
        public void nativeKeyReleased(NativeKeyEvent e) {
        }

        @Override
        public void nativeKeyTyped(NativeKeyEvent e) {
        }

        private int getNativeKeyCode(String hotkey) {
            return NativeKeyEvent.VC_F1 + Integer.parseInt(hotkey.substring(1)) - 1;
        }
    }

    private void showPhraseSelectionPopup(List<String> phrases, Point location) {
        JPopupMenu popupMenu = new JPopupMenu();
        Color backgroundColor = ColorUtils.lightenColor(getContentPane().getBackground(), 0.2);
        Color textColor = ColorUtils.isDark(getContentPane().getBackground()) ? Color.WHITE : Color.BLACK;

        popupMenu.setBackground(backgroundColor); // Set the background color of the popup menu

        for (String phrase : phrases) {
            JMenuItem item = new JMenuItem(phrase);
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    pastePhrase(phrase);
                    popupMenu.setVisible(false);
                }
            });
            item.setBackground(backgroundColor);
            item.setForeground(textColor);
            popupMenu.add(item);
        }

        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                popupMenu.setVisible(false);
            }
        });

        popupMenu.setFocusable(true);
        popupMenu.show(this, location.x - getLocationOnScreen().x, location.y - getLocationOnScreen().y);

        popupMenu.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                popupMenu.setVisible(false);
            }
        });

        // Add mouse listener to close the popup when clicking outside
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (event.getID() == MouseEvent.MOUSE_CLICKED) {
                    MouseEvent me = (MouseEvent) event;
                    if (!SwingUtilities.isDescendingFrom(me.getComponent(), popupMenu)) {
                        popupMenu.setVisible(false);
                        Toolkit.getDefaultToolkit().removeAWTEventListener(this);
                    }
                }
            }
        }, AWTEvent.MOUSE_EVENT_MASK);
    }

    public static void main(String[] args) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    new PhraseKeyApp().setVisible(true);
                }
            });
        }
    }

class HotkeyTableModel extends AbstractTableModel {
    private final String[] columnNames = {"Hotkey", "Phrase"};
    private final List<String[]> data = new ArrayList<>();

    public void addPhrase(String hotkey, String phrase) {
        data.add(new String[]{hotkey, phrase});
        fireTableDataChanged();
    }

    public void removePhrase(int index) {
        data.remove(index);
        fireTableDataChanged();
    }

    public List<String[]> getData() {
        return data;
    }

    public void setData(List<String[]> data) {
        this.data.clear();
        this.data.addAll(data);
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return data.get(rowIndex)[columnIndex];
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
}

class HotkeySelectionDialog extends JDialog {
    private JComboBox<String> hotkeyComboBox;
    private String phrase;
    private HotkeyTableModel tableModel;

    public HotkeySelectionDialog(Frame owner, String phrase, HotkeyTableModel tableModel, Color color) {
        super(owner, "Select Hotkey", true);
        this.phrase = phrase;
        this.tableModel = tableModel;

        setLayout(new BorderLayout());
        setSize(300, 150);
        setLocationRelativeTo(owner);

        hotkeyComboBox = new JComboBox<>();
        for (int i = 1; i <= 12; i++) {
            hotkeyComboBox.addItem("F" + i);
        }

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveHotkey();
            }
        });

        add(hotkeyComboBox, BorderLayout.CENTER);
        add(saveButton, BorderLayout.SOUTH);

        applyTheme(color);

        addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {}

            @Override
            public void windowLostFocus(WindowEvent e) {
                setVisible(false);
            }
        });
    }

    private void saveHotkey() {
        String hotkey = (String) hotkeyComboBox.getSelectedItem();
        tableModel.addPhrase(hotkey, phrase);
        setVisible(false);
        dispose();
    }

    private void applyTheme(Color color) {
        Color lighterColor = ColorUtils.lightenColor(color, 0.2);
        boolean isDark = ColorUtils.isDark(color);
        getContentPane().setBackground(color);
        hotkeyComboBox.setBackground(lighterColor);
        hotkeyComboBox.setForeground(isDark ? Color.BLACK : Color.WHITE);
        JButton saveButton = (JButton) getContentPane().getComponent(1);
        saveButton.setBackground(color.darker());
        saveButton.setForeground(Color.WHITE);
    }
}

    class ColorUtils {
    public static Color lightenColor(Color color, double percentage) {
        int r = color.getRed() + (int) ((255 - color.getRed()) * percentage);
        int g = color.getGreen() + (int) ((255 - color.getGreen()) * percentage);
        int b = color.getBlue() + (int) ((255 - color.getBlue()) * percentage);
        return new Color(r, g, b);
    }

    public static boolean isDark(Color color) {
        double darkness = 1 - (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255;
        return darkness >= 0.5;
    }
}
