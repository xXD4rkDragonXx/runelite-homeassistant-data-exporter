package haexporterplugin.data;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.*;

public class HealthDataTest {

    private final Gson gson = new Gson();

    @Test
    public void testConstructorSetsFields() {
        HealthData health = new HealthData(75, 99);
        String json = gson.toJson(health);

        assertTrue(json.contains("\"current\":75"));
        assertTrue(json.contains("\"max\":99"));
    }

    @Test
    public void testFullHealth() {
        HealthData health = new HealthData(99, 99);
        String json = gson.toJson(health);

        assertTrue(json.contains("\"current\":99"));
        assertTrue(json.contains("\"max\":99"));
    }

    @Test
    public void testZeroHealth() {
        HealthData health = new HealthData(0, 99);
        String json = gson.toJson(health);

        assertTrue(json.contains("\"current\":0"));
        assertTrue(json.contains("\"max\":99"));
    }

    @Test
    public void testBoostedHealth() {
        HealthData health = new HealthData(115, 99);
        String json = gson.toJson(health);

        assertTrue(json.contains("\"current\":115"));
        assertTrue(json.contains("\"max\":99"));
    }

    @Test
    public void testLowLevelHealth() {
        HealthData health = new HealthData(10, 10);
        String json = gson.toJson(health);

        assertTrue(json.contains("\"current\":10"));
        assertTrue(json.contains("\"max\":10"));
    }
}
