package nyub.expekt;

import nyub.expekt.junit.ExpectTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ExpectTestExtension.class)
@ExpectTestExtension.RootPath("src/test/kotlin")
class FileOffsetsTest {
  @Test
  @ExpectTestExtension.Promote(true)
  void multiPromotionLineProblem(ExpectTests.ExpectTest t) {
    for (int i = 0; i < 10; i++) {
      t.printf("[A]: %d", i);
      if (i != 9) t.newLine();
    }
    t.expect(
        """
            [A]: 0
            [A]: 1
            [A]: 2
            [A]: 3
            [A]: 4
            [A]: 5
            [A]: 6
            [A]: 7
            [A]: 8
            [A]: 9
            """);
    for (int i = 0; i < 10; i++) {
      t.printf("[B]: %d", i);
      if (i != 9) t.newLine();
    }
    t.expect(
        """
            [B]: 0
            [B]: 1
            [B]: 2
            [B]: 3
            [B]: 4
            [B]: 5
            [B]: 6
            [B]: 7
            [B]: 8
            [B]: 9
            """);
  }
}
