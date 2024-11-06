package ch.njol.skript.config;

import ch.njol.skript.Skript;
import ch.njol.skript.config.validate.SectionValidator;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a config file.
 */
public class Config implements Comparable<Config> {

	/**
	 * One level of the indentation, e.g. a tab or 4 spaces.
	 */
	private String indentation = "\t";
	/**
	 * The indentation's name, i.e. 'tab' or 'space'.
	 */
	private String indentationName = "tab";

	/**
	 * TODO @Moderocky what does this do
	 */
	boolean simple;
	final String defaultSeparator;
	String separator;
	int level = 0;

	/**
	 * The main section of the config.
	 */
	private final SectionNode main;

	/**
	 * Whether this config allows empty sections.
	 */
	final boolean allowEmptySections;

	/**
	 * The name of the file this config is loaded from.
	 */
	String fileName;

	/**
	 * The path of the file this config is loaded from.
	 */
	@Nullable Path file = null;

	public Config(InputStream source, String fileName, @Nullable File file,
				  boolean simple, boolean allowEmptySections, String defaultSeparator) throws IOException {
		try (source) {
			this.fileName = fileName;
			if (file != null) // Must check for null before converting to path
				this.file = file.toPath();
			this.simple = simple;
			this.allowEmptySections = allowEmptySections;
			this.defaultSeparator = defaultSeparator;
			separator = defaultSeparator;

			if (source.available() == 0) {
				main = new SectionNode(this);
				Skript.warning("'" + getFileName() + "' is empty");
				return;
			}

			if (Skript.logVeryHigh())
				Skript.info("loading '" + fileName + "'");

			try (ConfigReader reader = new ConfigReader(source)) {
				main = SectionNode.load(this, reader);
			}
		}
	}

	public Config(InputStream source, String fileName, boolean simple,
				  boolean allowEmptySections, String defaultSeparator) throws IOException {
		this(source, fileName, null, simple, allowEmptySections, defaultSeparator);
	}

	public Config(File file, boolean simple, boolean allowEmptySections,
				  String defaultSeparator) throws IOException {
		this(Files.newInputStream(file.toPath()), file.getName(), simple,
			allowEmptySections, defaultSeparator);
		this.file = file.toPath();
	}

	public Config(@NotNull Path file, boolean simple, boolean allowEmptySections,
				  String defaultSeparator) throws IOException {
		this(Channels.newInputStream(FileChannel.open(file)), "" + file.getFileName(), simple, allowEmptySections, defaultSeparator);
		this.file = file;
	}

