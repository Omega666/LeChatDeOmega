package packetManagement.packetDecoder;
import java.nio.ByteBuffer;

public class FilePacket {
    private final ByteBuffer bbFile;
    private final int uniqueId;
    
    /*Faire une copie défensive du ByteBuffer avant de l'utiliser dans le constructeur*/
    public FilePacket(ByteBuffer bbFile, int uniqueId) {
	this.bbFile = bbFile;
	this.uniqueId = uniqueId;
    }

    public ByteBuffer getBbFile() {
        return bbFile;
    }

    public int getUniqueId() {
        return uniqueId;
    }
}
