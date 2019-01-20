import java.util.Map;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.ArrayList;
/**
 * Collect all the Scoreboard Client classes in this file. Note that this file
 * MUST BE called ScoreboardClient.java
 */
class ScoreboardClient implements Runnable {
    private BufferedReader in;
    private PrintWriter out;
    private ScoreboardServer master;
    private String nick;
    private int points; // client score
    private ChallengeResponseGame currGame; // current game
    private boolean loggedIn; // user currently logged in/registered

    /**
     * Constructor
     * @param in Input stream
     * @param out Output stream
     * @param nick Nick name string
     */

    public ScoreboardClient(BufferedReader in, PrintWriter out, String nick) {
        this.in = in;
        this.out = out;
        this.nick = nick;
        loggedIn = false;
        currGame = null;
    }

    /**
     * Register chat server handler for callbacks
     */
    protected void registerCallback(ScoreboardServer c) {
        this.master = c;
    }

    /**
     * Display text to client
     * @param text Text to be displayed
     */
    protected void send(String text) {
        out.printf("%s\r\n", text);
    }

    /**
     * Display the current scoreboard
     */
    protected void displayScoreboard() {
        this.send("\033[35m-----------------");
        this.send("**SCOREBOARD**");
        currGame.getScores().entrySet().stream().forEach(entry ->
                this.send(entry.getKey() + ": " + entry.getValue())
        );
        this.send("-----------------\033[0m");
    }
    
    /**
     * Display the current scoreboard for requested game
     */
    protected void displayScoreboard(String gameID) {
        Game temp = master.getGame(gameID);
        if(!(temp instanceof ChallengeResponseGame)) { // invalid gameID
            this.send("\033[31m*** Invalid Game ID\033[0m");
            return;
        }
        ChallengeResponseGame g = (ChallengeResponseGame) temp;
        
        this.send("\033[35m-----------------");
        this.send("**SCOREBOARD (GameID " + gameID + ")**");
        g.getScores().entrySet().stream().forEach(entry ->
                this.send(entry.getKey() + ": " + entry.getValue())
        );
        this.send("-----------------\033[0m");
    }

    /**
     * Display all games
     */
    protected void displayGames() {
        this.send("\033[35m-----------------");
        this.send("**Games**");
        ArrayList<Game> games = master.getGames();
        for(Game g : games)
            this.send(g.getId());
        this.send("-----------------\033[0m");
    }

    /** 
     * Display all questions
     * format: ID. question (points )
     */
    protected void displayQuestions() {
        this.send("\033[35m-----------------");
        this.send("**Questions**");
        ArrayList<Question> questions = this.currGame.getQuestions();
        for(Question q : questions)
            this.send(q.getId() + ": " + q.getQuestion() + " (Points: " + q.getPoints() + ")");
        this.send("-----------------\033[0m");
    }

    /** 
     * Display all questions
     * format: ID. question (points )
     * @param gameID id of game trying to print questions of
     */
    protected void displayQuestions(String gameID) {
        Game temp = master.getGame(gameID);
        if(!(temp instanceof ChallengeResponseGame)) { // invalid gameID
            this.send("\033[31m*** Invalid Game ID\033[0m");
            return;
        }
        ChallengeResponseGame g = (ChallengeResponseGame) temp;
        
        this.send("\033[35m-----------------");
        this.send("**Questions (GameID " + gameID + ")**");
        ArrayList<Question> questions = g.getQuestions();
        for(Question q : questions)
            this.send(q.getId() + ": " + q.getQuestion() + " (Points: " + q.getPoints() + ")");
        this.send("-----------------\033[0m");
    }

    /**
     * get client nickname
     * @return client nick
     */
    protected String getNick() {
        return nick;
    }

