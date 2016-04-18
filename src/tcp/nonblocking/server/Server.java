package tcp.nonblocking.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import packetManagement.packetDecoder.PacketDecoder;
import packetManagement.packetFlag.PacketFlag;


public class Server {
    private int cptkey = 0;
    class Context{
	boolean isClosed = false;
	boolean writeAll = false;
	ByteBuffer bb = ByteBuffer.allocateDirect(1024);
	ByteBuffer out = ByteBuffer.allocateDirect(1024);
	BlockingQueue<ByteBuffer> bqPacket = new ArrayBlockingQueue<>(1000);
	SelectionKey key;
	SocketChannel sc;
	String Name;

	Context(SelectionKey key, int cpt) {
	    this.Name = "key" + cpt;
	    this.key = key;
	    this.sc = (SocketChannel) key.channel();
	}

	int updateInterestOps() {
	    int newInterestOps =0;
	    System.out.println();
	    if(/*out.position()!=0 || */!bqPacket.isEmpty()) {
		newInterestOps |= SelectionKey.OP_WRITE;
		System.out.println("Write mode pour la clef" + Name);
	    }

	    if(!isClosed && bb.hasRemaining()) {
		newInterestOps |= SelectionKey.OP_READ;
		System.out.println("Read mode pour la clef" + Name);
	    }

	    return newInterestOps;
	}
	
	void doRead() throws IOException {  
	    	bb.clear();
		int read = sc.read(bb);

		if (read == -1) {
		    isClosed = true;
		    return;
		}
		bb.flip();
		ByteBuffer answerPacket = ByteBuffer.allocate(bb.capacity());
		answerPacket.put(bb);
		bb.flip();
		byte typeMessage = bb.get();
		//System.out.println("LA TAILLE DE L'ENCODAGE " + bb.getInt());
		
		System.out.println("LE TYPE DU MESSAGE -> " + typeMessage);
		bb.flip();
		
	    	//bb.compact();
	    	if(typeMessage == PacketFlag.INIT.getValue()) {
	    	    //Mettre le Reader INIT
	    	    System.out.println("PAQUET D INITIALISATION RECU");
	    	    String pseudo = PacketDecoder.initPacketDecoder(answerPacket);
	    	    System.out.println("LE PSEUDO: " + pseudo);
	    	    if(clientHM.get(pseudo) != null) {
	    		ByteBuffer ninitBB = ByteBuffer.allocateDirect(Byte.BYTES+Byte.BYTES);
	    		ninitBB.put(PacketFlag.NINIT.getValue()).put(PacketFlag.SUCCESS_CODE.getValue());
	    		bqPacket.add(ninitBB);
	    	    } else {
	    		clientHM.put(pseudo, (InetSocketAddress) sc.getRemoteAddress());
	    	    }
	    	    
	    	    int newInterestOps = updateInterestOps();
	    	    key.interestOps(newInterestOps);
	    	}
	    	else if(typeMessage == PacketFlag.PUB.getValue()) {
			//BroadCast Loop
	    	    	System.out.println("PAQUET DE MESSAGE PUBLIC RECU.");
			for(SelectionKey keyBroadCast : selector.keys()) {
			    Context cc  = (Context) keyBroadCast.attachment();
			    if((cc) != null) {

				System.out.println("The key name is " + cc.Name);

				cc.bqPacket.add(answerPacket);

				int newInterestOps = cc.updateInterestOps();
				System.out.println("InteresOpsValue in the doRead -> " + newInterestOps);
				if(newInterestOps == 0) {
				    System.out.println("ON FERME LE CLIENT " + cc.sc.getRemoteAddress());
				    cc.sc.close();
				}		
				cc.key.interestOps(newInterestOps);
			    }
			}
		    
		}
	}

	void doWrite() throws IOException {
	    fullFill();
	    out.flip();
	    
	    int remainingToWrite = out.remaining();
	   
	    sc.write(out);

	    out.compact();

	    int newInterestOps = updateInterestOps();
	    System.out.println("InterestOps Value in the doWrite -> " + newInterestOps);
	    System.out.println("position :" + out.position() + ", isClosed field :" + isClosed);
	    System.out.println("bb hasRemaining :" + bb.hasRemaining());
	 
	    if(newInterestOps == 0) {
		System.out.println("ON FERME LE CLIENT " + sc.getRemoteAddress());
		sc.close();
	    }
	    key.interestOps(newInterestOps);
	}
	
	void fullFill() {
	    ByteBuffer b = bqPacket.peek();
	    b.flip();
	    out.flip();
	    int capacityLeft = out.capacity() - out.limit();
	    System.out.println("capacityLeft:" + capacityLeft + " remaining: "+ b.remaining());
	    if(capacityLeft >= b.remaining()) {
		out.compact();
		out.put(b);
		bqPacket.poll();
	    }
	   
	  
	}
    }

    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;
    private final Set<SelectionKey> selectedKeys;
    private HashMap<String, InetSocketAddress> clientHM = new HashMap<>();

