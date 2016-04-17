package packetManagement.packetDecoder;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Optional;

import packetManagement.packetFlag.PacketFlag;



public class PacketDecoder {
    	private static int MAX_SIZE_PSEUDO = 20;
    	private static int MAX_SIZE_MESSAGE = 500;
    	
    	public static String initPacketDecoder(ByteBuffer bb) {
    	    bb.flip();
    	    bb.get();
    	  
    	    int size = bb.getInt();
    	    int oldLimit = bb.limit();

    	    bb.limit(Byte.BYTES + Integer.BYTES + size);
    	    String encodage = Charset.forName("US-ASCII").decode(bb).toString();
    	    
    	    bb.limit(oldLimit);
    	    
    	    int size2 = bb.getInt();
    	    bb.limit(Byte.BYTES+ Integer.BYTES + size + Integer.BYTES + size2);
    	    String encodagePseudo = Charset.forName("US-ASCII").decode(bb).toString();
    	    bb.limit(oldLimit);
    	    
    	    int sizePseudo = bb.getInt();
    	    bb.limit(Byte.BYTES + Integer.BYTES + size + Integer.BYTES + size2+Integer.BYTES + sizePseudo);
    	    
    	    return Charset.forName(encodagePseudo).decode(bb).toString();
    	    
    	}
    	
    	public static Optional<MessagePacket> messagePacketDecoder(ByteBuffer bb) {
    	    int encodageSize = bb.getInt();
    	    
    	    if(encodageSize >= bb.remaining() || encodageSize <= 0) {
    		System.err.println("Encodage size problem: size 1 -> " + encodageSize);
    		return Optional.empty();
    	    }
    	    
    	    int oldLimit = bb.limit();
    	    
    	    bb.limit(Byte.BYTES + Integer.BYTES + encodageSize);
    	    String encodageName = Charset.forName("US-ASCII").decode(bb).toString();
    	    bb.limit(oldLimit);
    	    
    	    int pseudoEncodageSize = bb.getInt();
    	    if(pseudoEncodageSize >= bb.remaining() || pseudoEncodageSize <= 0) {
    		System.err.println("pseudoEncodage size problem: size  2 -> " + pseudoEncodageSize);
    		return Optional.empty();
    	    }
    	    bb.limit(Byte.BYTES + Integer.BYTES + encodageSize + Integer.BYTES + pseudoEncodageSize);
    	    String pseudoEncodageName = Charset.forName("US-ASCII").decode(bb).toString();
    	    bb.limit(oldLimit);
    	    
    	    
    	    int pseudoSize = bb.getInt();
    	    if(pseudoSize >= MAX_SIZE_PSEUDO || pseudoSize <= 0 || Integer.BYTES + encodageSize + Integer.BYTES + pseudoSize >= bb.capacity()) {
    		System.err.println("pseudo size problem: size -> " + pseudoSize);
    		return Optional.empty();
    	    }
    	    
    	    bb.limit(Byte.BYTES + Integer.BYTES + encodageSize + Integer.BYTES + pseudoEncodageSize + Integer.BYTES + pseudoSize);
    	    
    	    String pseudo = Charset.forName(pseudoEncodageName).decode(bb).toString();
    	    
    	    bb.limit(oldLimit);
    	    
    	    String message = Charset.forName(encodageName).decode(bb).toString();
    	    
    	    return Optional.of(new MessagePacket(pseudo, message));
    	}
    	
    	public static FilePacket filePacketDecoder(ByteBuffer bb) {
    	    int uniqueId = bb.getInt();
    	    
    	    ByteBuffer defensiveCopyBB = ByteBuffer.allocateDirect(bb.remaining());
    	    defensiveCopyBB.put(bb);
    	    
    	    return new FilePacket(defensiveCopyBB, uniqueId);
    	}
    	
