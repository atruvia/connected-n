import java.util.concurrent.TimeUnit;

import org.ase.fourwins.game.Game;
import org.ase.fourwins.tournament.TournamentListener;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;

public class InfluxDBListener implements TournamentListener {

  private InfluxDB influxDB;

  public InfluxDBListener(InfluxDB influxDB) {
    this.influxDB = influxDB;
  }

  @Override
  public void gameEnded(Game game) {
    Point point = Point.measurement("GAMES")
        .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
        .addField("player_id", "P1")
        .addField("value", 1L)
        .build();
    influxDB.write("GAMES", "default", point);
  }

}
