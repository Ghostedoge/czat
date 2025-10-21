import java.io.*;
import java.net.*;
import java.util.Map;

public class ClientHandler extends Thread {
    private Socket socket;
    private Map<String, PrintWriter> clients;
    private PrintWriter out;
    private String username;

    public ClientHandler(Socket socket, Map<String, PrintWriter> clients) {
        this.socket = socket;
        this.clients = clients;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ) {
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Podaj swój login:");
            username = in.readLine();
            synchronized (clients) {
                clients.put(username, out);
            }

            ChatServer.broadcast("[SERVER] " + username + " dołączył do czatu.", username);
            ChatServer.updateUserList();

            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("/msg")) {
                    String[] parts = message.split(" ", 3);
                    if (parts.length >= 3) {
                        String targetUser = parts[1];
                        String msg = parts[2];
                        PrintWriter targetOut = clients.get(targetUser);
                        if (targetOut != null) {
                            targetOut.println("[Prywatna od " + username + "]: " + msg);
                        } else {
                            out.println("[SERVER] Użytkownik " + targetUser + " nie jest dostępny.");
                        }
                    }
                } else {
                    ChatServer.broadcast(username + ": " + message, null);
                }
            }
        } catch (IOException e) {
            System.out.println("Klient " + username + " rozłączony.");
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
            synchronized (clients) {
                clients.remove(username);
            }
            ChatServer.broadcast("[SERVER] " + username + " opuścił czat.", username);
            ChatServer.updateUserList();
        }
    }
}
