package haexporterplugin;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class PluginVersionTest
{
	@Test
	public void testPluginVersionMatchesBuildGradle() throws IOException
	{
		// Gradle runs tests with the project directory as working directory
		String buildGradle = new String(Files.readAllBytes(Paths.get("build.gradle")), StandardCharsets.UTF_8);

		Matcher matcher = Pattern.compile("(?m)^\\s*version\\s*=\\s*['\"]([^'\"]+)['\"]").matcher(buildGradle);
		assertTrue("No `version = '...'` found in build.gradle", matcher.find());

		assertEquals("HAExporterPlugin.PLUGIN_VERSION must match the version in build.gradle",
			matcher.group(1), HAExporterPlugin.PLUGIN_VERSION);
	}
}
