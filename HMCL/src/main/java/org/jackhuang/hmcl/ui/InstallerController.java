/*
 * Hello Minecraft! Launcher.
 * Copyright (C) 2017  huangyuhui <huanghongxun2008@126.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see {http://www.gnu.org/licenses/}.
 */
package org.jackhuang.hmcl.ui;

import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import org.jackhuang.hmcl.download.game.VersionJsonSaveTask;
import org.jackhuang.hmcl.game.GameVersion;
import org.jackhuang.hmcl.game.Library;
import org.jackhuang.hmcl.game.Version;
import org.jackhuang.hmcl.setting.Profile;
import org.jackhuang.hmcl.task.Schedulers;
import org.jackhuang.hmcl.task.Task;
import org.jackhuang.hmcl.ui.download.InstallerWizardProvider;

import java.util.LinkedList;
import java.util.function.Consumer;

public class InstallerController {
    private Profile profile;
    private String versionId;
    private Version version;
    @FXML
    private ScrollPane scrollPane;
    @FXML private VBox contentPane;
    private String forge;
    private String liteLoader;
    private String optiFine;

    public void initialize() {
        FXUtils.smoothScrolling(scrollPane);
    }

    public void loadVersion(Profile profile, String versionId) {
        this.profile = profile;
        this.versionId = versionId;
        this.version = profile.getRepository().getVersion(versionId).resolve(profile.getRepository());

        contentPane.getChildren().clear();
        forge = liteLoader = optiFine = null;

        for (Library library : version.getLibraries()) {
            Consumer<InstallerItem> removeAction = x -> {
                LinkedList<Library> newList = new LinkedList<>(version.getLibraries());
                newList.remove(library);
                new VersionJsonSaveTask(profile.getRepository(), version.setLibraries(newList))
                        .with(profile.getRepository().refreshVersionsAsync())
                        .with(Task.of(Schedulers.javafx(), () -> loadVersion(this.profile, this.versionId)))
                        .start();
            };

            if (library.getGroupId().equalsIgnoreCase("net.minecraftforge") && library.getArtifactId().equalsIgnoreCase("forge")) {
                contentPane.getChildren().add(new InstallerItem("Forge", library.getVersion(), removeAction));
                forge = library.getVersion();
            }
            if (library.getGroupId().equalsIgnoreCase("com.mumfrey") && library.getArtifactId().equalsIgnoreCase("liteloader")) {
                contentPane.getChildren().add(new InstallerItem("LiteLoader", library.getVersion(), removeAction));
                liteLoader = library.getVersion();
            }
            if (library.getGroupId().equalsIgnoreCase("net.optifine") && library.getArtifactId().equalsIgnoreCase("optifine")) {
                contentPane.getChildren().add(new InstallerItem("OptiFine", library.getVersion(), removeAction));
                optiFine = library.getVersion();
            }
        }
    }

    public void onAdd() {
        String gameVersion = GameVersion.minecraftVersion(profile.getRepository().getVersionJar(version));

        // TODO: if minecraftVersion returns null.
        if (gameVersion == null) return;

        Controllers.getDecorator().startWizard(new InstallerWizardProvider(profile, gameVersion, version, forge, liteLoader, optiFine));
    }
}
