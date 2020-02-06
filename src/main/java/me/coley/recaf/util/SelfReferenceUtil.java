package me.coley.recaf.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import me.coley.recaf.Recaf;

import static me.coley.recaf.util.Log.error;

/**
 * Utility for self-referencing the Recaf application as a file.
 *
 * @author Matt
 */
public class SelfReferenceUtil {
	private final File file;
	private final boolean isJar;

	private SelfReferenceUtil(File file) {
		this.file = file;
		this.isJar = file.getName().toLowerCase().endsWith(".jar");
	}

	/**
	 * @return File reference to self.
	 */
	public File getFile() {
		return file;
	}

	/**
	 * @return File path to self.
	 */
	public String getPath() {
		return file.getAbsolutePath();
	}

	/**
	 * @return Is the current executable context a jar file.
	 */
	public boolean isJar() {
		return isJar;
	}

	/**
	 * @return List of language files recognized.
	 */
	public List<Resource> getLangs() {
		return getFiles("translations/", ".json");
	}

	/**
	 * @return List of application-wide styles recognized.
	 */
	public List<Resource> getStyles() {
		return getFiles("style/ui-", ".css");
	}

	/**
	 * @return List of text-editor styles recognized.
	 */
	public List<Resource> getTextThemes() {
		return getFiles("style/text-", ".css");
	}

	/**
	 * @param prefix
	 *            File prefix to match.
	 * @param suffix
	 *            File suffix to match <i>(such as a file extension)</i>.
	 * @return List of matching files.
	 */
	private List<Resource> getFiles(String prefix, String suffix) {
		List<Resource> list = new ArrayList<>();
		if (isJar()) {
			// Read self as jar
			try (ZipFile file = new ZipFile(getFile())) {
				Enumeration<? extends ZipEntry> entries = file.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					// skip directories
					if (entry.isDirectory()) continue;
					String name = entry.getName();
					if (prefix != null && !name.startsWith(prefix))
						continue;
					if (suffix != null && !name.endsWith(suffix))
						continue;
					list.add(Resource.internal(name));
				}
			} catch (Exception ex) {
				error(ex, "Failed internal file (archive) lookup: {}", getFile());
			}
		} else {
			// Read self as file directory
			File dir = getFile();
			try {
				Files.walk(dir.toPath()).forEach(p -> {
					File file = dir.toPath().relativize(p).toFile();
					String path = file.getPath().replace('\\', '/');
					if (prefix != null && !path.startsWith(prefix))
						return;
					if (suffix != null && !path.endsWith(suffix))
						return;
					list.add(Resource.internal(path));
				});
			} catch(IOException ex) {
				error(ex, "Failed internal file (directory) lookup: {}", getFile());
			}
		}
		return list;
	}

	/**
	 * @return Recaf executable context.
	 * @throws URISyntaxException
	 *             Thrown if the file reference could not be resolved.
	 */
	public static SelfReferenceUtil get() throws URISyntaxException {
		CodeSource codeSource = Recaf.class.getProtectionDomain().getCodeSource();
		File selfFile = new File(codeSource.getLocation().toURI().getPath());
		return new SelfReferenceUtil(selfFile);
	}
}