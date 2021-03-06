package Color_yr.AllMusic.Hud;

import com.google.gson.Gson;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;

public class Hud {
    public static final Object lock = new Object();
    public static String Info;
    public static String List;
    public static String Lyric;
    public static SaveOBJ save;
    private static ByteBuffer byteBuffer;
    private static final int textureID;
    private static boolean haveImg;

    private static final MatrixStack stack = new MatrixStack();

    static {
        textureID = GL11.glGenTextures();
    }

    public static void clear() {
        haveImg = false;
        Info = List = Lyric = "";
    }

    public static void stop() {
        clear();
        save = null;
    }

    public static void Set(String data) {
        synchronized (lock) {
            save = new Gson().fromJson(data, SaveOBJ.class);
        }
    }

    public static void SetImg(String picUrl) {
        if (picUrl != null) {
            try {
                var url = new URL(picUrl);
                var connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(4 * 1000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.105 Safari/537.36 Edg/84.0.522.52");
                connection.setRequestProperty("Host", "music.163.com");
                connection.connect();
                var inputStream = connection.getInputStream();
                var image = ImageIO.read(inputStream);
                int[] pixels = new int[image.getWidth() * image.getHeight()];
                image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
                byteBuffer = ByteBuffer.allocateDirect(image.getWidth() * image.getHeight() * 4);

                for (int h = 0; h < image.getHeight(); h++) {
                    for (int w = 0; w < image.getWidth(); w++) {
                        int pixel = pixels[h * image.getWidth() + w];

                        byteBuffer.put((byte) ((pixel >> 16) & 0xFF));
                        byteBuffer.put((byte) ((pixel >> 8) & 0xFF));
                        byteBuffer.put((byte) (pixel & 0xFF));
                        byteBuffer.put((byte) ((pixel >> 24) & 0xFF));
                    }
                }

                byteBuffer.flip();
                inputStream.close();
                Thread.sleep(500);
                MinecraftClient.getInstance().execute(() -> {
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
                    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, image.getWidth(), image.getHeight(),
                            0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, byteBuffer);

                    GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_NEAREST);
                    haveImg = true;
                });
            } catch (Exception e) {
                e.printStackTrace();
                haveImg = false;
            }
        }
    }

    public static void update() {
        var hud = MinecraftClient.getInstance().inGameHud;
        var textRenderer = hud.getFontRenderer();
        if (save == null)
            return;
        synchronized (lock) {
            if (save.isEnableInfo() && !Info.isEmpty()) {
                int offset = 0;
                String[] temp = Info.split("\n");
                for (String item : temp) {
                    textRenderer.drawWithShadow(stack, item, save.getInfo().getX(),
                            save.getInfo().getY() + offset, 0xffffff);
                    offset += 10;
                }
            }
            if (save.isEnableList() && !List.isEmpty()) {
                String[] temp = List.split("\n");
                int offset = 0;
                for (String item : temp) {
                    textRenderer.drawWithShadow(stack, item, save.getList().getX(),
                            save.getList().getY() + offset, 0xffffff);
                    offset += 10;
                }
            }
            if (save.isEnableLyric() && !Lyric.isEmpty()) {
                String[] temp = Lyric.split("\n");
                int offset = 0;
                for (String item : temp) {
                    textRenderer.drawWithShadow(stack, item, save.getLyric().getX(),
                            save.getLyric().getY() + offset, 0xffffff);
                    offset += 10;
                }
            }
            if (save.isEnablePic() && haveImg) {
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.setShaderTexture(0, textureID);
                DrawableHelper.drawTexture(new MatrixStack(), save.getPic().getX(), save.getPic().getY(),
                        0, 0, 0, 70, 70, 70, 70);
            }
        }
    }
}
