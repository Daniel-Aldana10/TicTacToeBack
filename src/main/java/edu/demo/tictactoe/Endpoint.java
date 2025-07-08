package edu.demo.tictactoe;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

@Component
@ServerEndpoint("/tttService")
public class Endpoint{
    private static final Logger logger = Logger.getLogger(Endpoint.class.getName());
    private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());
    private static Session playerX = null;
    private static Session playerO = null;
    private static String[] board = new String[9];
    private static List<Snapshot> history = new ArrayList<>();

    private static class Snapshot {
        String[] boardState;
        String turn;

        Snapshot(String[] boardState, String turn) {
            this.boardState = Arrays.copyOf(boardState, boardState.length);
            this.turn = turn;
        }
    }

    private static String currentTurn = "X";
    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        logger.info("New connection: " + session.getId());

        try {
            String assigned = assignPlayer(session);
            session.getBasicRemote().sendText("{\"type\":\"assign\",\"symbol\":\"" + assigned + "\"}");
            session.getBasicRemote().sendText(buildBoardMessage());
        } catch (IOException e) {
            logger.warning("Error assigning player: " + e.getMessage());
        }
    }
    @OnMessage
    public void onMessage(String msg, Session session) {
        try {
            Map<String, String> parsed = parseMessage(msg);
            String type = parsed.get("type");

            switch (type) {
                case "move":
                    handleMove(parsed, session);
                    break;
                case "reset":
                    resetGame();
                    break;

                case "jumpTo":
                    jumpTo(Integer.parseInt(parsed.get("index")));
                    break;

                default:
                    logger.warning("Unknown message type: " + type);
            }
        } catch (Exception e) {
            logger.warning("Error processing message: " + e.getMessage());
        }
    }
    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        if (session.equals(playerX)) playerX = null;
        if (session.equals(playerO)) playerO = null;
        logger.info("Connection closed: " + session.getId());
    }
    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.warning("WebSocket error: " + throwable.getMessage());
    }
    private void jumpTo(int index) throws IOException {
        if (index >= 0 && index < history.size()) {
            Snapshot snapshot = history.get(index);
            board = Arrays.copyOf(snapshot.boardState, snapshot.boardState.length);
            currentTurn = snapshot.turn;

            history = new ArrayList<>(history.subList(0, index + 1));

            broadcast(buildBoardMessage());
        }
    }


    private void handleMove(Map<String, String> data, Session sender) throws IOException {
        int index = Integer.parseInt(data.get("index"));
        String player = data.get("player");

        if (!isPlayerTurn(sender, player)) return;
        if (index < 0 || index >= 9 || board[index] != null) return;

        board[index] = player;
        currentTurn = player.equals("X") ? "O" : "X";


        history.add(new Snapshot(board, currentTurn));

        broadcast(buildBoardMessage());
    }


    private void resetGame() throws IOException {
        board = new String[9];
        history.clear();
        currentTurn = "X";
        broadcast(buildBoardMessage());
    }

    private String assignPlayer(Session session) {
        if (playerX == null) {
            playerX = session;
            return "X";
        } else if (playerO == null) {
            playerO = session;
            return "O";
        } else {
            return ""; // Spectator or ignored
        }
    }

    private boolean isPlayerTurn(Session session, String player) {
        if (player.equals("X") && !session.equals(playerX)) return false;
        if (player.equals("O") && !session.equals(playerO)) return false;
        return player.equals(currentTurn);
    }

    private void broadcast(String msg) throws IOException {
        for (Session s : sessions) {
            if (s.isOpen()) {
                s.getBasicRemote().sendText(msg);
            }
        }
    }

    private String buildBoardMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"update\",\"board\":[");

        for (int i = 0; i < board.length; i++) {
            sb.append(board[i] == null ? "null" : "\"" + board[i] + "\"");
            if (i < board.length - 1) sb.append(",");
        }

        sb.append("],\"turn\":\"").append(currentTurn).append("\",");
        sb.append("\"history\":[");

        for (int i = 0; i < history.size(); i++) {
            Snapshot snap = history.get(i);
            sb.append("[");
            for (int j = 0; j < snap.boardState.length; j++) {
                sb.append(snap.boardState[j] == null ? "null" : "\"" + snap.boardState[j] + "\"");
                if (j < snap.boardState.length - 1) sb.append(",");
            }
            sb.append("]");
            if (i < history.size() - 1) sb.append(",");
        }

        sb.append("]}");

        return sb.toString();
    }


    private Map<String, String> parseMessage(String json) {
        Map<String, String> map = new HashMap<>();
        json = json.replaceAll("[{}\"]", "");
        for (String part : json.split(",")) {
            String[] pair = part.split(":");
            if (pair.length == 2) {
                map.put(pair[0].trim(), pair[1].trim());
            }
        }
        return map;
    }

}

