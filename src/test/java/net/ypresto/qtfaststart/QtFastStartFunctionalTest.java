package net.ypresto.qtfaststart;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class QtFastStartFunctionalTest {

    static Path testFilePath;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    public static void setupBeforeClass() {
        QtFastStart.sDEBUG = true;
    }

    @Before
    public void setupBeforeTest() {
        testFilePath = testFolder.getRoot().toPath().resolve("test-movie.mp4");

        boolean successUnpackingTestMovie;

        try(InputStream testMovieStream =
                QtFastStartFunctionalTest.class.getClassLoader().getResourceAsStream("test-movie.mp4")) {
            Files.copy(testMovieStream, testFilePath);

            assert(Files.exists(testFilePath));

            successUnpackingTestMovie = true;
        }
        catch (Exception e) {
            successUnpackingTestMovie = false;

            System.out.printf("Failed to unpack the test movie due to %s - %s", e.getClass().getSimpleName(),
                    e.getMessage());
        }

        assertTrue(successUnpackingTestMovie);
    }

    @Test
    public void testQtFastStartOnNewMovie() throws Exception {
        Path fastStartOutPath = testFolder.getRoot().toPath().resolve("qt-faststart-enabled.mp4");

        boolean success = QtFastStart.fastStart(testFilePath.toFile(), fastStartOutPath.toFile());

        assertTrue(success);
        assertTrue(Files.exists(fastStartOutPath));
    }

    @Test
    public void testQtFastStartOnQtFastStartEnabledMovie() throws Exception {
        Path fastStartOutPath = testFolder.getRoot().toPath().resolve("qt-faststart-enabled.mp4");
        Path secondRunOutPath = testFolder.getRoot().toPath().resolve("second-run.mp4");

        boolean firstRunSuccess = QtFastStart.fastStart(testFilePath.toFile(), fastStartOutPath.toFile());

        assertTrue(firstRunSuccess);
        assertTrue(Files.exists(fastStartOutPath));

        boolean secondRunSuccess = QtFastStart.fastStart(fastStartOutPath.toFile(), secondRunOutPath.toFile());

        assertFalse(secondRunSuccess);
        assertFalse(Files.exists(secondRunOutPath));
    }

    @Test
    public void testIsFastStartEnabledOnNonFastStartFile() throws Exception {
        boolean isFastStartEnabled = QtFastStart.isFastStartEnabled(testFilePath.toFile());

        assertFalse(isFastStartEnabled);
    }

    @Test
    public void testIsFastStartEnabledOnNonFastStartFileThenEnableItAndCheck() throws Exception {
        Path fastStartOutPath = testFolder.getRoot().toPath().resolve("qt-faststart-enabled.mp4");

        boolean isFastStartEnabled = QtFastStart.isFastStartEnabled(testFilePath.toFile());

        assertFalse(isFastStartEnabled);

        boolean fastStartEnabled = QtFastStart.fastStart(testFilePath.toFile(), fastStartOutPath.toFile());

        assertTrue(fastStartEnabled);
        assertTrue(Files.exists(fastStartOutPath));

        boolean afterEnableCheck = QtFastStart.isFastStartEnabled(fastStartOutPath.toFile());

        assertTrue(afterEnableCheck);
    }
}
