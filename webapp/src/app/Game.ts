export interface Game {
  id: number;
  player1: Player.One;
  player2: Player.Two;
  winnner: Player;
  rounds: [];

}

export enum Player {
  One, Two
}
