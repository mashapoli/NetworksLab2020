import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

public class Main {
    static BufferedReader userInputReader = null;
    static BufferedReader inputUser = null;
    private static String userName = null;
    private static boolean writeMode = true;


    public static boolean processReadySet(Set readySet) throws Exception {
        Iterator iterator = readySet.iterator();
        while (iterator.hasNext()) {
            SelectionKey key = (SelectionKey) iterator.next();
            iterator.remove();
            if (key.isConnectable()) {
                boolean connected = processConnect(key);
                System.out.println("connected = " + connected);
                if (!connected) {
                    return true; // Exit
                }
            }
            System.out.println("writeMode = " + writeMode);
            if (!writeMode && key.isReadable()) {
                writeMode = true;
                String msg = processRead(key);
                System.out.println("msg = " + msg);
                int d1 = msg.indexOf("\0");
                int d2 = msg.indexOf("\0", d1+1);
                String millisStr = msg.substring(0, d1);
                String name = msg.substring(d1+1, d2);
                String text = msg.substring(d2+1);

                long millisLong = Long.parseLong(millisStr);
                Date time = new Date(millisLong);
                SimpleDateFormat dt1 = new SimpleDateFormat("HH:mm:ss.SSS");
                String dtime = dt1.format(time);
                msg =  "<" + dtime + "> "+ name + ": "+ text;
//                System.out.println("[Server]: " + dtime + name + text);
                System.out.println(msg);
                System.out.println("Thread.currentThread() = " + Thread.currentThread());
                System.out.flush();
            }
            if (writeMode && key.isWritable()) {
                writeMode = false;
                System.out.println("Thread.currentThread() = " + Thread.currentThread());
                System.out.print("msg:");
                String msg = userName + "\0" + userInputReader.readLine();
                long currentTime = System.currentTimeMillis();
//                msg = currentTime +"\0" + userName + "\0" + msg;
                msg = currentTime + "\0" + msg;

                if (msg.equalsIgnoreCase("bye")) {
                    return true; // Exit
                }
                SocketChannel sChannel = (SocketChannel) key.channel();
                ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
//                buffer = ByteBuffer.wrap(userName.getBytes());
                System.out.println("buffer = " + buffer);
                sChannel.write(buffer);
            }
        }
        return false; // Not done yet
    }

    public static boolean processConnect(SelectionKey key) throws Exception {
        SocketChannel channel = (SocketChannel) key.channel();
        System.out.print("Press Nick: ");
        userName = inputUser.readLine();
//        System.out.println(userName);
        while (channel.isConnectionPending()) {
            channel.finishConnect();
        }
        return true;
    }

    public static String processRead(SelectionKey key) throws Exception {
        SocketChannel sChannel = (SocketChannel) key.channel();
        System.out.println("sChannel = " + sChannel);
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        sChannel.read(buffer);
        buffer.flip();
        Charset charset = Charset.forName("UTF-8");
        CharsetDecoder decoder = charset.newDecoder();
        CharBuffer charBuffer = decoder.decode(buffer);
        String msg = charBuffer.toString();
        System.out.println("msgProcessRead = " + msg);
        return msg;
    }

    public static void main(String[] args) throws Exception {
        InetAddress serverIPAddress = InetAddress.getByName("localhost");
        int port = 1234;
        InetSocketAddress serverAddress = new InetSocketAddress(serverIPAddress, port);
        Selector selector = Selector.open();
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(serverAddress);
        int operations = SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE;
        channel.register(selector, operations);

        userInputReader = new BufferedReader(new InputStreamReader(System.in));
        inputUser = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            if (selector.select() > 0) {
                boolean doneStatus = processReadySet(selector.selectedKeys());
                if (doneStatus) {
                    break;
                }
            }
        }
        channel.close();
    }
}
