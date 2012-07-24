package jp.vmi.selenium.selenese;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.net.URL;

import jp.vmi.selenium.selenese.command.Command;
import jp.vmi.selenium.selenese.command.CommandFactory;
import jp.vmi.selenium.webdriver.WebDriverFactory;

import org.apache.commons.lang.time.StopWatch;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverCommandProcessor;

import com.thoughtworks.selenium.SeleniumException;

public abstract class CommandRunnerTest {

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    protected final FilenameFilter pngFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".png");
        }
    };

    protected abstract WebDriverFactory getWebDriverFactory() throws IllegalArgumentException;

    protected static String getScriptName(String name) {
        Class<CommandRunnerTest> c = CommandRunnerTest.class;
        String html = "/" + c.getCanonicalName().replace('.', '/') + name + ".html";
        URL resource = c.getResource(html);
        if (resource == null) {
            throw new RuntimeException(new FileNotFoundException(html));
        }
        return c.getResource(html).toString();
    }

    @Test
    public void testSimple() throws IllegalArgumentException {
        String script = getScriptName("Simple");
        Runner runner = new Runner(getWebDriverFactory());
        runner.setScreenshotDir(tmpDir.getRoot());
        runner.setScreenshotAll(true);
        runner.run(script);

        assertEquals(5, tmpDir.getRoot().listFiles(pngFilter).length);
    }

    @Test
    public void testFailSubmit() throws IllegalArgumentException {
        String script = getScriptName("Error");
        Runner runner = new Runner(getWebDriverFactory());
        runner.setScreenshotDir(tmpDir.getRoot());
        runner.setScreenshotAll(true);
        runner.run(script);

        assertEquals(3, tmpDir.getRoot().listFiles(pngFilter).length);
    }

    @Test
    public void testAssertFail() throws IllegalArgumentException {
        String script = getScriptName("AssertFail");
        Runner runner = new Runner(getWebDriverFactory());
        runner.setScreenshotDir(tmpDir.getRoot());
        runner.setScreenshotAll(true);
        runner.run(script);

        assertEquals(5, tmpDir.getRoot().listFiles(pngFilter).length);
    }

    @Test
    public void testFlowControl() throws IllegalArgumentException {
        execute(getScriptName("FlowControl"));

        assertEquals(28, tmpDir.getRoot().listFiles(pngFilter).length);
    }

    @Test
    public void testForEach() throws IllegalArgumentException {
        execute(getScriptName("ForEach"));

        assertEquals(18, tmpDir.getRoot().listFiles(pngFilter).length);
    }

    protected void execute(String scriptName) {
        Runner runner = new Runner(getWebDriverFactory());
        runner.setScreenshotDir(tmpDir.getRoot());
        runner.setScreenshotAll(true);
        runner.run(scriptName);
    }

    @Test
    public void noCommandSelenese() throws IllegalArgumentException {
        execute(getScriptName("NoCommand"));

        assertEquals(0, tmpDir.getRoot().listFiles(pngFilter).length);
    }

    @Test(expected = SeleniumException.class)
    public void invalidCommandInHtml() throws IllegalArgumentException {
        String script = getScriptName("InvalidCommand");
        Runner runner = new Runner(getWebDriverFactory());
        runner.run(script);
    }

    @Test(expected = SeleniumException.class)
    public void invalidCommand() throws IllegalArgumentException {
        WebDriver driver = getWebDriverFactory().get();
        WebDriverCommandProcessor proc = new WebDriverCommandProcessor("", driver);
        CommandFactory commandFactory = new CommandFactory(proc);
        Command invalidCommand = commandFactory.newCommand(1, "invalidCommand");
        Runner runner = new Runner(driver);
        Context context = new Context(proc);
        runner.run(context, invalidCommand);
    }

    @Test
    public void capture() throws IllegalArgumentException {
        final String filename = "test.png";
        File pngFile = new File(tmpDir.getRoot(), filename);
        if (pngFile.exists()) {
            pngFile.delete();
        }

        WebDriver driver = getWebDriverFactory().get();
        WebDriverCommandProcessor proc = new WebDriverCommandProcessor("", driver);
        CommandFactory commandFactory = new CommandFactory(proc);
        Command captureCommand = commandFactory.newCommand(1, "captureEntirePageScreenshot", pngFile.getAbsolutePath());
        Runner runner = new Runner(driver);
        Context context = new Context(proc);
        runner.run(context, captureCommand);

        if (driver instanceof TakesScreenshot) {
            assertTrue(pngFile.exists());
        }
    }

    @Test
    public void pauseCommand() throws IllegalArgumentException {
        WebDriver driver = getWebDriverFactory().get();
        WebDriverCommandProcessor proc = new WebDriverCommandProcessor("",
            driver);
        CommandFactory commandFactory = new CommandFactory(proc);
        Command pause = commandFactory.newCommand(1, "pause", "5000");
        Runner runner = new Runner(driver);
        Context context = new Context(proc);

        StopWatch sw = new StopWatch();
        sw.start();
        runner.run(context, pause);
        sw.stop();
        assertThat(sw.getTime(), is(greaterThan(5000L)));
    }
}
