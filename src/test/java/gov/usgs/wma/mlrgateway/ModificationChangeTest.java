package gov.usgs.wma.mlrgateway;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class ModificationChangeTest {

	@Test
	public void testHappyConstructor() {
		ModificationChange<String> instance = new ModificationChange<>("before", "after");
		assertEquals(ChangeType.MODIFICATION, instance.getType());
		assertEquals("before", instance.getPrevious());
		assertEquals("after", instance.getNext());
	}
	
	@Test
	public void testNullPrevious() {
		assertThrows(NullPointerException.class, () -> new ModificationChange<String>(null, "something"));
	}
	
	@Test
	public void testNullNext() {
		assertThrows(NullPointerException.class, () -> new ModificationChange<String>("something", null));
	}
	
	@Test
	public void testBothNull() {
		assertThrows(NullPointerException.class, () -> new ModificationChange<String>(null, null));
	}
}
