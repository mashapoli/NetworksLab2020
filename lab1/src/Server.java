import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

class ServerSomthing extends Thread {

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;


    public ServerSomthing(Socket socket) throws IOException {
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        start();
    }
    @Override
    public void run() {
        String word;
        try {
            try {
                while (true) {
                    word = in.readLine();
                    if(word.equals("stop")) {
                        this.downService();
                        break;
                    }
                    String millisStr = word.substring(word.indexOf("<") + 1, word.indexOf("> "));
                    long millisLong = Long.parseLong(millisStr);
                    Date time = new Date(millisLong);
                    SimpleDateFormat dt1 = new SimpleDateFormat("HH:mm:ss.SSS");
                    String dtime = dt1.format(time);
                    System.out.println("Echoing: " + "<" + dtime + ">" +  word.substring(word.indexOf("> ") +1));
                    Thread.sleep(5000);
                    for (ServerSomthing vr : Server.serverList) {
                        vr.send(word);
                    }
                }
            } catch (NullPointerException ignored) {} catch (InterruptedException e) {
                e.printStackTrace();
            }


        } catch (IOException e) {
            this.downService();
        }
    }


    private void send(String msg) {
        try {
            out.write(msg + "\n");
            out.flush();
        } catch (IOException ignored) {}

    }


    private void downService() {
        try {
            if(!socket.isClosed()) {
                socket.close();
                in.close();
                out.close();
                for (ServerSomthing vr : Server.serverList) {
                    if(vr.equals(this)) vr.interrupt();
                    Server.serverList.remove(this);
                }
            }
        } catch (IOException ignored) {}
    }
}


public class Server {

    public static final int PORT = 1234;
    public static LinkedList<ServerSomthing> serverList = new LinkedList<>();



    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(PORT);
        System.out.println("Server Started");
        try {
            while (true) {
                Socket socket = server.accept();
                try {
                    serverList.add(new ServerSomthing(socket));
                } catch (IOException e) {
                    socket.close();
                }
            }
        } finally {
            server.close();
        }
    }
}