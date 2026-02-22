package haexporterplugin.utils;

import haexporterplugin.HAExporterConfig;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.audio.AudioPlayer;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class EasterEggUtils {
    @Inject
    private AudioPlayer audioPlayer;
    private @Inject HAExporterConfig config;

    public static ExecutorService executorService;

    public void init(){
        executorService = Executors.newSingleThreadExecutor();
    }

    public void shutDown() throws Exception
    {
        executorService.shutdown();
    }

    public void playKebab(){
        if (config.kebab())
            executorService.submit(() -> playSound("kebab"));
    }

    public void playGarbage(){
        if (config.kebab())
            executorService.submit(() -> playSound("garbage"));
    }

    private void playSound(String file)
    {
        String filename = String.format("/%s.wav", file);

        try
        {
            audioPlayer.play(this.getClass(), filename, 1);
        } catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }
}
