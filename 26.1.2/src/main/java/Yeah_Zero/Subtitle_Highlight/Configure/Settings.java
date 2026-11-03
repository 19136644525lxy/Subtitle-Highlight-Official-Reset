package Yeah_Zero.Subtitle_Highlight.Configure;

import Yeah_Zero.Subtitle_Highlight.Util.ColorCode;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;

public class Settings {
    public long maxDuration = 3000;
    public double startRatio = 1;
    public double endRatio = 0.29411764705882354;
    public int backgroundColor = 0xcc000000;
    public float scale = 1;
    public float bottomMargin = 30;
    public float sideMargin = 1;
    public Boolean ikunEasterEgg = true;
    /** 数据包颜色是否优先于配置界面；默认 false，即配置界面优先。 */
    public boolean datapackPriority = false;
    public ColorSetting colorSettings = new ColorSetting();
    public ArrayList<Custom> customList = new ArrayList<>();

    public class ColorSetting {
        public ColorCode ambient = ColorCode.DARK_BLUE;
        public Block block = new Block();
        public ColorCode enchant = ColorCode.LIGHT_PURPLE;
        public Entity entity = new Entity();
        public Item item = new Item();
        public ColorCode other = ColorCode.GRAY;

        public class Item {
            public ColorCode weapon = ColorCode.RED;
            public ColorCode armor = ColorCode.GOLD;
            public ColorCode tool = ColorCode.AQUA;
            public ColorCode other = ColorCode.GRAY;
        }

        public class Block {
            public ColorCode generic = ColorCode.GRAY;
            public ColorCode interact = ColorCode.GREEN;
            public ColorCode working = ColorCode.YELLOW;
            public ColorCode dangerous = ColorCode.RED;
            public ColorCode crop = ColorCode.GREEN;
            public ColorCode other = ColorCode.GRAY;
        }

        public class Entity {
            public Mob mob = new Mob();
            public ColorCode vehicle = ColorCode.GRAY;
            public ColorCode projectile = ColorCode.AQUA;
            public ColorCode explosive = ColorCode.RED;
            public ColorCode decoration = ColorCode.GRAY;
            public ColorCode other = ColorCode.GRAY;

            public class Mob {

                public Player player = new Player();
                public ColorCode passive = ColorCode.GREEN;
                public ColorCode neutral = ColorCode.YELLOW;
                public ColorCode hostile = ColorCode.RED;
                public ColorCode boss = ColorCode.DARK_PURPLE;

                public class Player {
                    public ColorCode attack = ColorCode.RED;
                    public ColorCode hurt = ColorCode.RED;
                    public ColorCode other = ColorCode.WHITE;
                }
            }
        }
    }

    public class Custom {
        public String translationKey = "";
        public TextColor color = TextColor.fromRgb(0xffffff);
        public Boolean obfuscated = false;
        public Boolean bold = false;
        public Boolean strikethrough = false;
        public Boolean underline = false;
        public Boolean italic = false;
    }
}