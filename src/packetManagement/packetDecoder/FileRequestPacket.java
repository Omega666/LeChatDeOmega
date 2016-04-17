package packetManagement.packetDecoder;

public class FileRequestPacket {
    private final String pseudo;
    private final String nameFile;
    private final int size;
    
    public FileRequestPacket(String pseudo, String nameFile, int size) {
	this.pseudo = pseudo;
	this.nameFile = nameFile;
	this.size = size;
    }
    
    public String getPseudo() {
        return pseudo;
    }
    public String getNameFile() {
        return nameFile;
    }
    public int getSize() {
        return size;
    }
    
    
}
