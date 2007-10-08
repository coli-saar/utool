package de.saar.chorus.domgraph;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import de.saar.chorus.ubench.Ubench;

/**
 * A class representing user-specific properties which are stored in a File
 * ".utool" in the user's home directory. 
 * If there is no such file, all properties are initialized with a default value.
 * If the file contains only some of the properties, the other properties are initialized
 * with their default value. 
 * 
 * To add a new property type,
 * 	1) specify an enum with its name and default value in the <code>PropertyNames</code>
 * 	2) implement convenient / necessary getters and setters
 * 
 * @author Michaela Regneri
 *
 */
public class UserProperties {
	
	
	
	// the actual properties object
	private static Properties properties;
	
	// the local file
	private static File file;
	
	// indicates whether there have been changes made
	// during the current utool run
	private static boolean update;
	
	
	private static Calendar cal = Calendar.getInstance();
	private static String date = cal.get(Calendar.YEAR) + "-" +
		cal.get(Calendar.MONTH) + "-" +
		cal.get(Calendar.DAY_OF_MONTH) + "-" +
		cal.get(Calendar.HOUR_OF_DAY) + "-" +
		cal.get(Calendar.MINUTE) + "-" +
		cal.get(Calendar.SECOND);
	
	/**
	 * Initialize the properties that are stored in .utool;
	 * set default values for all the other properties.
	 */
	static {
		update = false;
		String home = System.getProperty("user.home");
		file = new File(home + File.separator + ".utool");
		if( file.exists() ) {
			
			properties = new Properties();
			try {
				properties.load(new FileInputStream(file));
			} catch ( IOException e) {
				properties = getDefaults();
				update = true;
			} catch ( IllegalArgumentException e ) {
				properties = getDefaults();
				update = true;
			}
			
			// to avoid "partial" initializing
			checkProperties();
		} else {

			properties = getDefaults();
			update = true;
		}
	}
	
	/**
	 * This iterates over the properties file and checks whether all properties
	 * have been assigned a value and assigns a default value to the properties
	 * which have not been initalised.
	 */
	private static void checkProperties() {
		PropertyNames[] props = PropertyNames.values();
		for( int i = 0; i< props.length; i++ ) {
			if(! properties.containsKey(props[i].getKeyString())) {
				properties.put(props[i].getKeyString(), props[i].getDefaultValue());
			}
		}
	}
	
	/**
	 * 
	 * @return A standard comment for the properties file update
	 */
	private static String createStoreComment() {
		String n = System.getProperty("line.separator");
		String line = "----------------------------------------------------------------" + n;
		
		StringBuffer log = new StringBuffer();
		log.append("utool - The Swiss Army Knife of Underspecification, v. " + GlobalDomgraphProperties.getVersion() + n
				+ n
				+ "created by the CHORUS project, SFB 378, Saarland University" +n + n +  line + n);
		log.append("utool user preferences - updated ");
		log.append(date  + n +n + n);
		return log.toString();
	}
	
	/**
	 * If there have been changes, the properties are saved to 
	 * the .utool file. 
	 * TODO so far, "changes" mean the user has called one of the setters.
	 * 		perhaps one should be more precise here?
	 * 
	 * @return true if updates have been stored; false if there went something wrong or
	 * 			there were no updates.
	 */
	public static boolean saveProperties() {
		if(update) {
			try {
				FileOutputStream fo = new FileOutputStream(file);
				properties.store(fo, createStoreComment());
				return true;
			} catch (IOException e) {
				return false;
			}
		} else {
			return false;
		}
	}
	
	/**
	 * 
	 * @return true if experimental codecs are integrated
	 */
	public static boolean allowExperimentalCodecs() {
		return Boolean.parseBoolean(properties.getProperty(
				PropertyNames.EXPERIMENTAL_CODECS.getKeyString()));
	}
	
	/**
	 *
	 * @return A <code>List</code> of the example directories represented as Strings.
	 */
	public static List<String> getExampleDirectories() {
		List<String> ret = new ArrayList<String>();
		String dirs = properties.getProperty(PropertyNames.EXAMPLE_DIRECTORIES.getKeyString());

		StringTokenizer tok = new StringTokenizer(dirs, File.pathSeparator, false);
		while(tok.hasMoreTokens()) {
			ret.add(tok.nextToken());
		}
		
		return ret;
	}
	
