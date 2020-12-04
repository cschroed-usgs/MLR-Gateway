package gov.usgs.wma.mlrgateway;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a change of an object of parameterized type `T`. Instances are 
 * serialized to JSON and published to external systems.
 */

public abstract class Change<T> {
	protected ChangeKind kind; //This field allows subclasses to indicate their type when serialized as JSON
	private final String version = "1.0";
	protected T previous;
	protected T next;
	
	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @return the kind
	 */
	public ChangeKind getKind() {
		return kind;
	}

	/**
	 * @return the previous state
	 */
	public T getPrevious() {
		return previous;
	}

	/**
	 * @return the next state
	 */
	public T getNext() {
		return next;
	}
}
