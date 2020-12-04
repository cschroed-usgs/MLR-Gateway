package gov.usgs.wma.mlrgateway;

import java.util.Objects;


public class Modification<T> extends Change<T> {
	public Modification (T previous, T next) {
		Objects.requireNonNull(previous);
		Objects.requireNonNull(next);
		this.previous = previous;
		this.next = next;
		kind = ChangeKind.MODIFICATION;
	}
}
