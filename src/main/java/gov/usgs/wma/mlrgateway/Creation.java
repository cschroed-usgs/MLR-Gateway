package gov.usgs.wma.mlrgateway;

import java.util.Objects;


public class Creation<T> extends Change<T> {
	public Creation(T newObject) {
		Objects.requireNonNull(newObject);
		next = newObject;
		previous = null;
		kind = ChangeKind.CREATION;
	}
}
