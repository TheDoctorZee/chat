import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Server {

  private static final int PORT = 19000;
  private static List<Connection> connectionList = new CopyOnWriteArrayList<>();
  private static boolean serverRunning = true;

  public static void main(String[] args) {
    Server server = new Server();
    server.runServer();
  }

  public void runServer() {
    System.out.println("-----Server started!-----");
    ExecutorService pool = Executors.newFixedThreadPool(4);
    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
      while (serverRunning) {
        pool.execute(new Connection(serverSocket.accept()));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.println("-----Server stopped!-----");
  }


  private class Connection implements Runnable {

    private Socket clientSocket;
    private BufferedReader input;
    private BufferedWriter output;
    private boolean isRunning;
    private Lock lock;


    Connection(Socket clientSocket) {
      this.clientSocket = clientSocket;
      try {
        input = new BufferedReader(
            new InputStreamReader(clientSocket.getInputStream()));
        output = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
      } catch (IOException e) {
        e.printStackTrace();
      }
      isRunning = true;
      lock = new ReentrantLock();
      connectionList.add(this);
    }

    @Override
    public void run() {
      while (isRunning) {
        try {
          String msg = input.readLine();
          if (Objects.nonNull(msg) && msg.contains("!exit")) {
            sendMsg(msg.substring(0, msg.indexOf('>')) + " left the chat!");
            closeConnection();
          } else {
            sendMsg(msg);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    public void getMsg(String msg) {
      lock.lock();
      try {
        output.write(msg + System.lineSeparator());
        output.flush();
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        lock.unlock();
      }
    }

    private void sendMsg(String msg) {
      for (Connection connection : connectionList) {
        if (connection.equals(this)) {
          continue;
        }
        connection.getMsg(msg);
      }
    }

    private void closeConnection() {
      try {
        isRunning = false;
        connectionList.remove(this);
        output.close();
        input.close();
        clientSocket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}


