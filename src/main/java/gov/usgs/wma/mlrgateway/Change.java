package gov.usgs.wma.mlrgateway;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a change of an object of parameterized type `T`. Instances are 
 * serialized and published to external systems.
 */

public abstract class Change<T> {
	protected ChangeType type;
	private static final String version = "1.0";
	protected T previous;
	protected T next;
	
	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @return the type
	 */
	public ChangeType getType() {
		return type;
	}

	/**
	 * @return the previous
	 */
	public T getPrevious() {
		return previous;
	}

	/**
	 * @return the next
	 */
	public T getNext() {
		return next;
	}
}
