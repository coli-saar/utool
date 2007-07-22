package de.saar.chorus.gecode;

/**
 * 
 * @author Marco Kuhlmann
 *
 */
public interface NodeVisitorInterface {
    abstract public void setCursor(NodeCursorInterface cursor);
    abstract public NodeCursorInterface getCursor();
    abstract public boolean next();
}
