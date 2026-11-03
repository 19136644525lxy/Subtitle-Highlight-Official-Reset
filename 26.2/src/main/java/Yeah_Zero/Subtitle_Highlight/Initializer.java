package Yeah_Zero.Subtitle_Highlight;

import Yeah_Zero.Subtitle_Highlight.Command.CommandManager;
import Yeah_Zero.Subtitle_Highlight.Configure.Manager;
import Yeah_Zero.Subtitle_Highlight.Data.SubtitleTypeLoader;
import Yeah_Zero.Subtitle_Highlight.Keybind.KeybindManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class Initializer implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Manager.load();
        
        ClientCommandRegistrationCallback.EVENT.register(CommandManager::registerCommands);
        
        SubtitleTypeLoader.register();
        
        KeybindManager.registerKeybinds();
    }
}