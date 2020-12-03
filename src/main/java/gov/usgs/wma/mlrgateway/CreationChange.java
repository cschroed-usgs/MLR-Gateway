package gov.usgs.wma.mlrgateway;

import java.util.Objects;


public class CreationChange<T> extends Change<T> {
	public CreationChange(T newObject) {
		Objects.requireNonNull(newObject);
		next = newObject;
		previous = null;
		type = ChangeType.CREATION;
	}
}
