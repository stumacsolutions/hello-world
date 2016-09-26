package contracts.base;

import com.jayway.restassured.module.mockmvc.RestAssuredMockMvc;
import hello.Application;
import org.junit.Before;

public class HelloBase
{
	@Before
	public void setup() {
		RestAssuredMockMvc.standaloneSetup(new Application());
	}
}