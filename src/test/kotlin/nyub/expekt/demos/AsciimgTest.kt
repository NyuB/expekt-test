package nyub.expekt.demos

import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.div
import kotlin.math.abs
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class AsciimgTest {
    private val asciimg = AsciiImg(mapOf('■' to 0, '□' to 255), 1)

    @Test
    fun `ascii to image`(@TempDir tmp: Path) = expectTest {
        val outputPath = tmp / "img.tiff"
        asciimg.asciiToImage(
            """
                ■■■■■
                ■□□□■
                ■□□□■
                ■■■■■
                """
                .trimIndent(),
            outputPath,
        )

        print(asciimg.imageToAscii(outputPath))
        expect(
            """
        ■■■■■
        ■□□□■
        ■□□□■
        ■■■■■
        """
                .trimIndent()
        )
    }

    @Test
    fun `multiple pixels per character`(@TempDir tmp: Path) = expectTest {
        val outputPath = tmp / "img_x3.tiff"
        asciimg
            .copy(pixelPerChar = 3)
            .asciiToImage(
                """
        ■■■■■
        ■□□□■
        ■□□□■
        ■■■■■
        """
                    .trimIndent(),
                outputPath,
            )

        asciimg
            .imageToAscii(outputPath)
            .expect(
                """
        ■■■■■■■■■■■■■■■
        ■■■■■■■■■■■■■■■
        ■■■■■■■■■■■■■■■
        ■■■□□□□□□□□□■■■
        ■■■□□□□□□□□□■■■
        ■■■□□□□□□□□□■■■
        ■■■□□□□□□□□□■■■
        ■■■□□□□□□□□□■■■
        ■■■□□□□□□□□□■■■
        ■■■■■■■■■■■■■■■
        ■■■■■■■■■■■■■■■
        ■■■■■■■■■■■■■■■
        """
                    .trimIndent()
            )
    }

    @Test
    fun chessboard(@TempDir tmp: Path) = expectTest {
        val outputImage = tmp / "chessboard.tiff"
        asciimg
            .copy(charToGrayLevel = mapOf('B' to 0, 'W' to 255))
            .asciiToImage(List(8) { if (it % 2 == 0) "WBWBWBWB" else "BWBWBWBW" }, outputImage)

        asciimg
            .imageToAscii(outputImage)
            .expect(
                """
        □■□■□■□■
        ■□■□■□■□
        □■□■□■□■
        ■□■□■□■□
        □■□■□■□■
        ■□■□■□■□
        □■□■□■□■
        ■□■□■□■□
        """
                    .trimIndent()
            )
    }

    data class AsciiImg(private val charToGrayLevel: Map<Char, Int>, private val pixelPerChar: Int) {
        fun asciiToImage(s: String, path: Path) {
            asciiToImage(s.split("\n"), path)
        }

        fun asciiToImage(ascii: List<String>, path: Path) {
            require(ascii.isNotEmpty())
            val rows = ascii.size
            val columns = ascii.first().length
            require(ascii.all { it.length == columns }) { "All lines must be of same length" }
            val result = BufferedImage(columns * pixelPerChar, rows * pixelPerChar, BufferedImage.TYPE_BYTE_GRAY)
            repeat(columns) { x ->
                repeat(rows) { y ->
                    val char = ascii[y][x]
                    val lvl = charToGrayLevel[char]!!
                    result.raster.setPixels(
                        x * pixelPerChar,
                        y * pixelPerChar,
                        pixelPerChar,
                        pixelPerChar,
                        IntArray(pixelPerChar * pixelPerChar) { lvl },
                    )
                }
            }
            ImageIO.write(result, "tiff", path.toFile())
        }

        fun imageToAscii(imagePath: Path): String {
            val img = ImageIO.read(imagePath.toFile())
            val result = StringBuilder()
            repeat(img.height) { y ->
                repeat(img.width) { x ->
                    val pixel = img.raster.getPixel(x, y, null as IntArray?)[0] // assumes gray level
                    val char = charToGrayLevel.minBy { (_, lvl) -> abs(lvl - pixel) }.key
                    result.append(char)
                }
                result.append("\n")
            }
            return result.toString()
        }
    }
}