    @Override
    /**
     * Thread logic
     */
    public void run() {
        /** TODO: update show qusetions/scoreboard to show w/ gameID **/
        /**
         * /REGISTER password
         * /LOGIN nick password
         * 
         * /JOIN gameID
         * /LEAVE
         * /NICK nickname
         * /SHOW games|questions|scoreboard (gameID)
         * /ANSWER qID ans
         */
        while(true) {
            /** get input **/
            String input = null;
            try {
                input = in.readLine(); // read line
            }
            catch (IOException e)
            {
                System.err.println(e);
            }
            /** no input **/
            if(input == null) // error reading or end of stream [probably closed window]
            {
                //master.sendToAll("*** " + nick + "'s connection has dropped"); // send disconnect message to everyone
                master.leave(this); // remove this from arraylist
                break;
            }
            /** QUIT **/
            if(input.toUpperCase().matches("/QUIT|QUIT") ) // disconnect
            {
                //master.sendToAll("*** " + nick + " has disconnected."); // send disconnect message to everyone
                master.leave(this); // remove this from arraylist
                break;
            }
            /** empty line **/
            if(input.equals(""))
                continue; // blank line
            /** NICK **/
            if(input.toUpperCase().startsWith("/NICK ")) { // user is requesting new nickname
                if(loggedIn) { // already registered
                    this.send("\033[31m*** ERROR: Cannot change nick after registering.\033[0m");
                    continue;
                }
                if(currGame != null) { // currently in game
                    this.send("\033[31m*** ERROR: Cannot change nick while in a game [[/LEAVE]].\033[0m");
                    continue;
                }
                input = input.replaceFirst("(?i)/NICK ", ""); // remove command (case insensitive)
                if(input.equals("")) { // no nick entered
                    this.send("\033[31m*** /NICK new nickname\033[0m");
                    continue;
                }
                if(input.contains(" ")) { // invalid nick
                    this.send("\033[31m*** ERROR: Invalid nick\033[0m");
                    continue;
                }
                if(!master.checkNick(input)) { // nickname not being used
                    // master.sendToAll("*** " + nick + " is now known as " + input); // send to everyone (no sender)
                    this.nick = input;
                    this.send("\033[32m*** nick set to " + input + ".\033[0m");
                }
                else { // nick used - error message
                    this.send("\033[31m*** ERROR: nick " + input + " is already taken.\033[0m"); // send error message to user only
                }
                continue;
            }
            /** REGISTER **/
            if(input.toUpperCase().startsWith("/REGISTER ")) { // user is requesting to register current nickname
                if(loggedIn) { // user already logged in/registered
                    this.send("\033[31m*** ERROR: Already Logged In.\033[0m");
                    continue;
                }
                input = input.replaceFirst("(?i)/REGISTER ", "");
                if(input.equals("")) { // no password entered
                    this.send("\033[31m*** /REGISTER password\033[0m");
                    continue;
                }
                if(master.registerNick(this.nick, input)) {// attempt to register nickname/password
                    this.send("\033[32m*** " + this.nick  + " Registered.\033[0m");
                    this.loggedIn = true;
                }
                else
                    this.send("\033[31m*** ERROR: Issue registering nick.\033[0m");
                continue;
            }
            /** LOGIN **/
            if(input.toUpperCase().startsWith("/LOGIN ")) { // user is attempting to login
                if(loggedIn) { // user already logged in/registered
                    this.send* /REGISTER password
         * /LOGIN nick password
                            *
         * /JOIN gameID
         * /LEAVE
                            * /NICK nickname
         * /SHOW games|questions|scoreboard (gameID)
                            * /ANSWER qID ans("\033[31m*** ERROR: Already Logged In.\033[0m");
                    continue;
                }
                input = input.replaceFirst("(?i)/LOGIN ", "");
                if(input.equals("")) { // no details entered
                    this.send("\033[31m*** /LOGIN nick password\033[0m");
                    continue;
                }
                String acc[] = input.split(" ", 2); // separate nick and password
                if(acc.length != 2 || acc[1].equals("")) { // didnt enter nick & password
                    this.send("\033[31m*** /LOGIN nick password\033[0m");
                    continue;
                }

                if(master.login(acc[0], acc[1])) { // attempt to login w/ user & pass
                    this.send("\033[32m*** Success: Logged in as " + acc[0] + ".\033[0m");
                    this.nick = acc[0];
                    this.loggedIn = true;
                }
                else
                    this.send("\033[31m*** ERROR: Invalid Account Credentials.\033[0m");
                continue;
            }
            /** SHOW [games, questions, scoreboard]**/
            if(input.toUpperCase().startsWith("/SHOW ")) { // user is requesting to show something (games, questions or scoreboard)
                input = input.replaceFirst("(?i)/SHOW ", ""); // remove command (case insensitive)
                if(input.equals("")) { // no command entered
                    this.send("\033[31m*** /SHOW games, questions, scoreboard (gameID)\033[0m");
                    continue;
                }
                if(input.toUpperCase().startsWith("GAMES")) { // show games
                    this.displayGames();
                    continue;
                }

                String req[] = input.split(" ", 2); // separate potential gameID from request
                boolean gameIDEntered = true;
                if(req.length != 2 || req[1].equals("")) // didnt enter gameID
                    gameIDEntered = false;

                if(input.toUpperCase().startsWith("QUESTIONS")) { // show questions
                    if(gameIDEntered) {
                        this.displayQuestions(req[1]); // print w/ gameID
                        continue;
                    }
                    if(currGame == null) { // not in a game
                        this.send("\033[31m*** Must be in a game to use /SHOW questions\033[0m");
                        continue;
                    }
                    this.displayQuestions();
                    continue;
                }
                if(input.toUpperCase().startsWith("SCOREBOARD")) { // show scoreboard
                    if(gameIDEntered) {
                        this.displayScoreboard(req[1]); // print w/ gameID
                        continue;
                    }
                    if(currGame == null) { // not in a game
                        this.send("\033[31m*** Must be in a game to use /SHOW scoreboard\033[0m");
                        continue;
                    }
                    this.displayScoreboard();
                    continue;
                }

                this.send("\033[31m*** /SHOW games, questions, scoreboard (gameID)\033[0m");
                continue;
            }
            /** JOIN **/
            if(input.toUpperCase().startsWith("/JOIN ")) { // user is requesting to join a game
                if(currGame != null) { // already in a game
                    this.send("\033[31m*** You must leave your current game before joining a new one. [[/LEAVE]]\033[0m");
                    continue;
                }
                input = input.replaceFirst("(?i)/JOIN ", ""); // remove command (case insensitive)
                if(input.equals("")) { // no ID entered
                    this.send("\033[31m*** /JOIN gameID\033[0m");
                    continue;
                }

                Game temp = master.joinGame(input, this.nick);
                if(temp instanceof ChallengeResponseGame) { // successfully joined game
                    currGame = (ChallengeResponseGame) temp;
                    this.send("\033[32m*** Joined " + input + "\033[0m");
                }
                else // failed to join game
                    this.send("\033[31m*** Invalid Game ID\033[0m");
                continue;
            }
            /** LEAVE **/
            if(input.toUpperCase().startsWith("/LEAVE")) // user is requseting to leave a game
                if(currGame == null) // not in a game
                    this.send("\033[31m*** You must join a game to leave a game. [[/JOIN ]]\033[0m");
                else {
                    this.send("\033[33m*** Left " + currGame.getId() + "\033[0m");
                    currGame = null; // "leave" game (player remains in scoreboard - just can't answer questions
                }
            /** ANSWER **/
            if(input.toUpperCase().startsWith("/ANSWER ")) { // user is requesting to answer a question
                if(currGame == null) { // not in a game
                    this.send("\033[31m*** You must be in a game to answer questions [[/JOIN]]\033[0m");
                    continue;
                }
                input = input.replaceFirst("(?i)/ANSWER ", ""); // remove command (case insensitive)
                if(input.equals("")) { // no ID entered
                    this.send("\033[31m*** /ANSWER questionID answer\033[0m");
                    continue;
                }

                String answer[] = input.split(" ", 2); // separate gameID and answer
                if(answer.length != 2 || answer[1].equals("")) { // didnt enter gameID & answer
                    this.send("\033[31m*** /ANSWER questionID answer\033[0m");
                    continue;
                }
                
                /* check if already answered/DNE
                 * (answer performs this check but its responce is the same as getting the question wrong) */
                ArrayList<Question> questions = currGame.getQuestions();
                Question theQuestion = null;
                for(Question q : questions)
                    if(q.equals(answer[0])) {
                        theQuestion = q;
                        break;
                    }
                if(theQuestion == null) {
                    this.send("\033[31m*** ERROR: Invalid Question\033[0m");
                    continue;
                } else if(theQuestion.isAnsweredBy(this.nick)) {
                    this.send("\033[31m*** ERROR: Question already answered.\033[0m");
                    continue;
                }
                
                int score = currGame.answer(this.nick, answer[0], answer[1]); // submit answer
                if(score == 0) // wrong answer
                    this.send("\033[31m*** Wrong answer\033[0m");
                else // correct answer
                    this.send("\033[32m*** Correct! " + score + " points awarded.\033[0m");
                continue;
            }
        }
    }
}
