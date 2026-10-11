package za.co.lightshed.placename;

import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

class PlaceNameTest {
    private PlaceNameCleaner.Result clean(String body) throws Exception {
        return new PlaceNameCleaner().clean(new StringReader("Town,Province\n" + body));
    }

    @Test void trimsAndNormalisesCase() throws Exception {
        var r = clean("  cape town ,western cape\n");
        assertEquals(new Place("Cape Town", "Western Cape"), r.places().get(0));
    }

    @Test void mapsProvinceAliases() throws Exception {
        var r = clean("Richards Bay,KZN\nRustenburg,North-West\n");
        assertEquals("KwaZulu-Natal", r.places().stream().filter(p -> p.name().equals("Richards Bay")).findFirst().get().province());
        assertEquals("North West", r.places().stream().filter(p -> p.name().equals("Rustenburg")).findFirst().get().province());
    }

    @Test void removesDuplicatesAfterNormalising() throws Exception {
        var r = clean("Paarl,Western Cape\nPAARL ,WESTERN CAPE\n");
        assertEquals(1, r.places().size());
        assertEquals(1, r.duplicates());
    }

    @Test void rejectsInvalidRows() throws Exception {
        var r = clean(",Gauteng\nSandton,\nAtlantis,Narnia\n12345,Gauteng\nToo,Many,Fields\n");
        assertEquals(0, r.places().size());
        assertEquals(5, r.rejected());
    }

    @Test void endpointsWork() throws Exception {
        var service = new PlaceNameService(clean("Springs,Gauteng\nPaarl,Western Cape\n").places());
        JavalinTest.test(Main.createApp(service), (server, client) -> {
            assertEquals(200, client.get("/provinces").code());
            assertTrue(client.get("/towns/gauteng").body().string().contains("Springs"));
            assertEquals(404, client.get("/towns/Narnia").code());
        });
    }
}
