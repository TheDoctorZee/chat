import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {

  private static final int PORT = 19000;
  private static final String HOST = "localhost";

  private String name;
  private boolean clientRunning = true;

  private Socket socket;
  private BufferedReader input;
  private BufferedWriter output;
  private Scanner scanner;

  public Client() {
    this.scanner = new Scanner(System.in);
    try {
      socket = new Socket(HOST, PORT);
      input = new BufferedReader(
          new InputStreamReader(socket.getInputStream()));
      output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    Client client = new Client();
    client.runClient();
  }

  public void runClient() {
    System.out.println("Type your name: ");
    setName(scanner.nextLine());

    ExecutorService executorService = Executors.newFixedThreadPool(2);
    executorService.execute(new GetMessage(input));
    executorService.execute(new SendMessage(output));
    executorService.shutdown();
  }

  private void setName(String name) {
    this.name = name;
  }

  private void closeConnection() {
    try {
      clientRunning = false;
      output.close();
      input.close();
      socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private class GetMessage implements Runnable {

    private BufferedReader input;

    public GetMessage(BufferedReader input) {
      this.input = input;
    }

    @Override
    public void run() {
      while (clientRunning) {
        try {
          String msg = input.readLine();
          System.out.println(msg);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private class SendMessage implements Runnable{

    private BufferedWriter output;

    public SendMessage(BufferedWriter output) {
      this.output = output;
    }

    @Override
    public void run() {
      try {
        output.write(Client.this.name + " entered the chat" + System.lineSeparator());
        output.flush();
        while (clientRunning) {
          String msg = scanner.nextLine();
          output.write(Client.this.name + ">>> " + msg + System.lineSeparator());
          output.flush();
          if ("!exit".equals(msg)) {
            Client.this.closeConnection();
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}

