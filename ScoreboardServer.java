import java.util.Random;
import javax.crypto.SecretKeyFactory;
import java.util.Arrays;
import javax.crypto.spec.PBEKeySpec;
import java.security.spec.KeySpec;
import javax.net.ssl.SSLServerSocket;
import java.security.SecureRandom;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.security.KeyStore;
import java.io.InputStream;
import java.io.FileInputStream;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
/**
 * Collect all the Scoreboard Server classes in this file. Note that this file
 * MUST BE called ScoreboardServer.java
 */
public class ScoreboardServer {
    private static String MOTD = "\033[32mWelcome\033[0m";
    private ArrayList<ScoreboardClient> clients = new ArrayList<ScoreboardClient>();
    private ArrayList<Account> accounts = new ArrayList<Account>();
    private ArrayList<Game> games;
    private KeyManagerFactory kmf;
    /**
     * Create a new Scoreboard Server
     * @param games ArrayList of Game
     */
    public ScoreboardServer(ArrayList<Game> games) {
        this.games = games;
    }

    /**
     * Set up SSL
     * @param keystorePath A string containing the path of the keystore.jks file
     * @param password A string containing the password for the keystore.jks file
     */
    protected void setupSSL(String keystorePath, String password) {
        // Load the key store
        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance("JKS");
            InputStream ksIs = new FileInputStream(keystorePath);
            ks.load(ksIs, password.toCharArray());
        } catch (java.security.KeyStoreException | IOException | java.security.NoSuchAlgorithmException | java.security.cert.CertificateException e) {
            System.err.println(e);
            return;
        }

        // create a keymanager that uses the keys from the keystore
        try {
            kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, "password".toCharArray());
        } catch(java.security.KeyStoreException | java.security.NoSuchAlgorithmException | java.security.UnrecoverableKeyException e) {
            System.err.println(e);
            return;
        }
    }

    /**
     * Start the server
     * @param SSLPort int containing SSL port number
     */
    protected void startServer(int SSLPort) {
        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("TLSv1.2");
            sc.init(kmf.getKeyManagers(), null, new SecureRandom());
        } catch(NullPointerException | java.security.NoSuchAlgorithmException | java.security.KeyManagementException e) {
            System.err.println(e);
            return;
        }
        this.handleTLS(SSLPort, sc);
    }

    /**
     * Start the server and handle incoming TLS connections
     * @param port TCP port to listen on
     */
    private void handleTLS(int port, SSLContext sc) {
        // create sslserversocket from sslcontext
        SSLServerSocket server;
        try {
            server = (SSLServerSocket) sc.getServerSocketFactory().createServerSocket(port);
        } catch(Exception e) {
            System.err.println(e);
            return;
        }

        BufferedReader in = null;
        PrintWriter out = null;
        while (true) {
            SSLSocket clientSocket = null;
            try {
                // accept connection
                clientSocket = (SSLSocket) server.accept();

                // extract streams
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);
            }
            catch (IOException e) {
                System.err.println(e);
                return;
            }

            // start thread
            ScoreboardClient client = new ScoreboardClient(in, out, clientSocket.getRemoteSocketAddress().toString());
            client.send(MOTD);
            Thread t = new Thread(client);
            t.start();

            // register callback
            client.registerCallback(this);
            clients.add(client);
        }
    }

    /** 
     * Attempt to register nick name (succeeds if nick isn't already taken)
     * @param nick The requested nickname
     * @param password The requested password
     * @return boolean whether or not the registration was successful
     */
    public boolean registerNick(String nick, String password) {
        for(Account acc : accounts) // double check all registered accounts [[should have already been checked when first choosing nick name]]
            if(acc.getNick().equals(nick))
                return false;
        accounts.add(new Account(nick, password));
        System.out.println("New Account Registered: " + nick); // log newly registered accounts
        return true;
    }

    /** 
     * Attempt to login
     * @param nick The account's nickname
     * @param password The account's password
     * @return boolean whether or not login was successful
     */
    public boolean login(String nick, String password) {
        for(Account acc : accounts) // check all registered accounts
            if(acc.getNick().equals(nick)) // account found
                return acc.checkPass(password); // return whether or not the passwords match

        return false; // no nick found
    }

    /**
     * Check if nick is in use
     * @param nick The requested nickname
     * @return boolean whether or not the nick is in use
     */
    public boolean checkNick(String nick) {
        for(ScoreboardClient c : clients) // loop through all chat handlers (make sure name isn't used by a current user - registered or not)
            if(c.getNick().equals(nick))
                return true;
        for(Account acc : accounts) // check all registered accounts
            if(acc.getNick().equals(nick))
                return true;

        return false;
    }

    /**
     * Called when a client disconnects.
     * @param c Disconnecting client
     */
    public void leave(ScoreboardClient c) {
        int i = clients.indexOf(c);
        synchronized(System.out) {
            if (i != -1) {
                System.out.print("Client disconnected. ");
                clients.remove(c);
            }
            System.out.println(clients.size()+" clients remaining.");
        }
    }

    /**
     * Called when a client tries to join a game
     * @param gameID ID of game client is trying to join
     * @param nick The clients Nick name
     * @return the Game the client joined (or null if failed)
     */
    protected Game joinGame(String gameID, String nick) {
        Game g = this.getGame(gameID);
        if(g == null)
            return null; // game not found
        g.addPlayer(nick);// won't add a second player if already in scoreboard (so you continue same score)
        return g;
    }

    /**
     * Called when trying to get/find game
     * @param gameID ID of game
     */
    protected Game getGame(String gameID) {
        for(Game g : games)
            if(g.getId().equals(gameID))
                return g;
        return null;
    }

    /**
     * Called when trying to get a list of all games
     * @return all Games
     */
    protected ArrayList<Game> getGames() {
        return this.games;
    }
}
class Password {
    /**
     * Returns the password hash using the supplied salt (PKCS5 based)
     * @param password the password to be hashed
     * @param salt the salt to be used to compute the hash
     * @return the hashed password
     */
    public static byte[] calculateHash(String password, byte[] salt) {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
        try {
            SecretKeyFactory kf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return kf.generateSecret(spec).getEncoded();
        } catch (java.security.NoSuchAlgorithmException | java.security.spec.InvalidKeySpecException e) {
            return salt;
        }
    }
}
class Account {
    private byte[] salt;
    private byte[] passwordHash;
    private String nick;

    /**
     * constructor
     */
    public Account(String nick, byte[] passwordHash, byte[] salt) {
        this.nick = nick;
        this.passwordHash = passwordHash;
        this.salt = salt;
    }

    /**
     * constructor
     */
    public Account(String nick, String password) {
        this.salt = new byte[16];
        Random r = new Random();
        r.nextBytes(salt);
        this.nick = nick;
        this.passwordHash = Password.calculateHash(password, salt);
    }

    /**
     * check if password matches
     * @param pass the password to check
     * @return whether or not the passwords match
     */
    protected boolean checkPass(String pass) {
        return Arrays.equals(this.passwordHash, Password.calculateHash(pass, this.salt));
    }

    /**
     * get the account nick
     * @return the nick name
     */
    public String getNick() {
        return this.nick;
    }
}
