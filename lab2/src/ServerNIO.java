import java.nio.ByteBuffer;
import java.nio.channels.*;

import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ServerNIO {

    public static void main(String[] argv) throws Exception {
        new ServerNIO().go(argv);
    }

    public void go (String [] argv)
            throws Exception
    {
        int port = 1234;

        if (argv.length > 0) {    
            port = Integer.parseInt (argv [0]);
        }

        System.out.println ("Listening on port " + port);

        ServerSocketChannel serverChannel = ServerSocketChannel.open(  );
        ServerSocket serverSocket = serverChannel.socket(  );
        Selector selector = Selector.open(  );

        serverSocket.bind (new InetSocketAddress (port));

        serverChannel.configureBlocking (false);

        serverChannel.register (selector, SelectionKey.OP_ACCEPT);

        List<SocketChannel> channels = new ArrayList<>();
        while (true) {
            int n = selector.select(  );

            if (n == 0) {
                continue;    
            }

            Iterator it = selector.selectedKeys().iterator(  );

            while (it.hasNext(  )) {
                SelectionKey key = (SelectionKey) it.next(  );

                if (key.isAcceptable(  )) {
                    ServerSocketChannel server =
                            (ServerSocketChannel) key.channel(  );
                    SocketChannel channel = server.accept(  );
                    channels.add(channel);
                    System.out.println("channels = " + channels);

                    System.out.println("channel = " + channel);

                    registerChannel (selector, channel,
                            SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    System.out.println("accept");
                    sayHello (channel);
                }

                if (key.isReadable(  )) {
                    System.out.println("read");
                    readDataFromSocket (key, channels);
                    System.out.println("afterRead" + key.toString());
                }

                it.remove(  );
            }
        }
    }

    protected void registerChannel (Selector selector,
                                    SelectableChannel channel, int ops)
            throws Exception
    {
        if (channel == null) {
            return;       
        }

        channel.configureBlocking (false);

        System.out.println("register");
        channel.register (selector, ops);
        System.out.println("afterReg" + channel.register (selector, ops));
    }


    private ByteBuffer buffer = ByteBuffer.allocate(210000);

    protected void readDataFromSocket(SelectionKey key, List<SocketChannel> channels)
            throws Exception
    {
        SocketChannel socketChannel = (SocketChannel) key.channel(  );
        int count;

        buffer.clear();           

        while ((count = socketChannel.read (buffer)) > 0) {
            buffer.flip();        

            while (buffer.hasRemaining()) {
                System.out.println("channels = " + channels);
                for (Iterator<SocketChannel> it = channels.iterator(); it.hasNext();) {
                    SocketChannel ch = it.next();
                    if(!ch.isConnected()) {
                        it.remove();
                    } else {
                        ch.write(it.hasNext() ? buffer.duplicate() : buffer);
                    }
                }
            }

            buffer.clear();        
        }

        if (count < 0) {
            socketChannel.close();
        }
    }


    private void sayHello (SocketChannel channel)
            throws Exception
    {
        buffer.clear();
        long currentTime = System.currentTimeMillis();
        buffer.flip(  );

        System.out.println("hel");
        channel.write (buffer);

        System.out.println("afteHel");

    }

}

