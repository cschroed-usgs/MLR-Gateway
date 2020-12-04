package gov.usgs.wma.mlrgateway;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class ModificationTest {

	@Test
	public void testHappyConstructor() {
		Modification<String> instance = new Modification<>("before", "after");
		assertEquals(ChangeKind.MODIFICATION, instance.getKind());
		assertEquals("before", instance.getPrevious());
		assertEquals("after", instance.getNext());
	}
	
	@Test
	public void testNullPrevious() {
		assertThrows(NullPointerException.class, () -> new Modification<String>(null, "something"));
	}
	
	@Test
	public void testNullNext() {
		assertThrows(NullPointerException.class, () -> new Modification<String>("something", null));
	}
	
	@Test
	public void testBothNull() {
		assertThrows(NullPointerException.class, () -> new Modification<String>(null, null));
	}
}
