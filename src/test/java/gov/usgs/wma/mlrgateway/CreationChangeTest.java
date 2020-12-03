package gov.usgs.wma.mlrgateway;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class CreationChangeTest {

	@Test
	public void testHappyConstructor() {
		CreationChange<String> instance = new CreationChange<>("foo");
		assertNull(instance.getPrevious());
		assertEquals("foo", instance.getNext());
		assertEquals(ChangeType.CREATION, instance.getType());
	}
	
	@Test
	public void testNullNewObject() {
		assertThrows(NullPointerException.class, () -> new CreationChange(null));
	}
	
}
