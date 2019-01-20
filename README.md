# Client-Server-Secure-Scoreboard
A distributed scoring system. Including a client, a server, and a protocol.


Documentation

PROJECT TITLE: Client/Server Secure Scoreboard

PURPOSE OF PROJECT: Networking Final Project

VERSION or DATE: V1.1 - May 10th 2018

HOW TO START THIS PROJECT: Run the java program then use 'ncat --ssl localhost 4001' on an Ubuntu Linux machine to connect.

AUTHORS: Mateusz Gembarzewski and John Zaino

USER INSTRUCTIONS:
First run main.java and connect to the server on port 4001 using the command 'ncat --ssl localhost 4001'. 
(or replace localhost with the ip/domain name if connecting from a separate location)

If the connection is successful, you will receive a 'Welcome' message(motd) and now you have a choice of which command you
would like to run. This is part of our own protocol where we have set commands to interact with the server.

COMMANDS:

* /REGISTER password
* /LOGIN nickname password
* /JOIN gameID
* /LEAVE
* /NICK nickname
* /SHOW games|questions|scoreboard (gameID)
* /ANSWER questionID answer

The /REGISTER command allows one to register their current nickname with a password so they can save their progess and continue later.
    - Neither the nickname nor the password can be changed after registering in the current version (v1.1).
The /LOGIN command allows a user to login with their nickname and password they set.
    - The user must enter their registered nickname and password, there is no logout feature in this current version (v1.1).
    - If users wish to logout they must terminate the connection and reconnect.
The /JOIN command allows a user to join one of the pre-set quiz games.
    - Users must enter the gameID of the game they wish to join.
    - Users can only join one game at a time, to join another game they must first leave the current one (/LEAVE).
The /LEAVE command leaves a game that a user is currently in.
The /NICK command allows a user to set themselves a nickname.
    - Users must enter their desired nickname which cannot contain spaces.
    - Users cannot set their nick to any nickname that has already been registered or is currently used by another client.
    - Clients cannot change nicknames while in a game.
    - Scores are tied to the nickname in this current version (v1.1), thus changing nicknames allows another user to take the nickname and its scores (unless it's registered).
The /SHOW command, depending on the parameter given, shows the games, questions, or scoreboard.
    - Acceptable Parameters:
        - games - displays the list of games.
        - questions - displays the list of questions for the current game (must be in a game).
            - Optional: additional parameter gameID to display the questions for a specific game (does not need to be in a/the game to display questions via gameID).
        - scoreboard - displays the scoreboard for the current game (must be in a game).
            - Optional: additional parameter gameID to display the scoreboard for a specific game (does not need to be in a game                                to display the scoreboard via gameID).
The /ANSWER command allows the user to attempt to answer a question, it requires a question id and the answer itself.
    - Must be in the game that the client is trying to answer a question for.
    - Questions can only be answered once.
