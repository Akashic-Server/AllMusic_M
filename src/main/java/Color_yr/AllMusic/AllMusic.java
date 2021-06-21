package Color_yr.AllMusic;

import Color_yr.AllMusic.Hud.Hud;
import Color_yr.AllMusic.player.APlayer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AllMusic implements ModInitializer {
    public static final Identifier ID = new Identifier("allmusic", "channel");
    private static final APlayer nowPlaying = new APlayer();
    public static boolean isPlay = false;
    public static int v = -1;
    private static URL nowURL;

    public final Thread thread = new Thread(() -> {
        while (true) {
            try {
                if (MinecraftClient.getInstance().options != null) {
                    int nowV = (int) (MinecraftClient.getInstance().options.getSoundVolume(SoundCategory.RECORDS) *
                            MinecraftClient.getInstance().options.getSoundVolume(SoundCategory.MASTER) * 100);
                    if (v != nowV) {
                        nowPlaying.Set(nowV);
                    }
                }
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    });

    public static void onServerQuit() {
        try {
            nowPlaying.close();
            Hud.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static URL Get(URL url) {
        if (url.toString().contains("https://music.163.com/song/media/outer/url?id=")
                || url.toString().contains("http://music.163.com/song/media/outer/url?id=")) {
            try {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(4 * 1000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.105 Safari/537.36 Edg/84.0.522.52");
                connection.setRequestProperty("Host", "music.163.com");
                connection.connect();
                if (connection.getResponseCode() == 302) {
                    return new URL(connection.getHeaderField("Location"));
                }
                return connection.getURL();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return url;
    }

    public static void onClicentPacket(final String message) {
        final Thread asyncThread = new Thread(() -> {
            try {
                if (message.equals("[Stop]")) {
                    stopPlaying();
                } else if (message.startsWith("[Play]")) {
                    MinecraftClient.getInstance().getSoundManager().stopSounds(null, SoundCategory.MUSIC);
                    MinecraftClient.getInstance().getSoundManager().stopSounds(null, SoundCategory.RECORDS);
                    stopPlaying();
                    nowURL = new URL(message.replace("[Play]", ""));
                    nowURL = Get(nowURL);
                    if (nowURL == null)
                        return;
                    nowPlaying.SetMusic(nowURL);
                    nowPlaying.play();
                } else if (message.startsWith("[Lyric]")) {
                    Hud.Lyric = message.substring(7);
                } else if (message.startsWith("[Info]")) {
                    Hud.Info = message.substring(6);
                } else if (message.startsWith("[List]")) {
                    Hud.List = message.substring(6);
                } else if (message.startsWith("[Img]")) {
                    Hud.SetImg(message.substring(5));
                } else if (message.equalsIgnoreCase("[clear]")) {
                    Hud.clear();
                } else if (message.startsWith("{")) {
                    Hud.Set(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        asyncThread.start();
    }

    private static void stopPlaying() {
        try {
            nowPlaying.close();
            Hud.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onInitialize() {
        ClientPlayNetworking.registerGlobalReceiver(ID, (client, handler, buffer, responseSender) -> {
            try {
                byte[] buff = new byte[buffer.readableBytes()];
                buffer.readBytes(buff);
                buff[0] = 0;
                String data = new String(buff, StandardCharsets.UTF_8).substring(1);
                onClicentPacket(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }
}
