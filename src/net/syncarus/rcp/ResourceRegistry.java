package net.syncarus.rcp;

import java.lang.reflect.Field;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * In order to ease the usage of SWT resources, this class has been introduced.
 * It caches images and colours and disposes them when the application shuts
 * down. Should not be used for big images, since the images are never disposed.
 */
public class ResourceRegistry {
	// images
	private static final String IMAGE_FOLDER = "/icons/";
	public static final String IMAGE_SELECT_TREE = IMAGE_FOLDER + "select.gif";
	public static final String IMAGE_COLLAPSE_TREE = IMAGE_FOLDER + "collapseall.gif";
	public static final String IMAGE_EXPAND_TREE = IMAGE_FOLDER + "expandall.gif";
	public static final String IMAGE_FLIP_CHECK_STATE = IMAGE_FOLDER + "flipcheckstate.png";
	public static final String IMAGE_DIFF_DETECT = IMAGE_FOLDER + "detect.png";
	public static final String IMAGE_SHOW_IN_EXPLORER = IMAGE_FOLDER + "show_in_explorer.gif";
	public static final String IMAGE_CHOOSE_DIR = IMAGE_FOLDER + "dirchoose.png";
	public static final String IMAGE_SWITCH_STATUS = IMAGE_FOLDER + "switch_status.png";
	public static final String IMAGE_SYNCHRONIZE = IMAGE_FOLDER + "synchronize.png";
	public static final String IMAGE_DIR_UP = IMAGE_FOLDER + "dir_up.gif";
	public static final String IMAGE_DIR_FLOPPY = IMAGE_FOLDER + "dir_floppy.gif";
	public static final String IMAGE_DIR_HDD = IMAGE_FOLDER + "dir_hdd.gif";
	public static final String IMAGE_DIR_NORMAL = IMAGE_FOLDER + "dir_normal.gif";
	public static final String IMAGE_DIR_ADD_LEFT = IMAGE_FOLDER + "dir_add_to_source.gif";
	public static final String IMAGE_DIR_ADD_RIGHT = IMAGE_FOLDER + "dir_add_to_target.gif";
	public static final String IMAGE_DIR_REMOVE_LEFT = IMAGE_FOLDER + "dir_remove_from_source.gif";
	public static final String IMAGE_DIR_REMOVE_RIGHT = IMAGE_FOLDER + "dir_remove_from_target.gif";
	public static final String IMAGE_FILE_ADD_LEFT = IMAGE_FOLDER + "file_add_to_source.gif";
	public static final String IMAGE_FILE_ADD_RIGHT = IMAGE_FOLDER + "file_add_to_target.gif";
	public static final String IMAGE_FILE_MODIFY_LEFT = IMAGE_FOLDER + "file_modify_source.gif";
	public static final String IMAGE_FILE_MODIFY_RIGHT = IMAGE_FOLDER + "file_modify_target.gif";
	public static final String IMAGE_FILE_REMOVE_LEFT = IMAGE_FOLDER + "file_remove_from_source.gif";
	public static final String IMAGE_FILE_REMOVE_RIGHT = IMAGE_FOLDER + "file_remove_from_target.gif";
	public static final String IMAGE_FILE_TOUCH = IMAGE_FOLDER + "file_touch.png";
	public static final String IMAGE_FILE_PUZZLED = IMAGE_FOLDER + "puzzled.gif";
	public static final String IMAGE_WIZARD_LEFT = IMAGE_FOLDER + "source_wizard.gif";
	public static final String IMAGE_WIZARD_RIGHT = IMAGE_FOLDER + "target_wizard.gif";
	public static final String IMAGE_PREFERENCES = IMAGE_FOLDER + "filter.png";
	public static final String IMAGE_PROPERTIES = IMAGE_FOLDER + "properties.png";
	public static final String IMAGE_ABOUT = IMAGE_FOLDER + "about.png";
	public static final String IMAGE_CLEAR_LOG = IMAGE_FOLDER + "clearlog.png";
	public static final String IMAGE_ENABLE = IMAGE_FOLDER + "enable.png";
	public static final String IMAGE_DISABLE = IMAGE_FOLDER + "disable.png";

	// fonts
	public static final FontData FONT_8 = new FontData("?", 8, SWT.NORMAL);
	public static final FontData FONT_BOLD_8 = new FontData("?", 8, SWT.BOLD);

	// registries
	private final ColorRegistry COLOR_REGISTRY = new ColorRegistry(Display.getDefault(), true);
	private final FontRegistry FONT_REGISTRY = new FontRegistry(Display.getDefault());

	private static final String DISABLED_IMAGE = "DISABLED";

	public ResourceRegistry() {
		try {
			initializeColors();
			initializeFonts();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns an enabled image of key
	 * 
	 * @param key
	 * @return
	 */
	public Image getImage(String key) {
		return getImage(key, true);
	}

	/**
	 * Returns an enabled or disabled image of key depending on parameter
	 * enabled
	 * 
	 * @param key
	 * @param enabled
	 * @return
	 */
	public Image getImage(String key, boolean enabled) {
		if (!enabled)
			key += DISABLED_IMAGE;

		SyncarusPlugin plugin = SyncarusPlugin.getInstance();
		ImageRegistry registry = plugin.getImageRegistry();
		Image image = registry.get(key);
		if (image == null) {
			ImageDescriptor id = ImageDescriptor.createFromURL(plugin.getBundle().getEntry(key));
			registry.put(key, id);
			image = registry.get(key);
		}
		return image;
	}

	public ImageDescriptor getImageDescriptor(String key) {
		return getImageDescriptor(key, true);
	}

	public ImageDescriptor getImageDescriptor(String key, boolean enabled) {
		Image image = getImage(key, enabled);
		return ImageDescriptor.createFromImage(image);
	}

	public Color getColor(RGB rgb) {
		String name = rgb.toString();

		Color color = COLOR_REGISTRY.get(name);
		if (color == null) {
			COLOR_REGISTRY.put(name, rgb);
			color = COLOR_REGISTRY.get(name);
		}

		return color;
	}

	public Font getFont(FontData key) {
		return FONT_REGISTRY.get(key.toString());
	}

	private void initializeColors() throws IllegalAccessException {
		for (Field field : ResourceRegistry.class.getFields()) {
			if (!field.getName().startsWith("COLOR_"))
				continue;

			RGB fieldValue = (RGB) field.get(null);
			COLOR_REGISTRY.put(fieldValue.toString(), fieldValue);
		}
	}

	private void initializeFonts() throws IllegalAccessException {
		for (Field field : ResourceRegistry.class.getFields()) {
			if (!field.getName().startsWith("FONT_"))
				continue;

			FontData fieldValue = (FontData) field.get(null);
			FONT_REGISTRY.put(fieldValue.toString(), new FontData[] { fieldValue });
		}
	}
}
