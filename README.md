# Tic Tac Toe WebSocket Backend

This is a Java-based WebSocket backend for a real-time multiplayer Tic Tac Toe game. It is built using **Jakarta WebSocket API** and **Spring Boot**, and manages player sessions, game state, turn handling, history tracking, and move synchronization across connected clients.

## Features

- Real-time communication via WebSockets
- Two-player symbol assignment (`X` and `O`)
- Spectator support (read-only clients)
- Game state broadcast to all connected clients
- Turn-based move validation
- Game reset and undo
- Full move history with ability to jump to a specific state and continue playing from there

---

## Endpoint

### WebSocket URL

ws://localhost:8080/tttService



### Message Types

#### `assign`
Sent to the client upon connection to assign a symbol (`X`, `O`, or `""` for spectators).

```json
{
  "type": "assign",
  "symbol": "X"
}
```
update
Broadcasted to all clients when the board state changes.
```json

{
  "type": "update",
  "board": ["X", "O", null, ...],
  "turn": "O",
  "history": [
    [null, null, ...],
    ["X", null, ...],
    ...
  ]
}
```
Incoming Message Types
Make a move
```json
{
  "type": "move",
  "index": 4,
  "player": "X"
}
```

Reset the game
```json
{
  "type": "reset"
}
```
Jump to a specific move in history
```json
{
  "type": "jumpTo",
  "index": 2
}
```
# Code Structure
- Endpoint.java: Main WebSocket endpoint class

 - Manages player sessions

- Handles message parsing and dispatch

- Stores game state in board

- Tracks move history via snapshots

- Implements time travel logic

# Prerequisites
- Java 17+

- Maven

- Git
# Build
```bash

mvn clean package
```
# Run
```bash
mvn spring-boot:run
```
The server will start on localhost:8080.