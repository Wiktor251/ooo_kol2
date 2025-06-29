package org.example.powtkol2.server;

import org.example.powtkol2.Dot;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 5001;
    private static final Set<PrintWriter> clientWriters = new HashSet<>();
    private final ExecutorService pool = Executors.newFixedThreadPool(50);
    private java.sql.Connection connection;

    public void start() {
        System.out.println("Server is running on port " + PORT);
        try (ServerSocket listener = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = listener.accept();
                pool.execute(new ClientThread(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            pool.shutdown();
        }
    }

    public void broadcast(String message) {
        Dot dot = Dot.fromMessage(message);
        saveDot(dot);
        synchronized (clientWriters) {
            for (PrintWriter writer : clientWriters) {
                writer.println(message);
            }
        }
    }

    private class ClientThread implements Runnable {
        private final Socket socket;

        public ClientThread(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            System.out.println("New client connected: " + socket);
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                synchronized (clientWriters) {
                    clientWriters.add(out);
                }
                List<Dot> savedDots = getSavedDots();
                for (Dot dot : savedDots) {
                    out.println(dot.toMessage());
                }

                String message;
                while ((message = in.readLine()) != null) {
                    broadcast(message);
                }
            } catch (IOException e) {
                System.err.println("Client connection error: " + e.getMessage());
            } finally {
                synchronized (clientWriters) {
                    clientWriters.removeIf(writer -> writer.checkError());
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Socket close error: " + e.getMessage());
                }
            }
        }
    }
    public void connect(String dbPath) throws SQLException {
        try {
            String url = "jdbc:sqlite:" + dbPath;
            this.connection = DriverManager.getConnection(url);
            System.out.println("Database connection established.");
            String ddl = """
                CREATE TABLE IF NOT EXISTS dot (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  x INTEGER NOT NULL,
                  y INTEGER NOT NULL,
                  color TEXT NOT NULL,
                  radius INTEGER NOT NULL
                );
                """;
            connection.createStatement().executeUpdate(ddl);
        } catch (SQLException e) {
            System.out.println("An error occurred while connecting to the DB " + e.getMessage());
        }
    }
    public void saveDot(Dot dot) {
        String sql = "INSERT INTO dot (x, y, color, radius) VALUES (?, ?, ?, ?)";
        try (var pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, (int) dot.x());
            pstmt.setInt(2, (int) dot.y());
            pstmt.setString(3, dot.color().toString());
            pstmt.setInt(4, (int) dot.r());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving dot to DB: " + e.getMessage());
        }
    }

    public List<Dot> getSavedDots() {
        List<Dot> dots = new ArrayList<>();
        String sql = "SELECT x, y, color, radius FROM dot";
        try (var stmt = connection.createStatement();
             var rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double r = rs.getDouble("radius");
                var color = javafx.scene.paint.Color.valueOf(rs.getString("color"));
                dots.add(new Dot(x, y, color, r));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching dots: " + e.getMessage());
        }
        return dots;
    }
}