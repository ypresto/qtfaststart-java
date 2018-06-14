package net.ypresto.qtfaststart;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;

public class QtFastStartFunctionalTest {

    static final String FAST_START_ENABLED_FILE_HASH = "b6ead8494587b9c0cb44f0e96ff853cc";

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
        assertEquals(FAST_START_ENABLED_FILE_HASH, getMD5Checksum(fastStartOutPath));
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
        assertEquals(FAST_START_ENABLED_FILE_HASH, getMD5Checksum(fastStartOutPath));
    }

    @Test
    public void checkForFastStartEnabledOnNonMovieDoesntThrowException() throws Exception {
        Path nonMoviePath = testFolder.getRoot().toPath().resolve("test-file.txt");
        Files.write(nonMoviePath, "Here is some text".getBytes(), StandardOpenOption.CREATE_NEW);

        assertTrue(Files.exists(nonMoviePath));

        boolean fastStartEnabled = QtFastStart.isFastStartEnabled(nonMoviePath.toFile());

        assertFalse(fastStartEnabled);
    }

    @Test
    public void attemptToFastStartOnNonMovieDoesntThrowException() throws Exception {
        Path nonMoviePath = testFolder.getRoot().toPath().resolve("test-file.txt");
        Path outPath = testFolder.getRoot().toPath().resolve("out.mov");
        Files.write(nonMoviePath, "Here is some text".getBytes(), StandardOpenOption.CREATE_NEW);

        assertTrue(Files.exists(nonMoviePath));

        boolean fastStartEnabled = QtFastStart.fastStart(nonMoviePath.toFile(), outPath.toFile());

        assertFalse(fastStartEnabled);
    }

    private static byte[] createChecksum(Path filePath) throws Exception {
        InputStream fis =  new FileInputStream(filePath.toFile());

        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;

        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);

        fis.close();
        return complete.digest();
    }

    private static String getMD5Checksum(Path filePath) throws Exception {
        byte[] b = createChecksum(filePath);
        StringBuilder buffer = new StringBuilder();

        for (int i=0; i < b.length; i++) {
            buffer.append(Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 ));
        }
        return buffer.toString();
    }
}
