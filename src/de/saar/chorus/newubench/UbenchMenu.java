package de.saar.chorus.newubench;

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

import de.saar.chorus.ubench.MacIntegration;

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

	@MenuItemAnnotation(title="Open example...", parentTitle="File", command=CommandListener.FILE_OPEN, isEnabledOnEmptyWindow=true)
	private JMenuItem fileExample;

	@MenuItemAnnotation(title="Quit", parentTitle="File", keystroke="control Q", command=CommandListener.QUIT, addSeparatorBefore=true, useForNonMacOnly=true)
	private JMenuItem fileQuit;

	
	@MenuAnnotation(title="Edit")
	private JMenu editMenu;
	
	@MenuItemAnnotation(title="Copy", parentTitle="Edit", command="edit")
	private JMenuItem editCopy;
	
	@MenuItemAnnotation(title="Hi Andrew", parentTitle="Edit", command="andrew")
	private JMenuItem andrewItem;
	
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