	/**
	 * This changes all example directories to the path (or path list)
	 * specified in dirs. Multiple paths must be separated by the system
	 * specific path separator.
	 * 
	 * @param dirs
	 */
	public static void setExampleDirectories(String dirs) {
		update = true;
		properties.setProperty(PropertyNames.EXAMPLE_DIRECTORIES.getKeyString(), dirs);
	}
	
	/**
	 * This adds a single example directory to the current list of directories.
	 * 
	 * @param dir the new directory
	 */
	public static void addExampleDirectory(String dir) {
		update = true;
		String old = properties.getProperty(PropertyNames.EXAMPLE_DIRECTORIES.getKeyString());
		old = old + File.separator + dir;
		properties.setProperty(PropertyNames.EXAMPLE_DIRECTORIES.getKeyString(),old);
	}
	
	/**
	 * 
	 * @return the name of the default input codec
	 */
	public static String getDefaultInputCodec() {
		return properties.getProperty(PropertyNames.INPUT_CODEC.getKeyString());
	}
	
	/**
	 * This changes the current default input codec.
	 * 
	 * @param codec the new default input codec
	 */
	public static void setDefaultInputCodec(String codec) {
		update = true;
		properties.setProperty(PropertyNames.INPUT_CODEC.getKeyString(), codec);
	}
	
	/**
	 * 
	 * @return the name of the default output codec
	 */
	public static String getDefaultOutputCodec() {
		return properties.getProperty(PropertyNames.OUTPUT_CODEC.getKeyString());
	}
	
	/**
	 * This changes the current default output codec.
	 * 
	 * @param codec the new default output codec
	 */
	public static void setDefaultOutputCodec(String codec) {
		update = true;
		properties.setProperty(PropertyNames.OUTPUT_CODEC.getKeyString(), codec);
	}
	
	/**
	 * The "utool working directory" is the last directory checked manually for any kind of
	 * files. It is only used and changed in Ubench.
	 * 
	 * @return the last directory in use
	 * @see Ubench#getLastPath()
	 */
	public static String getWorkingDirectory() {
		return properties.getProperty(PropertyNames.WORKING_DIRECTORY.getKeyString());
	}
	
	/**
	 * Indicates the last 'active' directory the user has accessed
	 * via Ubench. 
	 * 
	 * @param dir the last directory in use
	 * @see Ubench#setLastPath()
	 */
	public static void setWorkingDirectory(String dir) {
		update = true;
		properties.setProperty(PropertyNames.WORKING_DIRECTORY.getKeyString(), dir);
	}
	
	
     

	

	/**
	 * 
	 * @return A <code>Properties</code> object containing default values for all properties
	 * 		   defined in <code>PropertyNames</code>
	 */
	public static Properties getDefaults() {
		Properties ret = new Properties();
		PropertyNames[] props = PropertyNames.values();
		for( int i = 0; i< props.length; i++ ) {
			ret.put(props[i].getKeyString(), props[i].getDefaultValue());
		}
		return ret;
	}
	
	/**
	 * An enum class with a type for each property we use in utool.
	 * The property enums can return their "official" key string (as it is stored
	 * in the <code>Properties</code> object) and their default value.
	 * 
	 * @author Michaela Regneri
	 *
	 */
	private enum PropertyNames {
		EXPERIMENTAL_CODECS {
			String getKeyString() {
				return "show experimental codecs";
			}
			
			String getDefaultValue() {
				return "true";
			}
		},
		
		INPUT_CODEC {
			String getKeyString() {
				return "default input codec";
			}
			
			String getDefaultValue() {
				return "domcon-oz";
			}
		}, 
		
		OUTPUT_CODEC {
			String getKeyString() {
				return "default output codec";
			}
			
			String getDefaultValue() {
				return "domcon-oz";
			}
		},
		
		WORKING_DIRECTORY {
			
			String getKeyString() {
				return "utool working directory";
			}
			
			String getDefaultValue() {
				return System.getProperty("user.dir");
			}
		},
		
		EXAMPLE_DIRECTORIES {
			String getKeyString() {
				return "example directories";
			}
			
			String getDefaultValue() {
				return "projects" + File.separator + 
						"Domgraph" + File.separator + "examples" 
						+ File.pathSeparator + "examples";
			}
 		};
		
		
		 abstract String getKeyString();
		 abstract String getDefaultValue();
	}
 }
