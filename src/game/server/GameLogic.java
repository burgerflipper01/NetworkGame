package game.server;

import game.client.Generel;
import game.client.Gui;
import game.server.model.Pair;
import game.server.model.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;


public class GameLogic {
    private static GameLogic instance;

    private final static int POINTS_TO_WIN = 100;
    // Add points for each action
    private final static int TREASURE_POINTS = 10;
    private final static int PLAYER_KILL_POINTS = 5;

    // Remove points for each action
    private final static int PLAYER_DEATH_POINTS = 5;
    private final static int WALL_POINTS = 1;

    private GameLogic() {
        instance = this;
        treasureSpawner();
    }

    public static GameLogic getInstance() {
        return Objects.requireNonNullElseGet(instance, GameLogic::new);
    }

    private List<Player> players = new ArrayList<Player>();

    private Pair treasure = null;

    public Player makePlayers(String name) {
        Pair p = getRandomFreePosition();
        Player me = new Player(name, p, "up");
        players.add(me);
        sendState();
        return me;
    }

    // finds a random new position which is not wall
    // and not occupied by other players
    public Pair getRandomFreePosition() {
        int x = 1;
        int y = 1;
        boolean foundFreePos = false;
        while (!foundFreePos) {
            Random r = new Random();
            x = Math.abs(r.nextInt() % 18) + 1;
            y = Math.abs(r.nextInt() % 18) + 1;
            // er det gulv ?
            if (Generel.board[y].charAt(x) == ' ') {
                foundFreePos = true;
                for (Player p : players) {
                    if (p.getXpos() == x && p.getYpos() == y) //pladsen optaget af en anden
                        foundFreePos = false;
                }

            }
        }
        Pair p = new Pair(x, y);
        return p;
    }

    public void updatePlayer(Player me, int deltaX, int deltaY, String direction) {
        me.setDirection(direction);
        int x = me.getXpos(), y = me.getYpos();

        if (isTreasure(x + deltaX, y + deltaY)) {
            me.addPoints(TREASURE_POINTS);
            treasure = null;
        }

        if (isWall(x + deltaX, y + deltaY)) {
            me.addPoints(WALL_POINTS);
        } else {
            // collision detection
            Player other = getPlayerAt(x + deltaX, y + deltaY);

            if (other != null) {
                me.addPoints(PLAYER_KILL_POINTS);
                //update the other player
                other.takePoints(PLAYER_DEATH_POINTS);
                Pair pa = getRandomFreePosition();

                other.setLocation(pa);
            }

            Pair newpos = new Pair(x + deltaX, y + deltaY);
            me.setLocation(newpos);
        }

        sendState();

        if (hasWon(me)) {
            ClientHandler.sendStateToAll("WINNER:" + me.getName());
        }
    }

    private boolean isTreasure(int i, int i1) {
        return treasure != null && treasure.getX() == i && treasure.getY() == i1;
    }

    private synchronized void sendState() {
        StringBuilder state = new StringBuilder();
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            String serializedPlayer = p.serializePlayer();

            if (i == players.size() - 1)
                state.append(serializedPlayer);
            else
                state.append(serializedPlayer).append(",");
        }

        if (treasure != null) {
            state.append(";");
            state.append(treasure.getX()).append(":").append(treasure.getY());
        }

        ClientHandler.sendStateToAll("state:" + state);
    }

    public Player getPlayerAt(int x, int y) {
        for (Player p : players) {
            if (p.getXpos() == x && p.getYpos() == y) {
                return p;
            }
        }
        return null;
    }

    public static boolean isWall(int x, int y) {
        return Generel.board[y].charAt(x) == 'w';
    }

    public void removePlayer(Player player) {
        players.remove(player);
        sendState();
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void treasureSpawner() {
        new Thread(() -> {
            while (true) {
                try {
                    int min = 3, max = 6;
                    int random = (int) (Math.random() * (max - min + 1) + min);
                    Thread.sleep(1000L * random);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (treasure == null) {
                    treasure = getRandomFreePosition();
                    sendState();
                }
            }
        }).start();
    }

    private boolean hasWon(Player player) {
        return player.getPoints() >= POINTS_TO_WIN;
    }
}
