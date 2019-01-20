// Author: Dr. Leune - Provided Initial version
// Editor: Mateusz Gembarzewski - Finialized to work with project
import java.util.ArrayList;

public class Main {
    private static final int SSLPort = 4001;

    public static void main(String... args) {
        ArrayList<Game> games = new ArrayList<Game>();

        ChallengeResponseGame cryptoGame = new ChallengeResponseGame("crypto");
        cryptoGame.addQuestion(new Question(
                "q1",
                "What block cipher uses a block size of 128 bits?",
                "AES",
                5
            ));
        cryptoGame.addQuestion(new Question(
                "q2",
                "Which asymmetric cipher is based on the integer factorization problem?",
                "RSA",
                5
            ));
        games.add(cryptoGame);

        ChallengeResponseGame networkingGame = new ChallengeResponseGame("networking");
        networkingGame.addQuestion(new Question (
                "q1",
                "What layer-7 protocol would you expect at TCP port 25?",
                "SMTP",
                5
            ));
        networkingGame.addQuestion(new Question (
                "q2",
                "What is the name of the layer in the OSI stack at which routers operate?",
                "Network layer",
                5
            ));
        games.add(networkingGame);

        ScoreboardServer server = nw ScoreboardServer(games);
        server.setupSSL("./keystore.jks", "password");
        server.startServer(SSLPort);
    }
}
