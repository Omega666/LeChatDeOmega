package packetManagement.packetBuilder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

import packetManagement.packetFlag.PacketFlag;

public class PacketCreator {
    private static int MAX_SIZE_PSEUDO = 20;
    private static int MAX_SIZE_MESSAGE = 500;
    
    public static ByteBuffer intiConnectionPacket(String encodage, String encodagePseudo, String pseudo) {
	ByteBuffer encodageBB = Charset.forName("US-ASCII").encode(encodage);
	ByteBuffer encodagePseudoBB = Charset.forName("US-ASCII").encode(encodagePseudo);
	ByteBuffer pseudoBB = Charset.forName(encodagePseudo).encode(pseudo);
	
	int encodageSize = encodageBB.remaining();
	int encodagePseudoSize = encodagePseudoBB.remaining();
	int pseudoSize = pseudoBB.remaining();
	
	ByteBuffer finalBB = ByteBuffer.allocateDirect(Byte.BYTES + Integer.BYTES + encodageSize + Integer.BYTES + encodagePseudoSize + Integer.BYTES + pseudoSize);
	
	finalBB.put(PacketFlag.INIT.getValue()).putInt(encodageSize).put(encodageBB).putInt(encodagePseudoSize).put(encodagePseudoBB).putInt(pseudoSize).put(pseudoBB);
	
	finalBB.flip();
	
	return finalBB;
    }
    
    public static ByteBuffer messagePacket(String pseudo,String encodagePseudo,  String encodage, String message, byte mode) {
	ByteBuffer pseudoEncodageBB = Charset.forName("US-ASCII").encode(encodagePseudo);
	ByteBuffer pseudoBB = Charset.forName(encodagePseudo).encode(pseudo);
	ByteBuffer encodageNameBB = Charset.forName("US-ASCII").encode(encodage);
	ByteBuffer messageBB = Charset.forName(encodage).encode(message);
	
	int pseudoEncodageSize = pseudoEncodageBB.remaining();
	int pseudoSize = pseudoBB.remaining();
	int encodageSize = encodageNameBB.remaining();
	int messageSize = messageBB.remaining();
	
	if(pseudoSize >= MAX_SIZE_PSEUDO) {
	    throw new IllegalStateException("Pseudo has too many character.");
	}
	
	if(messageSize >= MAX_SIZE_MESSAGE) {
	    throw new IllegalStateException("Message is too big.");
	}
	
	ByteBuffer finalBB = ByteBuffer.allocateDirect(Byte.BYTES+ Integer.BYTES + Integer.BYTES + Integer.BYTES + pseudoEncodageSize + pseudoSize + encodageSize + messageSize);
	
	finalBB.put(mode).putInt(encodageSize).put(encodageNameBB).putInt(pseudoEncodageSize).put(pseudoEncodageBB).putInt(pseudoSize).put(pseudoBB).put(messageBB);
	
	finalBB.flip();
	
	return finalBB;
    }
    
    public static ByteBuffer requestForPrivateSessionPacket(String pseudo, String encodagePseudo) {
	ByteBuffer pseudoEncodageBB = Charset.forName("US-ASCII").encode(encodagePseudo);
	ByteBuffer pseudoBB = Charset.forName(encodagePseudo).encode(pseudo);
	
	int pseudoEncodageSize = pseudoEncodageBB.remaining();
	int pseudoSize = pseudoBB.remaining();
	
	ByteBuffer finalBB = ByteBuffer.allocate(Byte.BYTES + pseudoSize);
	
	finalBB.put(PacketFlag.ASK.getValue()).putInt(pseudoEncodageSize).put(pseudoEncodageBB).put(pseudoBB);
	
	finalBB.flip();
	return finalBB;
    }  
    
    public static ByteBuffer answerForPrivateSessionPacket(PacketFlag answer) {
	
	ByteBuffer finalBB = ByteBuffer.allocate(Byte.BYTES + Byte.BYTES);
	
	finalBB.put(PacketFlag.ANS.getValue()).put(answer.getValue());
	finalBB.flip();
	return finalBB;
    }
    
