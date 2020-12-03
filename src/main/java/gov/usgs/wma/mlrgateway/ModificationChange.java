package gov.usgs.wma.mlrgateway;

import java.util.Objects;


public class ModificationChange<T> extends Change<T> {
	public ModificationChange (T previous, T next) {
		Objects.requireNonNull(previous);
		Objects.requireNonNull(next);
		this.previous = previous;
		this.next = next;
		type = ChangeType.MODIFICATION;
	}
}
