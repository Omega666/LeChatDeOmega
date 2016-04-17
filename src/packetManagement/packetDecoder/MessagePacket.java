package packetManagement.packetDecoder;


public class MessagePacket {
    private final String message;
    private final String pseudo;
    
    public MessagePacket(String pseudo, String message) {
	this.message = message;
	this.pseudo = pseudo;
    }

    public String getMessage() {
        return message;
    }

    public String getPseudo() {
        return pseudo;
    }
}