	/**
	 * Saves the config to a file.
	 *
	 * @param file The file to save to
	 * @throws IOException If the file could not be written to.
	 */
	public void save(File file) throws IOException {
		separator = defaultSeparator;
		PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8);
		try {
			main.save(writer);
		} finally {
			writer.flush();
			writer.close();
		}
	}

	/**
	 * @deprecated Sets all values in the config, which may affect sensitive values that have not and
	 * should not be changed. Use {@link #updateKeys(Config)}} instead.
	 */
	@Deprecated
	public boolean setValues(Config other) {
		return getMainNode().setValues(other.getMainNode());
	}

	/**
	 * @deprecated Sets all values in the config, which may affect sensitive values that have not and
	 * should not be changed. Use {@link #updateKeys(Config)}} instead.
	 */
	@Deprecated
	public boolean setValues(Config other, String... excluded) {
		return getMainNode().setValues(other.getMainNode(), excluded);
	}

	/**
	 * Updates the keys of this config with the keys of another config.
	 * Used for updating a config file to a newer version.
	 * This method only sets keys that are missing in this config, thus preserving any existing values.
	 *
	 * @param newer The newer config to update from.
	 * @return True if any keys were added to this config, false otherwise.
	 */
	public boolean updateKeys(Config newer) {
		Set<String> newKeys = findKeys(newer.getMainNode());
		Set<String> oldKeys = findKeys(getMainNode());

		newKeys.removeAll(oldKeys);
		Set<String> missingKeys = Set.copyOf(newKeys);

		if (missingKeys.isEmpty()) {
			return false;
		}

		for (String key : missingKeys) {
			String value = newer.getByPath(key);

			if (value == null) {
				continue;
			}

			getMainNode().set(key, value);
		}
		return true;
	}

	/**
	 * Recursively finds all keys in a section node.
	 * <p>
	 *     Keys are represented in dot notation, e.g. {@code grandparent.parent.child}.
	 * </p>
	 * @param node The parent node to search.
	 * @return A set of the discovered keys.
	 */
	@Contract(pure = true)
	private Set<String> findKeys(SectionNode node) {
		Set<String> keys = new HashSet<>();

		String key;
		if (node.getParent() != null) {
			key = node.getKey() + ".";
		} else {
			key = "";
		}

		for (Node child : node) {
			if (child instanceof SectionNode sectionNode) {
				keys.addAll(findKeys(sectionNode));
			} else if (child instanceof EntryNode entryNode) {
				keys.add(key + entryNode.getKey());
			}
		}
		return keys;
	}

	/**
	 * Compares the keys and values of this Config and another.
	 * @param other The other Config.
	 * @param excluded Keys to exclude from this comparison.
	 * @return True if there are differences in the keys and their values
	 *  of this Config and the other Config.
	 */
	public boolean compareValues(Config other, String... excluded) {
		return getMainNode().compareValues(other.getMainNode(), excluded);
	}

	@Nullable
	public File getFile() {
		if (file != null) {
			try {
				return file.toFile();
			} catch (Exception e) {
				return null; // ZipPath, for example, throws undocumented exception
			}
		}
		return null;
	}

	@Nullable
	public Path getPath() {
		return file;
	}

	/**
	 * @return The most recent separator. Only useful while the file is loading.
	 */
	public String getSeparator() {
		return separator;
	}

	/**
	 * @return A separator string useful for saving, e.g. ": " or " = ".
	 */
	public String getSaveSeparator() {
		if (separator.equals(":"))
			return ": ";
		if (separator.equals("="))
			return " = ";
		return " " + separator + " ";
	}

	/**
	 * Splits the given path at the dot character and passes the result to {@link #get(String...)}.
	 *
	 * @param path The path to get the value from.
	 * @return <tt>get(path.split("\\."))</tt>
	 */
	@Nullable
	public String getByPath(String path) {
		return get(path.split("\\."));
	}

	/**
	 * Gets an entry node's value at the designated path
	 *
	 * @param path The path to the entry node
	 * @return The entry node's value at the location defined by path or null if it either doesn't exist or is not an entry.
	 */
	public @Nullable String get(String... path) {
		SectionNode section = main;
		for (int i = 0; i < path.length; i++) {
			Node node = section.get(path[i]);
			if (node == null)
				return null;
			if (node instanceof SectionNode sectionNode) {
				if (i == path.length - 1)
					return null;
				section = sectionNode;
			} else {
				if (node instanceof EntryNode entryNode && i == path.length - 1)
					return entryNode.getValue();
				else
					return null;
			}
		}
		return null;
	}

	/**
	 * @return True if the config is empty, i.e. has no sections or entries.
	 */
	public boolean isEmpty() {
		return main.isEmpty();
	}

	public HashMap<String, String> toMap(final String separator) {
		return main.toMap("", separator);
	}

	public boolean validate(final SectionValidator validator) {
		return validator.validate(getMainNode());
	}

	private void load(final Class<?> cls, final @Nullable Object object, final String path) {
		for (final Field field : cls.getDeclaredFields()) {
			field.setAccessible(true);
			if (object != null || Modifier.isStatic(field.getModifiers())) {
				try {
					if (OptionSection.class.isAssignableFrom(field.getType())) {
						final OptionSection section = (OptionSection) field.get(object);
						@NotNull final Class<?> pc = section.getClass();
						load(pc, section, path + section.key + ".");
					} else if (Option.class.isAssignableFrom(field.getType())) {
						((Option<?>) field.get(object)).set(this, path);
					}
				} catch (final IllegalArgumentException | IllegalAccessException e) {
					assert false;
				}
			}
		}
	}

	/**
	 * Sets all {@link Option} fields of the given object to the values from this config
	 * @param object The object to load the options from
	 */
	public void load(Object object) {
		load(object.getClass(), object, "");
	}

	/**
	 * Sets all static {@link Option} fields of the given class to the values from this config
	 * @param clazz The class to load the options from
	 */
	public void load(Class<?> clazz) {
		load(clazz, null, "");
	}

	@Override
	public int compareTo(@Nullable Config other) {
		if (other == null)
			return 0;
		return fileName.compareTo(other.fileName);
	}

	void setIndentation(String indent) {
		assert indent != null && !indent.isEmpty() : indent;
		indentation = indent;
		indentationName = (indent.charAt(0) == ' ' ? "space" : "tab");
	}

	String getIndentation() {
		return indentation;
	}

	String getIndentationName() {
		return indentationName;
	}

	public SectionNode getMainNode() {
		return main;
	}

	public String getFileName() {
		return fileName;
	}

}
