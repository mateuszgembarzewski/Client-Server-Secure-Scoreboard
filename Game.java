import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * A generic game class.
 * @author leune
 *
 */
public class Game {
    protected HashMap<String, Integer> scores;  // scores per player
    protected String gameId;                    // identifier of game

    /**
     * Create a new game
     * @param id Identifier of the new game
     */
    public Game(String id) {
        scores = new HashMap<String, Integer>();
        gameId = id;
    }

    /**
     * Fetch the game identifier. 
     * @return 
     */
    public String getId() {
        return new String(gameId);
    }

    /**
     * Determine if game matched by identifier
     * @param id to match against
     * @return true on match, false otherwise
     */
    public boolean equals(String id) {
        return gameId.equals(id);
    }

    /**
     * Return the set of players in this game
     * @return Set of player names
     */
    public Set<String> getPlayers() {
        return scores.keySet();
    }

    /**
     * Add player with given name to this game
     * @param player Name of player
     */
    public void addPlayer(String player) {
        if (scores.containsKey(player)) 
        return;

        scores.put(player, 0);
        return;
    }

    /**
     * Get a score table
     * @return A HashMap with player names as key and score as value
     */
    public HashMap<String, Integer> getScores() {
        HashMap<String, Integer> out = new HashMap<String, Integer>();
        out.putAll(scores);
        return out;
    }
}

class ChallengeResponseGame extends Game {
    private ArrayList<Question> questions;

    /**
     * Retrieve the list of available questions
     * @return an ArrayList of Questions.
     */
    public ArrayList<Question> getQuestions() {
        ArrayList<Question> out = new ArrayList<Question>();
        out.addAll(questions);
        return out;
    }

    /**
     * Answers a question
     * 
     * @param player Name of the player
     * @param question Id of the question
     * @param answer Suggested answer
     * @return Number of points awarded.
     */
    public int answer(String player, String question, String answer) {
        if (!scores.containsKey(player)) return 0;
        // find corresponding question
        for (Question q : questions){
            if (q.equals(question)) {
                if (q.answer(player, answer)) {
                    if (scores.containsKey(player)) {
                        scores.replace(player, scores.get(player) + q.getPoints());
                    } else {
                        scores.put(player, q.getPoints());
                    }
                    return q.getPoints();
                } else {
                    return 0;
                }
            }
        }
        return 0;
    }

    /**
     * Add a question to the game
     */
    public void addQuestion(Question q) {
        questions.add(q);
    }

    /**
     * Constructor 
     */
    public ChallengeResponseGame(String id) {
        super(id);

        questions = new ArrayList<Question>();
    }
}

/**
 * Question class for challenge response game
 * @author leune
 *
 */
class Question {
    private String id;
    private String question;
    private String answer;
    private int points;
    private ArrayList<String> answeredBy;

    /**
     * Create a new question
     * @param id Question identifier
     * @param question text of question
     * @param answer correct answer
     * @param points points awarded if answered correctly
     */
    public Question(String id, String question, String answer, int points) {
        this.id = id;
        answeredBy = new ArrayList<String>();

        this.question = question;
        this.answer = answer;
        this.points = points;
    }

    /**
     * Fetch question text
     * @return String with text of question
     */
    public String getQuestion() {
        return question;
    }

    /**
     * Determine if question is answered correctly
     * @param answer given answer
     * @return
     */

    private boolean isAnswer(String answer) {
        return (this.answer.toUpperCase().trim()).equals(answer.toUpperCase().trim());
    }

    public int getPoints() {
        return points;
    }

    public boolean equals(String id) {
        return this.id.equals(id);
    }

    public boolean isAnsweredBy(String player) {
        return answeredBy.contains(player);
    }

    public boolean answer(String player, String answer) {
        if (isAnsweredBy(player)) return false;
        if (!isAnswer(answer)) return false;
        answeredBy.add(player);
        return true;
    }

    public String getId() {
        return id;
    }
}
