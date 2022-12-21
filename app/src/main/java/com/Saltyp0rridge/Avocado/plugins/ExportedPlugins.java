package com.Saltyp0rridge.Avocado.plugins;

import com.Saltyp0rridge.Avocado.plugins.BatteryPlugin.BatteryPlugin;
import com.Saltyp0rridge.Avocado.plugins.MediaSession.MediaSessionPlugin;
import com.Saltyp0rridge.Avocado.plugins.MoneyPlugin.MoneyPlugin;
import com.Saltyp0rridge.Avocado.plugins.Notification.NotificationPlugin;
import com.Saltyp0rridge.Avocado.plugins.AlarmPlugin.AlarmPlugin;
import com.Saltyp0rridge.Avocado.plugins.TextPlugin.TextPlugin;

import java.util.ArrayList;

public class ExportedPlugins {
    public static ArrayList<BasePlugin> getPlugins() {
        ArrayList<BasePlugin> plugins = new ArrayList<>();
        plugins.add(new MediaSessionPlugin());
        plugins.add(new NotificationPlugin());
        plugins.add(new BatteryPlugin());
        plugins.add(new AlarmPlugin());
        plugins.add(new MoneyPlugin());
        plugins.add(new TextPlugin());
        return plugins;
    }
}
