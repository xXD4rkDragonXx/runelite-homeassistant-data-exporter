package haexporterplugin.data;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.*;

public class PrayerDataTest {

    private final Gson gson = new Gson();

    @Test
    public void testConstructorSetsFields() {
        PrayerData prayer = new PrayerData(50, 70);
        String json = gson.toJson(prayer);

        assertTrue(json.contains("\"current\":50"));
        assertTrue(json.contains("\"max\":70"));
    }

    @Test
    public void testFullPrayer() {
        PrayerData prayer = new PrayerData(99, 99);
        String json = gson.toJson(prayer);

        assertTrue(json.contains("\"current\":99"));
        assertTrue(json.contains("\"max\":99"));
    }

    @Test
    public void testZeroPrayer() {
        PrayerData prayer = new PrayerData(0, 70);
        String json = gson.toJson(prayer);

        assertTrue(json.contains("\"current\":0"));
        assertTrue(json.contains("\"max\":70"));
    }

    @Test
    public void testBoostedPrayer() {
        PrayerData prayer = new PrayerData(80, 70);
        String json = gson.toJson(prayer);

        assertTrue(json.contains("\"current\":80"));
        assertTrue(json.contains("\"max\":70"));
    }
}