    	public static Optional<String> requestForPrivateSessionDecoder(ByteBuffer bb) {
    	    int oldLimit = bb.limit();
    	    int pseudoEncodageSize = bb.getInt();
	    if(pseudoEncodageSize >= bb.remaining() || pseudoEncodageSize <= 0) {
		System.err.println("pseudoEncodage size problem: size -> " + pseudoEncodageSize);
		return Optional.empty();
	    }
	    bb.limit(Integer.BYTES + pseudoEncodageSize);
	    String pseudoEncodageName = Charset.forName("US-ASCII").decode(bb).toString();
	    bb.limit(oldLimit);
    	    return Optional.of(Charset.forName(pseudoEncodageName).decode(bb).toString());
    	}
    	
    	public static boolean answerForPrivateSessionDecoder(ByteBuffer bb) {
    	    return (bb.get() == PacketFlag.SUCCESS_CODE.getValue());
    	}
    	
    	public static Optional<String> requestForIPDecoder(ByteBuffer bb) {    
    	    int oldLimit = bb.limit();
    	    int pseudoEncodageSize = bb.getInt();
    	    if(pseudoEncodageSize >= bb.remaining() || pseudoEncodageSize <= 0) {
    		System.err.println("pseudoEncodage size problem: size -> " + pseudoEncodageSize);
    		return Optional.empty();
    	    }
    	    bb.limit(Integer.BYTES + pseudoEncodageSize);
    	    String pseudoEncodageName = Charset.forName("US-ASCII").decode(bb).toString();
    	    bb.limit(oldLimit);
    	    return Optional.of(Charset.forName(pseudoEncodageName).decode(bb).toString());
    	}

    	public static Optional<String> answerForIPDecoder(ByteBuffer bb) {
    	    if(bb.get() == PacketFlag.FAILURE_CODE.getValue())
    		return Optional.empty();
    	    
    	    String ip ="";
    	    
    	    for(int i = 0; i < 4; i++) {
    		ip += bb.getInt();
    	    }
    	    
    	    return Optional.of(ip);
    	}
    	
    	public static Optional<FileRequestPacket> requestForFileSendDecoder(ByteBuffer bb) {
    	    int sizeFile = bb.getInt();
    	    
    	    int pseudoEncodageSize = bb.getInt();
    	    if(pseudoEncodageSize >= bb.remaining() || pseudoEncodageSize <= 0) {
		System.err.println("Encodage size problem: size -> " + pseudoEncodageSize);
		return Optional.empty();
	    }
    	    
    	    int oldLimit  = bb.limit();
    	    bb.limit(Integer.BYTES + Integer.BYTES +pseudoEncodageSize);
    	    String pseudoEncodageName = Charset.forName("US-ASCII").decode(bb).toString();
    	    bb.limit(oldLimit);
    	    
    	    int pseudoSize = bb.getInt();
    	    
    	    if(pseudoSize >= MAX_SIZE_PSEUDO || pseudoSize + Integer.BYTES >= bb.remaining()) {
    		System.err.println("Pseudo too big.");
    		return Optional.empty();
    	    }
    	    
    	    
    	    bb.limit(Integer.BYTES + Integer.BYTES +pseudoEncodageSize + Integer.BYTES + pseudoSize);
    	    
    	    String pseudo = Charset.forName(pseudoEncodageName).decode(bb).toString();
    	    
    	    bb.limit(oldLimit);
    	    int encodageFileSize = bb.getInt();
	    if(encodageFileSize >= bb.remaining() || encodageFileSize <= 0) {
		System.err.println("Encodage size problem: size -> " + encodageFileSize);
		return Optional.empty();
	    }
	    
	    bb.limit(Integer.BYTES + Integer.BYTES +pseudoEncodageSize + Integer.BYTES + pseudoSize+ Integer.BYTES +encodageFileSize);
	    String encodageFileName = Charset.forName("US-ASCII").decode(bb).toString();
	    bb.limit(oldLimit);
    	    
    	    String nameFile = Charset.forName(encodageFileName).decode(bb).toString();
    	    
    	    return Optional.of(new FileRequestPacket(pseudo, nameFile, sizeFile));
    	}
    	
    	public static boolean answerForFileSendDecoder(ByteBuffer bb) {
    	   return (bb.get() == PacketFlag.SUCCESS_CODE.getValue());
    	}
}
