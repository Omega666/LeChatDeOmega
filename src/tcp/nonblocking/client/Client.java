package tcp.nonblocking.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Optional;
import java.util.Scanner;

import packetManagement.packetBuilder.PacketCreator;
import packetManagement.packetDecoder.MessagePacket;
import packetManagement.packetDecoder.PacketDecoder;
import packetManagement.packetFlag.PacketFlag;

/*Faire l'envoi de connexion
 *Lister les commandes :
 *	/w pseudo-> message privée
 *	/askw pseudo -> demande de session privée
 * 	/askf pseudo nameFile-> demande d'envoi de fichier
 * 	/help
 * 	/whois 
 *      /quitw pseudo
 *      /quit
 */
public class Client {
    private static final int MAX_PSEUDO_SIZE = 30;
    public static void main(String[] args) throws IOException {
	if(args.length != 3) {
	    System.err.println("Usage: <adresse serveur> <port> <pseudo> (sans les <>)");
	    System.exit(-1);
	}
	
	if(args[2].length() > MAX_PSEUDO_SIZE) {
	    System.err.println("Le pseudo est trop grand, il doit être inferieur à 30 caractère.");
	    System.exit(-1);
	}
	String server = args[0];
	int port = Integer.parseInt(args[1]);
	String pseudo = args[2];


	try(SocketChannel sc = SocketChannel.open(new InetSocketAddress(server, port))) {
	  //Envoi du pseudo au serveur
	    ByteBuffer initPacketBB = PacketCreator.intiConnectionPacket("US-ASCII", "UTF-8", pseudo);

	    try {
		sc.write(initPacketBB);
	    }catch(IOException e) {
		sc.close();
		System.err.println("Problème lors de l'envoi du pseudo: "+ e.getMessage());
		System.exit(-1);
	    }

	    //Thread d'écriture
	    new Thread(() -> {
		try(Scanner scan = new Scanner(System.in)) {
		    while(scan.hasNextLine()) {
			String message = scan.nextLine();
			if(message.charAt(0) != '/') {//On envoi en message publique
			    ByteBuffer publicMessageBB = PacketCreator.messagePacket(pseudo, "UTF-8", "UTF-8", message, PacketFlag.PUB.getValue());
			    //System.out.println("PUB value " + PacketFlag.PUB.getValue());
			    try {
				sc.write(publicMessageBB);
			    } catch (Exception e) {
				e.printStackTrace();
			    }

			} else {//On est dans le cas d'une commande
			    String command = message.substring(1);
			    String[] token = command.split(" ");

			    switch(token[0]) {
			    case "w"://Message privé
				if(token.length != 3) {
				    System.out.println("Usage message privé: /w pseudo message.");
				}
				break;
			    case "askw"://demande de session privée
				if(token.length!= 2) {
				    System.out.println("Usage demande de session privé: /askw pseudo");
				}
				break;
			    case "quitw":
				if(token.length!=2) {
				    System.out.println("Usage quitw: /quitw pseudo");
				}
				break;
			    case "askf":
				if(token.length!=3) {
				    System.out.println("Usage askf: /askf pseudo nameFile");
				}
				break;
			    case "help"://affiche toute les commandes
				//On ignore les arguments aprés si il y en a
				System.out.println("Liste des commandes: \n"
					+ "	/w pseudo message -> envoyer un message privé à pseudo.\n"
					+ "	/askw pseudo -> faire une demande de communication privé à pseudo.\n"
					+ "	/askf pseudo nameFile -> envoyer le fichier nameFile à pseudo.\n"
					+ "	/help -> affiche toutes les commandes.\n"
					+ "	/whois -> affiche qui est connecté sur le serveur.\n"
					+ "	/quitw pseudo -> ferme la session privée établit avec pseudo\n"
					+ "	/quit -> sortir du programme.\n");
				break;
			    case "whois"://affiche qui est connecté
				//Faire le ByteBuffer correspondant.
				break;
			    case "quit"://quitte le programme
				System.out.println("Vous quittez le serveur");
				System.exit(1);
				break;
			    default:
				System.out.println("La commande n'existe pas. /help pour avoir la liste des commandes.");
				break;
			    }
			}
		    }
		}
	    }).start();

	    //THread d'écoute
	    ByteBuffer messageReadedBB = ByteBuffer.allocate(1024);

	    while(sc.read(messageReadedBB) != -1) {
		messageReadedBB.flip();
		byte typeMessage = messageReadedBB.get();
		
		if(typeMessage == PacketFlag.NINIT.getValue()) {
		    System.out.println("Connexion refusé par le serveur.");
		    if(messageReadedBB.get() == PacketFlag.SUCCESS_CODE.getValue()) {
			System.out.println("Le pseudo " + pseudo + " est déjà utilisé.");
		    }
		    else if(messageReadedBB.get() == PacketFlag.FAILURE_CODE.getValue()){
			System.out.println("Le serveur est plein.");
		    }
		    else {
			//DEBUG
			System.err.println("FLAG INCONNU POUR LE CLIENT "+ typeMessage);

		    }
		    messageReadedBB.clear();
		    System.exit(1);
		}
		else if(typeMessage == PacketFlag.PUB.getValue()) {
		    //Gérer la réception de message public
		    //System.out.println("RECEPTION DU MESSAGE");
		    
		    Optional<MessagePacket> mpOpt = PacketDecoder.messagePacketDecoder(messageReadedBB);
		    if(mpOpt.isPresent()) {
			MessagePacket mp = mpOpt.get();
			System.out.println(mp.getPseudo() + ": " + mp.getMessage());
		    }
		}
		else if(typeMessage == PacketFlag.PRIV.getValue()) {
		  //Gérer la réception de message privé
		}
		else if(typeMessage == PacketFlag.PRIVIP.getValue()) {
		  //Gérer la réception de demande privé
		}
		else if(typeMessage == PacketFlag.ANS.getValue()) {
		  //Gérer la réception de réponse de demande privé
		}
		else if(typeMessage == PacketFlag.ASKF.getValue()) {
		  //Gérer la réception de demande d'envoi de fichier.
		}
		else if(typeMessage == PacketFlag.ANSF.getValue()) {
		  //Gérer la réception de réponse à la demande d'envoi de fichier.
		}
		else {
		    //DEBUG
		    System.err.println("FLAG INCONNU POUR LE CLIENT "+ typeMessage);
		    messageReadedBB.clear();
		}
		messageReadedBB.clear();
	    }

	}
    }
}
