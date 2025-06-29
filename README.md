✅ Main.java – Główna klasa aplikacji JavaFX

        // Zadanie 4 + Zadanie 5 (inicjalizacja serwera z bazą danych)

        public class Main extends Application {
        @Override
            public void start(Stage primaryStage) throws Exception {
        // Wczytanie pliku FXML z interfejsem graficznym
        FXMLLoader loader = new FXMLLoader(getClass().getResource("plik.fxml"));

        // Tworzenie instancji serwera (Zadanie 2) i łączenie z bazą danych (Zadanie 5)
        Server server = new Server();
        server.connect("dots.db"); // Łączenie z lokalną bazą SQLite

        // Uruchomienie serwera w osobnym wątku, żeby GUI nie zamarzło
        new Thread(() -> server.start()).start();

        // Utworzenie klienta - wątku do komunikacji z serwerem (Zadanie 2, 4)
        ServerThread serverThread = new ServerThread();

        // Tworzenie kontrolera i przekazanie obiektów serwera i klienta (Zadanie 4)
        Controller controller = new Controller(server, serverThread);
        loader.setController(controller);

        // Ustawienia sceny JavaFX
        Parent root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setTitle("Circle Drawing App");
        primaryStage.setScene(scene);

        // Zamykanie aplikacji całkowicie przy zamknięciu okna
        primaryStage.setOnCloseRequest(event -> System.exit(0));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args); // Uruchomienie aplikacji JavaFX
    }
    }
    
✅ Dot.java – Rekord reprezentujący koło

    // Zadanie 3 – Reprezentacja danych okręgu i ich konwersja na/ze stringa

      public record Dot(double x, double y, Color color, double r) {

    // Statyczna metoda konwertująca dane okręgu na wiadomość tekstową
    public static String toMessage(double x, double y, Color color, double r){
        return x + " " + y + " " + r + " " + color;
    }

    // Odtwarza obiekt Dot z wiadomości tekstowej
    public static Dot fromMessage(String message){
        String[] arr = message.split(" ");
        double x = Double.parseDouble(arr[0]);
        double y = Double.parseDouble(arr[1]);
        double r = Double.parseDouble(arr[2]);
        Color color = Color.web(arr[3]);
        return new Dot(x, y, color, r);
    }

    // Instancyjna wersja toMessage – przydatna do broadcastu i zapisu do bazy
    public String toMessage() {
        return x + " " + y + " " + r + " " + color.toString();
    }
    }
✅ Controller.java – Sterowanie UI + przekaz do serwera

      // Zadania 1, 4 – Obsługa kliknięcia, pobierania danych z kontrolek i rysowania

    public class Controller {
      private final Server server;
      private final ServerThread serverThread;

    public Controller(Server server, ServerThread serverThread){
        this.server = server;
        this.serverThread = serverThread;

        // Ustawienie zachowania klienta po otrzymaniu danych (Zadanie 4)
        this.serverThread.setDotConsumer(dot -> {
            Platform.runLater(() -> drawDot(dot)); // Rysowanie na kanwie w głównym wątku
        });
    }

    @FXML private Slider radiusSlider;
    @FXML private Canvas canvas;
    @FXML private ColorPicker colorPicker;

    // Inicjalizacja pustej kanwy (biała)
    @FXML private void initialize() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    // Obsługa kliknięcia – wysyłanie danych do serwera (Zadanie 1 + 4)
    @FXML private void onMouseClicked(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();
        double r = radiusSlider.getValue();
        Color color = colorPicker.getValue();
        serverThread.send(x, y, r, color);
    }

    @FXML private void onStartServerClicked() {
        System.out.println("Start Server clicked");
    }

    @FXML private void onConnectClicked() {
        System.out.println("Connect clicked");
    }

    // Rysowanie pojedynczego okręgu (wywoływane przez klienta przy odbiorze danych)
    private void drawDot(Dot dot) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(dot.color());
        gc.fillOval(dot.x() - dot.r() / 2, dot.y() - dot.r() / 2, dot.r(), dot.r());
    }
    }

