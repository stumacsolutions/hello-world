package hello;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ApplicationTest
{
    private Application application;

    @Before
    public void setUp()
    {
        application = new Application();
    }

    @Test
    public void shouldSayHelloWhenHomeIsAccessed()
    {
        assertEquals("Hello, World!", application.home());
    }
}
