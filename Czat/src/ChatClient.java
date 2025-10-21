import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.Arrays;

public class ChatClient extends JFrame {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private JTextArea chatArea = new JTextArea();
    private JTextField inputField = new JTextField();
    private JButton sendButton = new JButton("Wyślij");
    private JList<String> userList = new JList<>();
    private DefaultListModel<String> listModel = new DefaultListModel<>();

    private String username;

    public ChatClient(String host, int port) {
        super("Czat sieciowy");

        chatArea.setEditable(false);
        userList.setModel(listModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane chatScroll = new JScrollPane(chatArea);
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

            username = JOptionPane.showInputDialog(this, "Podaj swój login:");
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
                        chatArea.append(msg + "\n");
                    }
                }
            } catch (IOException e) {
                chatArea.append("Rozłączono z serwerem.\n");
            }
        }).start();
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (message.isEmpty()) return;

        String target = userList.getSelectedValue();
        if (target != null && !target.equals(username)) {
            out.println("/msg " + target + " " + message);
            chatArea.append("[Prywatna do " + target + "]: " + message + "\n");
        } else {
            out.println(message);
        }
        inputField.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClient("127.0.0.1", 5555));
    }
}
