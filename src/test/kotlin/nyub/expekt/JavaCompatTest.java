package nyub.expekt;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.Consumer;
import kotlin.Unit;
import org.junit.jupiter.api.Test;

public class JavaCompatTest {
  private final ExpectTests e =
      new ExpectTests(
          Paths.get("src/test/kotlin"),
          System.getProperty("nyub.expekt.promote", "false").equals("true"));

  @Test
  void happyPath() {
    e.expectTest(
        t -> {
          final var sine = sineWave(0, 100, 5, 0.2);
          printValues(sine, t::print);
          t.expect(
              """
    ^      +++++++                         ++++++                         ++++++                         ++
    |     +       +                      ++      +                       +      ++                      +
    |    +         +                    +         ++                   ++         +                    +
    |   +           +                  +            +                 +            +                  +
    | ++             +                +              +               +              +                +
    |                 +              +                +             +                +              +
    |                  +            +                  +           +                  +            +
    |                   +          +                    +         +                    +          +
    |                    ++      ++                      +       +                      ++      ++
    |                      ++++++                         +++++++                         ++++++
    o --------------------------------------------------------------------------------------------------->
    """);
          return Unit.INSTANCE;
        });
  }

  void printValues(int[] values, Consumer<String> print) {
    final var maxValue = Arrays.stream(values).max().orElse(0);
    for (int i = maxValue; i >= 0; i--) {
      if (i == maxValue) print.accept("^ ");
      else print.accept("| ");
      for (int value : values) {
        if (value == i) print.accept("+");
        else print.accept(" ");
      }
      print.accept("\n");
    }
    for (int j = 0; j < values.length; j++) {
      if (j == values.length - 1) print.accept(">");
      else if (j == 0) print.accept("o ");
      else print.accept("-");
    }
  }

  int[] sineWave(int from, int to, int factor, double step) {
    final var result = new int[to - from + 1];
    for (int x = from; x <= to; x++) {
      double y = (Math.sin(x * step) + 1) * factor;
      result[x - from] = (int) y;
    }
    return result;
  }
}
