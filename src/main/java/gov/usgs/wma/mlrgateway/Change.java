package gov.usgs.wma.mlrgateway;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a change in an object of parameterized type `T`
 *  * Change objects whose `type` properties are set to 
 *   `ChangeType.CREATION` must have a null-valued `previous` property.
 *  * Change objects whose `type` properties are set to `ChangeType.MODIFICATION`
 *    must have non-null valued `previous` and `next` properties.
 */

public class Change<T> {
	private final ChangeType type;
	private final T previous;
	private final T next;
	
	private static final String version = "1.0";
	private final static Map<ChangeType, String> validationMessages = new HashMap<>();
	static {
		validationMessages.put(ChangeType.CREATION, "Creations must not specify a `previous` value.");
		validationMessages.put(ChangeType.MODIFICATION, "Modifications must specify both a `previous` and a `next` value");
	}
	
	public Change(ChangeType type, T previous, T next) {
		validate(type, previous, next);
		this.type = type;
		this.previous = previous;
		this.next = next;
	}
	
	private void validate(ChangeType type, T previous, T next) {
		boolean valid = false;
		if (ChangeType.CREATION == type) {
			valid = 
				null == previous
				&&
				next != null
			;
		} else if (ChangeType.MODIFICATION == type) {
			valid = 
				null != previous
				&&
				next != null
			;
		}
		
		if (!valid) {
			throw new IllegalArgumentException(validationMessages.get(type));
		}
	}
	
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