    public Server(int port) throws IOException {
	serverSocketChannel = ServerSocketChannel.open();
	serverSocketChannel.bind(new InetSocketAddress(port));
	selector = Selector.open();
	selectedKeys = selector.selectedKeys();
    }

    public void launch() throws IOException {
	serverSocketChannel.configureBlocking(false);
	serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

	while (!Thread.interrupted()) {
	    printKeys();
	    System.out.println("Starting select");
	    selector.select();
	    System.out.println("Select finished");

	    printSelectedKey();

	    processSelectedKeys();

	    selectedKeys.clear();
	}
    }

    private void processSelectedKeys() throws IOException {
	for (SelectionKey key : selectedKeys) {

	    if (key.isValid() && key.isAcceptable()) {
		doAccept(key);
	    }
	    Context cc = (Context) key.attachment();
	    if(cc == null) {
		continue;
	    }
	    try {
		System.out.println("Position du BB out dans la méthode processSelectedKeys " + cc.out.position() + ", Pour la clef " + cc.Name);
		if (key.isValid() && key.isWritable()) {
		    cc.doWrite();
		}
		if (key.isValid() && key.isReadable()) {
		    cc.doRead();
		}
	    } catch (IOException e) {
		System.err.println("FERMERTURE D'UN CLIENT :" + e.getMessage());
		clientHM.values().remove(cc.sc.getRemoteAddress());
		
		for(String pseudo : clientHM.keySet()) {
		    System.out.println(pseudo + ":" + clientHM.get(pseudo));
		}
		
		cc.sc.close();
	    }
	}
    }

    private void doAccept(SelectionKey key) throws IOException {
	SocketChannel sc = serverSocketChannel.accept();
	if (sc == null)
	    return;
	sc.configureBlocking(false);
	Context cc = new Context(sc.register(selector, SelectionKey.OP_READ),cptkey);
	cptkey++;
	sc.register(selector, SelectionKey.OP_READ, cc);
    }

    public static void main(String[] args) throws NumberFormatException, IOException {
	new Server(4545).launch();
    }

    /***
     * Theses methods are here to help understanding the behavior of the
     * selector
     ***/

    private static String interestOpsToString(SelectionKey key) {
	if (!key.isValid()) {
	    return "CANCELLED";
	}
	int interestOps = key.interestOps();
	ArrayList<String> list = new ArrayList<>();
	if ((interestOps & SelectionKey.OP_ACCEPT) != 0)
	    list.add("OP_ACCEPT");
	if ((interestOps & SelectionKey.OP_READ) != 0)
	    list.add("OP_READ");
	if ((interestOps & SelectionKey.OP_WRITE) != 0)
	    list.add("OP_WRITE");
	return String.join("|", list);
    }

    public void printKeys() {
	Set<SelectionKey> selectionKeySet = selector.keys();
	if (selectionKeySet.isEmpty()) {
	    System.out.println("The selector contains no key : this should not happen!");
	    return;
	}
	System.out.println("The selector contains:");
	for (SelectionKey key : selectionKeySet) {
	    SelectableChannel channel = key.channel();
	    if (channel instanceof ServerSocketChannel) {
		System.out.println("\tKey for ServerSocketChannel : " + interestOpsToString(key));
	    } else {
		SocketChannel sc = (SocketChannel) channel;
		System.out.println("\tKey for Client " + remoteAddressToString(sc) + " : " + interestOpsToString(key));
	    }

	}
    }

    private static String remoteAddressToString(SocketChannel sc) {
	try {
	    return sc.getRemoteAddress().toString();
	} catch (IOException e) {
	    System.out.println(e.getMessage());
	    return "???";
	}
    }

    private void printSelectedKey() {
	if (selectedKeys.isEmpty()) {
	    System.out.println("There were not selected keys.");
	    return;
	}
	System.out.println("The selected keys are :");
	for (SelectionKey key : selectedKeys) {
	    SelectableChannel channel = key.channel();
	    if (channel instanceof ServerSocketChannel) {
		System.out.println("\tServerSocketChannel can perform : " + possibleActionsToString(key));
	    } else {
		SocketChannel sc = (SocketChannel) channel;
		System.out.println(
			"\tClient " + remoteAddressToString(sc) + " can perform : " + possibleActionsToString(key));
	    }

	}
    }

    private static String possibleActionsToString(SelectionKey key) {
	if (!key.isValid()) {
	    return "CANCELLED";
	}
	ArrayList<String> list = new ArrayList<>();
	if (key.isAcceptable())
	    list.add("ACCEPT");
	if (key.isReadable())
	    list.add("READ");
	if (key.isWritable())
	    list.add("WRITE");
	return String.join(" and ", list);
    }
}
