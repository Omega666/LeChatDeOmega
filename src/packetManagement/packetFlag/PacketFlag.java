package packetManagement.packetFlag;

public enum PacketFlag {
    INIT ((byte) 0),
    PUB ((byte) 1),
    PRIVIP ((byte) 2),
    REIP ((byte) 3),
    ASK ((byte) 4),
    ANS ((byte) 5),
    PRIV ((byte) 6),
    ASKF ((byte) 7),
    ANSF ((byte) 8),
    FILE ((byte) 9),
    NINIT ((byte) 11),
    SUCCESS_CODE ((byte) 1),
    FAILURE_CODE ((byte) 0),
    IPV4_CODE ((byte) 1),
    IPV6_CODE ((byte) 0);
    
    private final byte value;
    
    PacketFlag(byte value) {
	this.value = value;
    }
    
    public byte getValue() {

	return value;
    }
    
}