    public static ByteBuffer requestForIPPacket(String pseudo, String pseudoEncodage) {
	ByteBuffer pseudoEncodageBB = Charset.forName("US-ASCII").encode(pseudoEncodage);
	ByteBuffer pseudoBB = Charset.forName(pseudoEncodage).encode(pseudo);
	
	int pseudoEncodageSize = pseudoEncodageBB.remaining();
	int pseudoSize = pseudoBB.remaining();
	
	ByteBuffer finalBB = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES+ pseudoEncodageSize +pseudoSize);
	
	finalBB.put(PacketFlag.PRIVIP.getValue()).putInt(pseudoEncodageSize).put(pseudoEncodageBB).put(pseudoBB);
	finalBB.flip();
	return finalBB;
    }
    
    public static ByteBuffer answerIPPacket(PacketFlag answer, List<Integer> IP) {
	ByteBuffer finalBB;
	if(answer.getValue() == PacketFlag.SUCCESS_CODE.getValue()) {
	    finalBB = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + Integer.BYTES + Integer.BYTES + Integer.BYTES);
	    
	    finalBB.put(PacketFlag.REIP.getValue()).put(PacketFlag.SUCCESS_CODE.getValue());
	    for(Integer byteIP : IP) {
		finalBB.putInt(byteIP);
	    }
	} else {
	    finalBB = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES);
	    
	    finalBB.put(PacketFlag.REIP.getValue()).put(PacketFlag.FAILURE_CODE.getValue());
	}
	
	
	finalBB.flip();
	return finalBB;
    }
    
    public static ByteBuffer requestForFileSendPacket(String fileName, int fileSize, String pseudo, String pseudoEncodage, String fileEncodage) {
	ByteBuffer fileEncodageBB = Charset.forName("US-ASCII").encode(fileEncodage);
	ByteBuffer pseudoEncodageBB = Charset.forName("US-ASCII").encode(pseudoEncodage);
	ByteBuffer fileNameBB = Charset.forName(fileEncodage).encode(fileName);
	ByteBuffer pseudoBB = Charset.forName(pseudoEncodage).encode(pseudo);
	
	int fileEncodageBBSize = fileEncodageBB.remaining();
	int pseudoEncodageBBSize = pseudoEncodageBB.remaining();
	int pseudoBBSize = pseudoBB.remaining();
	int fileNameBBsize = fileNameBB.remaining();
	
	ByteBuffer finalBB = ByteBuffer.allocateDirect(Byte.BYTES + Integer.BYTES + Integer.BYTES + pseudoBBSize + fileNameBBsize);
	
	finalBB.put(PacketFlag.ASKF.getValue())
		.putInt(fileSize).putInt(pseudoEncodageBBSize).put(pseudoEncodageBB)
		.putInt(pseudoBBSize).put(pseudoBB)
		.putInt(fileEncodageBBSize).put(fileEncodageBB)
		.put(fileNameBB);
		
	finalBB.flip();
	return finalBB;
    }
    
    public static ByteBuffer answerForFileSendPacket(PacketFlag answer) {
	ByteBuffer finalBB = ByteBuffer.allocateDirect(Byte.BYTES + Byte.BYTES);
	
	finalBB.put(PacketFlag.ANSF.getValue()).put(answer.getValue());
	finalBB.flip();
	return finalBB;
    }
    
    public static ByteBuffer filePacket(int uniqueID, ByteBuffer fileBytes) {
	
	ByteBuffer finalBB = ByteBuffer.allocateDirect(Byte.BYTES+ Integer.BYTES + fileBytes.remaining());
	
	finalBB.put(PacketFlag.FILE.getValue()).putInt(uniqueID);
	finalBB.flip();
	return finalBB;
    }
    
    
}	
