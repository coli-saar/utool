package de.saar.chorus.newubench;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class CommandListener extends WindowAdapter implements ActionListener, ItemListener {
	private Map<Object,String> eventSources;
	
	/******** actions *********/
	
	public static final String QUIT="quit";
	@CommandAnnotation(command=QUIT)
	private void quit(String command) {
		Ubench.getInstance().quit();
	}
	
	public static final String SOLVE="solve";
	@CommandAnnotation(command=SOLVE)
	private void solve(String command) {
		UbenchTab tab = Ubench.getInstance().getCurrentTab();
		
		if( tab instanceof GraphTab ) {
			((GraphTab) tab).showFirstSolvedForm();
		}
	}
	
	public static final String NEXT="next";
	@CommandAnnotation(command=NEXT)
	private void next(String command) {
		UbenchTab tab = Ubench.getInstance().getCurrentTab();
		
		if( tab instanceof SolvedFormTab ) {
			((SolvedFormTab) tab).showNextSolvedForm();
		}
	}
	
	public static final String PREVIOUS="prev";
	@CommandAnnotation(command=PREVIOUS)
	private void previous(String command) {
		UbenchTab tab = Ubench.getInstance().getCurrentTab();
		
		if( tab instanceof SolvedFormTab ) {
			((SolvedFormTab) tab).showPreviousSolvedForm();
		}
	}
	
	public static final String JUMP_TO_SF="jumpToSf";
	@CommandAnnotation(command=JUMP_TO_SF)
	private void jumpToSf(String command) {
		UbenchTab tab = Ubench.getInstance().getCurrentTab();
		
		if( tab instanceof SolvedFormTab ) {
			((SolvedFormTab) tab).showSelectedSolvedForm();
		}
	}
	
	public static final String FILE_OPEN="fileOpen";
	@CommandAnnotation(command=FILE_OPEN)
	private void fileOpen(String command) {
		System.err.println("FILE OPEN");
	}
	
	
	
	
	
	
	
	/********* event handling ***********/
	
	public void actionPerformed(ActionEvent ev) {
		// obtain the command
		String command = ev.getActionCommand();
		
		if( command == null ) {
			command = lookupEventSource(ev.getSource());
		}
		
		if( command == null ) {
			System.err.println("Undefined action command!");
			return;
		}

		// call the appropriate method
		call(command);
	}

	public void itemStateChanged(ItemEvent ev) {
		String command = lookupEventSource(ev.getSource());
		
		if( command == null ) {
			System.err.println("Undefined item state change command!");
			return;
		}
	
		call(command);
	}
	
	@Override
	public void windowClosing(WindowEvent e) {
		quit(null);
	}
	
	private void call(String command) {
		try {
			for( Method m : getClass().getDeclaredMethods() ) {
				if( m.isAnnotationPresent(CommandAnnotation.class) ) {
					if( command.startsWith(m.getAnnotation(CommandAnnotation.class).command()) ) {
						m.invoke(this, command);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD) 
	private static @interface CommandAnnotation {
		String command();
	}

	

	
	/********* event source handling ***********/
	
	public void registerEventSource(Object source, String desc) {
		eventSources.put(source,desc);
	}
	
	private String lookupEventSource(Object source) {
		return eventSources.get(source);
	}
	
	
	public CommandListener() {
		eventSources = new HashMap<Object, String>();
	}

}
