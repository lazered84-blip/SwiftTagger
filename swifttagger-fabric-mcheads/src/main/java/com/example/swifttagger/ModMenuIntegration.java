package com.example.swifttagger;

import com.example.swifttagger.config.SwiftTaggerConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Retire cette classe (et la dépendance ModMenu dans build.gradle et
 * fabric.mod.json) si tu ne veux pas dépendre de ModMenu.
 *
 * Corrigé : renvoyait un écran vide avec un simple "TODO" en titre, ce qui
 * donnait l'impression que "le Mod Menu ne fonctionne pas". Renvoie
 * maintenant le vrai écran de config (voir SwiftTaggerConfigScreen).
 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SwiftTaggerConfigScreen::create;
    }
}