✅ ServerThread.java – Klient TCP łączący się z serwerem

    // Zadania 2, 4 – Komunikacja klienta z serwerem, odbiór i wysyłka danych

    public class ServerThread implements Runnable {
      private static final String server_address = "localhost";
      private static final int PORT = 5001;

    private Socket socket;
    private PrintWriter writer;
    private Consumer<Dot> dotConsumer;

    // Mutator – pozwala kontrolerowi ustawić co się dzieje z odebranym Dot-em (Zadanie 4)
    public void setDotConsumer(Consumer<Dot> consumer) {
        this.dotConsumer = consumer;
    }

    // Konstruktor – łączenie z serwerem i uruchomienie nasłuchiwania (w osobnym wątku)
    public ServerThread() {
        try {
            socket = new Socket(server_address, PORT);
            writer = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Connected");
            new Thread(this).start(); // startuje nasłuchiwanie odpowiedzi
        } catch (IOException e) {
            System.err.println("Unable to connect to server: " + e.getMessage());
        }
    }

    // Wysyłanie okręgu do serwera jako wiadomość (Zadanie 4)
    public void send(double x, double y, double r, Color color) {
        if (writer != null) {
            String message = Dot.toMessage(x, y, color, r);
            writer.println(message);
        } else {
            System.err.println("Output stream not initialized.");
        }
    }

    // Odbieranie wiadomości od serwera i przekazywanie ich do kontrolera (Zadanie 4)
    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );
            String line;
            while ((line = reader.readLine()) != null) {
                Dot dot = Dot.fromMessage(line);
                if (dotConsumer != null) {
                    dotConsumer.accept(dot);
                }
            }
        } catch (IOException e) {
            System.err.println("Error receiving message: " + e.getMessage());
        }
    }
    }

✅ Server.java – Serwer TCP + baza danych

      // Zadania 2, 5 – Komunikacja z klientami i zapisywanie do bazy danych

    public class Server {
      private static final int PORT = 5001;
      private static final Set<PrintWriter> clientWriters = new HashSet<>();
      private final ExecutorService pool = Executors.newFixedThreadPool(50);
      private java.sql.Connection connection;

    // Uruchomienie serwera – nasłuchiwanie na porcie i przyjmowanie klientów
    public void start() {
        System.out.println("Server is running on port " + PORT);
        try (ServerSocket listener = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = listener.accept();
                pool.execute(new ClientThread(clientSocket)); // każdy klient działa w osobnym wątku
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            pool.shutdown();
        }
    }

    // Wysyłanie wiadomości do wszystkich klientów + zapis do bazy (Zadanie 5)
    public void broadcast(String message) {
        Dot dot = Dot.fromMessage(message);
        saveDot(dot);
        synchronized (clientWriters) {
            for (PrintWriter writer : clientWriters) {
                writer.println(message);
            }
        }
    }

    // Wewnętrzna klasa obsługująca jednego klienta (Zadanie 2)
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

                // Wysłanie nowemu klientowi wszystkich dotychczasowych punktów (Zadanie 5)
                List<Dot> savedDots = getSavedDots();
                for (Dot dot : savedDots) {
                    out.println(dot.toMessage());
                }

                // Nasłuchiwanie na nowe wiadomości
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

    // Połączenie z bazą danych SQLite (Zadanie 5)
    public void connect(String dbPath) throws SQLException {
        try {
            String url = "jdbc:sqlite:" + dbPath;
            this.connection = DriverManager.getConnection(url);
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
            System.out.println("DB error: " + e.getMessage());
        }
    }

    // Zapisanie pojedynczego punktu do bazy danych (Zadanie 5)
    public void saveDot(Dot dot) {
        String sql = "INSERT INTO dot (x, y, color, radius) VALUES (?, ?, ?, ?)";
        try (var pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, (int) dot.x());
            pstmt.setInt(2, (int) dot.y());
            pstmt.setString(3, dot.color().toString());
            pstmt.setInt(4, (int) dot.r());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("DB insert error: " + e.getMessage());
        }
    }

    // Pobranie wszystkich dotów z bazy przy dołączeniu nowego klienta (Zadanie 5)
    public List<Dot> getSavedDots() {
        List<Dot> dots = new ArrayList<>();
        String sql = "SELECT x, y, color, radius FROM dot";
        try (var stmt = connection.createStatement();
             var rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double r = rs.getDouble("radius");
                Color color = Color.valueOf(rs.getString("color"));
                dots.add(new Dot(x, y, color, r));
            }
        } catch (SQLException e) {
            System.err.println("DB read error: " + e.getMessage());
        }
        return dots;
    }
    }
