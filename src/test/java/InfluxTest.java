import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.ase.fourwins.board.Board.GameState;
import org.ase.fourwins.board.Board.Score;
import org.ase.fourwins.game.Game;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InfluxTest {
  private static final String DBNAME = "GAMES";
  private InfluxDB influxDB;

  @BeforeEach
  public void setup() {
    influxDB = InfluxDBFactory.connect("http://localhost:8086", "root", "root");
    influxDB.query(new Query("CREATE DATABASE " + DBNAME, DBNAME));
    influxDB.setDatabase(DBNAME);
  }

  @AfterEach
  public void tearDown() {
    influxDB.query(new Query("DROP DATABASE \"" + DBNAME + "\"", DBNAME));
  }

  @Test
  void testInfluxDBIsPingable() {
    assertThat(influxDB.ping()
        .isGood(), is(true));
  }

  @Test
  void testOneGameEndingIsInsertedToInfluxDB() {
    InfluxDBListener listener = new InfluxDBListener(influxDB);

    Game game = new Game() {

      @Override
      public Game runGame() {
        return null;
      }

      @Override
      public GameState gameState() {
        return GameState.builder()
            .token("P1")
            .score(Score.WIN)
            .build();
      }
    };
    listener.gameEnded(game);

    QueryResult query = influxDB.query(new Query("SELECT * FROM " + DBNAME, DBNAME));

    query.getResults()
        .forEach(System.out::println);


  }

}
