package net.syncarus.rcp;

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
	public static final String IMAGE_SELECT_TREE = IMAGE_FOLDER + "select_tree.gif";
	public static final String IMAGE_COLLAPSE_TREE = IMAGE_FOLDER + "collapse_tree.gif";
	public static final String IMAGE_EXPAND_TREE = IMAGE_FOLDER + "expand_tree.gif";
	public static final String IMAGE_FLIP_CHECK_STATE = IMAGE_FOLDER + "flip_check_state.png";
	public static final String IMAGE_COMPARE_LOCATIONS = IMAGE_FOLDER + "compare_locations.png";
	public static final String IMAGE_SHOW_IN_EXPLORER = IMAGE_FOLDER + "show_in_explorer.gif";
	public static final String IMAGE_SELECT_LOCATION = IMAGE_FOLDER + "select_location.png";
	public static final String IMAGE_SWITCH_STATUS = IMAGE_FOLDER + "switch_status.png";
	public static final String IMAGE_SYNCHRONIZE = IMAGE_FOLDER + "synchronize.png";
	public static final String IMAGE_DIR_UP = IMAGE_FOLDER + "dir_up.gif";
	public static final String IMAGE_DIR_FLOPPY = IMAGE_FOLDER + "dir_floppy.gif";
	public static final String IMAGE_DIR_HDD = IMAGE_FOLDER + "dir_hdd.gif";
	public static final String IMAGE_DIR_NORMAL = IMAGE_FOLDER + "dir_normal.gif";
	public static final String IMAGE_DIR_ADD_A = IMAGE_FOLDER + "dir_add_a.gif";
	public static final String IMAGE_DIR_ADD_B = IMAGE_FOLDER + "dir_add_b.gif";
	public static final String IMAGE_DIR_REMOVE_A = IMAGE_FOLDER + "dir_remove_a.gif";
	public static final String IMAGE_DIR_REMOVE_B = IMAGE_FOLDER + "dir_remove_b.gif";
	public static final String IMAGE_FILE_ADD_A = IMAGE_FOLDER + "file_add_a.gif";
	public static final String IMAGE_FILE_ADD_B = IMAGE_FOLDER + "file_add_b.gif";
	public static final String IMAGE_FILE_REPLACE_A = IMAGE_FOLDER + "file_replace_a.gif";
	public static final String IMAGE_FILE_REPLACE_B = IMAGE_FOLDER + "file_replace_b.gif";
	public static final String IMAGE_FILE_REMOVE_A = IMAGE_FOLDER + "file_remove_a.gif";
	public static final String IMAGE_FILE_REMOVE_B = IMAGE_FOLDER + "file_remove_b.gif";
	public static final String IMAGE_FILE_TOUCH = IMAGE_FOLDER + "file_touch.png";
	public static final String IMAGE_FILE_CONFLICT = IMAGE_FOLDER + "file_conflict.gif";
	public static final String IMAGE_WIZARD_A = IMAGE_FOLDER + "wizard_a.gif";
	public static final String IMAGE_WIZARD_B = IMAGE_FOLDER + "wizard_b.gif";
	public static final String IMAGE_PREFERENCES = IMAGE_FOLDER + "preferences.png";
	public static final String IMAGE_ABOUT = IMAGE_FOLDER + "about.png";
	public static final String IMAGE_CLEAR_LOG = IMAGE_FOLDER + "clear_log.png";
	public static final String IMAGE_ENABLE = IMAGE_FOLDER + "enable.png";
	public static final String IMAGE_DISABLE = IMAGE_FOLDER + "disable.png";

	// fonts
	public static final FontData FONT_8 = new FontData("?", 8, SWT.NORMAL);
	public static final FontData FONT_BOLD_8 = new FontData("?", 8, SWT.BOLD);
	
	// colors
	
	public static final RGB COLOR_RED = new RGB(255, 200, 200);
	public static final RGB COLOR_GREEN = new RGB(200, 255, 200);
	public static final RGB COLOR_ORANGE =  new RGB(255, 255, 150);

	// registries
	private final ColorRegistry COLOR_REGISTRY = new ColorRegistry(Display.getDefault(), true);
	private final FontRegistry FONT_REGISTRY = new FontRegistry(Display.getDefault());

	private static final String DISABLED_IMAGE = "DISABLED";

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
		String name = key.toString();
		
		if (!FONT_REGISTRY.hasValueFor(name))
			FONT_REGISTRY.put(name, new FontData[] { key });
		
		return FONT_REGISTRY.get(name);
	}
}
