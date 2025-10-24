import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 5555;
    private static Map<String, PrintWriter> clients = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) {
        System.out.println("Serwer wystartował na porcie " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket, clients).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void sendPrivateMessage(String fromUser, String toUser, String message) {
        synchronized (clients) {
            PrintWriter targetOut = clients.get(toUser);
            PrintWriter fromOut = clients.get(fromUser);

            if (targetOut != null) {
                targetOut.println("[PRIV od " + fromUser + "]: " + message);
            } else {
                if (fromOut != null) {
                    fromOut.println("[SERVER] Użytkownik " + toUser + " nie jest dostępny.");
                }
            }

            if (fromOut != null) {
                fromOut.println("[PRIV do " + toUser + "]: " + message);
            }
        }
    }


    public static void broadcast(String message, String excludeUser) {
        synchronized (clients) {
            for (Map.Entry<String, PrintWriter> entry : clients.entrySet()) {
                if (!entry.getKey().equals(excludeUser)) {
                    entry.getValue().println(message);
                }
            }
        }
    }

    public static void updateUserList() {
        String users = String.join(",", clients.keySet());
        synchronized (clients) {
            for (PrintWriter writer : clients.values()) {
                writer.println("/users " + users);
            }
        }
    }
}
