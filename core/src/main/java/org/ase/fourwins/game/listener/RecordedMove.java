package org.ase.fourwins.game.listener;

import lombok.Value;

@Value
public class RecordedMove {
  private Integer column;
  private String token;
}
