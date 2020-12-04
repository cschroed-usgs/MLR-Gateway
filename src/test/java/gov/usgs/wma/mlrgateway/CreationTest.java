package gov.usgs.wma.mlrgateway;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class CreationTest {

	@Test
	public void testHappyConstructor() {
		Creation<String> instance = new Creation<>("foo");
		assertNull(instance.getPrevious());
		assertEquals("foo", instance.getNext());
		assertEquals(ChangeKind.CREATION, instance.getKind());
	}
	
	@Test
	public void testNullNewObject() {
		assertThrows(NullPointerException.class, () -> new Creation(null));
	}
	
}
