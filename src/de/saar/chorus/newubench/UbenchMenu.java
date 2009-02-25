package de.saar.chorus.newubench;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;

import de.saar.chorus.domgraph.UserProperties;

@SuppressWarnings("unused")
public class UbenchMenu extends JMenuBar {
	private Map<String,JMenu> menus;
	private Map<String,ButtonGroup> checkBoxGroups;
	private boolean isMac;
	private CommandListener listener;
	
	
	@MenuAnnotation(title="File")
	private JMenu fileMenu;
	
	@MenuItemAnnotation(title="Open...", parentTitle="File", keystroke="control O", command=CommandListener.FILE_OPEN, isEnabledOnEmptyWindow=true)
	private JMenuItem fileOpen;

	@MenuItemAnnotation(title="Open example...", parentTitle="File", command=CommandListener.FILE_OPEN_EXAMPLE, isEnabledOnEmptyWindow=true)
	private JMenuItem fileExample;
	
	@MenuItemAnnotation(title="Save...", parentTitle="File", keystroke="control S", command=CommandListener.FILE_SAVE)
	private JMenuItem fileSave;
	
	@MenuItemAnnotation(title="Save all solved forms...", parentTitle="File", command=CommandListener.FILE_SAVE_SOLVED_FORMS, isEnabledForSolvedForms=false)
	private JMenuItem fileSaveSolvedForms;
	
	@MenuItemAnnotation(title="Display codecs", parentTitle="File", command=CommandListener.DISPLAY_CODECS, useForMacOnly=true)
	private JMenuItem fileDisplayCodecs;

	@MenuItemAnnotation(title="Duplicate tab", parentTitle="File", keystroke="control D", command=CommandListener.DUPLICATE, addSeparatorBefore=true)
	private JMenuItem fileDuplicate;

	@MenuItemAnnotation(title="Close tab", parentTitle="File", keystroke="control W", command=CommandListener.FILE_CLOSE)
	private JMenuItem fileCloseTab;

	@MenuItemAnnotation(title="Quit", parentTitle="File", keystroke="control Q", command=CommandListener.QUIT, addSeparatorBefore=true, useForNonMacOnly=true)
	private JMenuItem fileQuit;

	
	@MenuAnnotation(title="Edit")
	private JMenu editMenu;
	
	@MenuAnnotation(title="Copy to clipboard", parentTitle="Edit")
	private JMenu editCopyMenu;
	
	@MenuAnnotation(title="Paste into new tab", parentTitle="Edit")
	private JMenu editPasteMenu;
	
	
	@MenuAnnotation(title="Solver")
	private JMenu solverMenu;
	
	@MenuAnnotation(title="Help", useForNonMacOnly=true)
	private JMenu helpMenu;
	
	
	
	
	
	
	
	
	


	public UbenchMenu() {
		super();

		menus = new HashMap<String, JMenu>();
		checkBoxGroups = new HashMap<String, ButtonGroup>();
		
		isMac = MacIntegration.isMac();
		listener = Ubench.getInstance().getCommandListener();
		
		computeMenuBar();
		addCodecMenus();
	}
	
	private void computeMenuBar() {
		// add all menus
		try {
			for( Field f : getClass().getDeclaredFields() ) {
				if( f.isAnnotationPresent(MenuAnnotation.class) ) {
					MenuAnnotation ann = f.getAnnotation(MenuAnnotation.class);
					
					if( !(isMac && ann.useForNonMacOnly()) && !(!isMac && ann.useForMacOnly()) ) {
						JMenu menu = new JMenu(ann.title());

						if( "".equals(ann.parentTitle())) {
							add(menu);
						} else {
							menus.get(ann.parentTitle()).add(menu);
						}


						menus.put(ann.title(), menu);
						f.set(this, menu);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		

		// add all menu items
		try {
			for( Field f : getClass().getDeclaredFields() ) {
				if( f.isAnnotationPresent(MenuItemAnnotation.class) ) {
					MenuItemAnnotation ann = f.getAnnotation(MenuItemAnnotation.class);
					
					if( !(isMac && ann.useForNonMacOnly()) && !(!isMac && ann.useForMacOnly()) ) {
						JMenuItem item = (f.getType() == JCheckBoxMenuItem.class) ? new JCheckBoxMenuItem(ann.title()) : new JMenuItem(ann.title());
						addToButtonGroup(item, ann.checkBoxGroup());
						item.setActionCommand(ann.command());

						if( ann.addSeparatorBefore() ) {
							menus.get(ann.parentTitle()).add(new JSeparator());
						}

						menus.get(ann.parentTitle()).add(item);
						f.set(this, item);

						if( ! "".equals(ann.keystroke())) {
							item.setAccelerator(KeyStroke.getKeyStroke(mungeKeystrokeForMac(ann.keystroke())));
						}
						
						if( f.getType() == JCheckBoxMenuItem.class ) {
							item.addItemListener(listener);
							listener.registerEventSource(item, ann.command());
						} else {
							item.addActionListener(listener);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String mungeKeystrokeForMac(String keystroke) {
		if( isMac ) {
			return keystroke.replaceAll("control", "meta");
		} else {
			return keystroke;
		}
	}

	private void addToButtonGroup(JMenuItem item, String groupname) {
		if( ! "".equals(groupname) ) {
			ButtonGroup group = checkBoxGroups.get(groupname);

			if( group == null ) {
				group = new ButtonGroup();
				checkBoxGroups.put(groupname, group);
			}

			group.add(item);
		}
	}
	
	private void addCodecMenus() {
		String definput = UserProperties.getDefaultInputCodec();
        String defoutput = UserProperties.getDefaultOutputCodec();
        int control = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        CommandListener listener = Ubench.getInstance().getCommandListener();
        
        for( String codecname : Ubench.getInstance().getCodecManager().getAllOutputCodecs() ) {
        	JMenuItem item = new JMenuItem("as " + codecname);
        	item.setActionCommand(CommandListener.EXPORT_CLIPBOARD + codecname);
        	item.addActionListener(listener);
        	editCopyMenu.add(item);
        	
        	if( codecname.equals(defoutput)) {
        		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, control));
        	} 
        }

        for( String codecname : Ubench.getInstance().getCodecManager().getAllInputCodecs() ) {
        	JMenuItem item = new JMenuItem("as " + codecname);
        	item.setActionCommand(CommandListener.IMPORT_CLIPBOARD + codecname);
        	item.addActionListener(listener);
        	editPasteMenu.add(item);
        	
        	if( codecname.equals(definput)) {
        		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, control));
        	} 
        }
	}



	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD) 
	private static @interface MenuAnnotation {
		String title();
		String parentTitle() default "";
		
		boolean useForMacOnly() default false;
		boolean useForNonMacOnly() default false;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD) 
	private static @interface MenuItemAnnotation {
		String title();
		String parentTitle();
		String checkBoxGroup() default "";

		String keystroke() default "";
		String command();

		boolean isEnabledOnEmptyWindow() default false;
		boolean isEnabledForGraphs() default true;
		boolean isEnabledForSolvedForms() default true;

		boolean useForMacOnly() default false;
		boolean useForNonMacOnly() default false;

		boolean addSeparatorBefore() default false;
	}

	private static final long serialVersionUID = 7862528735560086295L;
}
