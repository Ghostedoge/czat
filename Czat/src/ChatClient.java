import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.ArrayList;

public class ChatClient extends JFrame {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private JTextPane chatPane = new JTextPane();
    private JTextField inputField = new JTextField();
    private ArrayList<String> messageHistory = new ArrayList<>();
    private int historyIndex = -1;

    private JButton sendButton = new JButton("Wyślij");
    private JList<String> userList = new JList<>();
    private DefaultListModel<String> listModel = new DefaultListModel<>();

    private String username;

    public ChatClient(String host, int port) {
        super("Czat sieciowy");

        chatPane.setEditable(false);
        userList.setModel(listModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list,
                                                        Object value,
                                                        int index,
                                                        boolean isSelected,
                                                        boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value.toString().equals(username)) {
                    setForeground(new Color(0, 102, 204));
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    setForeground(Color.BLACK);
                    setFont(getFont().deriveFont(Font.PLAIN));
                }
                return c;
            }
        });


        JScrollPane chatScroll = new JScrollPane(chatPane);
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(150, 0));

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(chatScroll, BorderLayout.CENTER);
        add(userScroll, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        inputField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (messageHistory.isEmpty()) return;

                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_UP) {
                    if (historyIndex > 0) {
                        historyIndex--;
                        inputField.setText(messageHistory.get(historyIndex));
                    }
                } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN) {
                    if (historyIndex < messageHistory.size() - 1) {
                        historyIndex++;
                        inputField.setText(messageHistory.get(historyIndex));
                    } else {
                        historyIndex = messageHistory.size();
                        inputField.setText("");
                    }
                }
            }
        });


        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);

        connectToServer(host, port);
        startMessageListener();
    }

    private void connectToServer(String host, int port) {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
                
        do {
            username = JOptionPane.showInputDialog(this, "Podaj swój login:");
            if (username == null) {
                System.exit(0);
            }
            username = username.trim();
            if (username.contains(" ")) {
                JOptionPane.showMessageDialog(this, "Login nie może zawierać spacji!");
                username = "";
            }
        } while (username.isEmpty());


        out.println(username);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Nie udało się połączyć z serwerem.");
            System.exit(0);
        }
    }

    private void startMessageListener() {
        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    if (msg.startsWith("/users")) {
                        String[] users = msg.substring(7).split(",");
                        SwingUtilities.invokeLater(() -> {
                            listModel.clear();
                            Arrays.stream(users).forEach(listModel::addElement);
                        });
                    } else {
                        if (msg.startsWith("[SERVER]")) {
                            appendMessage(msg + "\n", Color.RED);
                        } else if (msg.startsWith("[PRIV od")) {
                            appendMessage(msg + "\n", new Color(200, 200, 50));
                        } else if (msg.startsWith("[PRIV do")) {
                            appendMessage(msg + "\n", new Color(200, 200, 50));
                        } else {
                        appendMessage(msg + "\n", Color.BLACK);
                    }

                    }
                }
            } catch (IOException e) {
                appendMessage("Rozłączono z serwerem.\n", Color.GRAY);
            }
        }).start();
    }

    private void sendMessage() {
    String message = inputField.getText().trim();
    if (message.isEmpty()) return;
    
    messageHistory.add(message);
    historyIndex = messageHistory.size();

    if (message.startsWith("/msg ")) {
        String[] parts = message.split(" ", 3);
        if (parts.length < 3) {
            appendMessage("[SERVER] Użycie: /msg <nick> <wiadomość>\n", Color.RED);
            inputField.setText("");
            return;
        }

        String target = parts[1];
        String msgText = parts[2];
        out.println(message);
        appendMessage("[PRIV do " + target + "]: " + msgText + "\n", new Color(200, 200, 50));
        inputField.setText("");
        return;
    }

    String target = userList.getSelectedValue();
    if (target != null && !target.equals(username)) {
        out.println("/msg " + target + " " + message);
        appendMessage("[PRIV do " + target + "]: " + message + "\n", new Color(200, 200, 50));
    } else {
    out.println(message);

    if (!message.isEmpty()) {
        messageHistory.add(message);
        historyIndex = messageHistory.size();
    }

    inputField.setText("");
    }
}





    private void appendMessage(String msg, Color color) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = chatPane.getStyledDocument();
            Style style = chatPane.addStyle("Style", null);
            StyleConstants.setForeground(style, color);
            try {
                doc.insertString(doc.getLength(), msg, style);
                chatPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClient("127.0.0.1", 5555));
    }
}
