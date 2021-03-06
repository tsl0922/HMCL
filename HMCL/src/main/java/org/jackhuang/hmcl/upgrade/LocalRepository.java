/*
 * Hello Minecraft! Launcher.
 * Copyright (C) 2018  huangyuhui <huanghongxun2008@126.com>
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
package org.jackhuang.hmcl.upgrade;

import org.jackhuang.hmcl.Launcher;
import org.jackhuang.hmcl.task.FileDownloadTask;
import org.jackhuang.hmcl.util.JarUtils;
import org.tukaani.xz.XZInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.logging.Level;

import static org.jackhuang.hmcl.util.Logging.LOG;

/**
 * A class used to manage the local HMCL repository.
 *
 * @author yushijinhun
 */
final class LocalRepository {
    private LocalRepository() {}

    private static Path localStorage = Launcher.HMCL_DIRECTORY.toPath().resolve("hmcl.jar");

    /**
     * Gets the current stored executable in local repository.
     */
    public static Optional<LocalVersion> getStored() {
        if (!Files.isRegularFile(localStorage)) {
            return Optional.empty();
        }
        return Optional.of(localStorage)
                .flatMap(JarUtils::getImplementationVersion)
                .map(version -> new LocalVersion(version, localStorage));
    }

    private static void writeToStorage(Path source, boolean checkHeaders) throws IOException {
        IntegrityChecker.requireVerifiedJar(source);
        Files.createDirectories(localStorage.getParent());
        if (checkHeaders) {
            ExecutableHeaderHelper.copyWithoutHeader(source, localStorage);
        } else {
            Files.copy(source, localStorage, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Creates a task that downloads the given version to local repository.
     */
    public static FileDownloadTask downloadFromRemote(RemoteVersion version) throws IOException {
        Path downloaded = Files.createTempFile("hmcl-update-", null);
        return new FileDownloadTask(new URL(version.getUrl()), downloaded.toFile(), version.getIntegrityCheck()) {
            @Override
            public void execute() throws Exception {
                super.execute();

                try {
                    switch (version.getType()) {
                        case JAR:
                            writeToStorage(downloaded, false);
                            break;

                        case PACK_XZ:
                            Path unpacked = Files.createTempFile("hmcl-update-unpack-", null);
                            try {
                                try (InputStream in = new XZInputStream(Files.newInputStream(downloaded));
                                        JarOutputStream out = new JarOutputStream(Files.newOutputStream(unpacked))) {
                                    Pack200.newUnpacker().unpack(in, out);
                                }
                                writeToStorage(unpacked, false);
                            } finally {
                                Files.deleteIfExists(unpacked);
                            }
                            break;

                        default:
                            throw new IllegalArgumentException("Unknown type: " + version.getType());
                    }
                } finally {
                    Files.deleteIfExists(downloaded);
                }
            }
        };
    }

    /**
     * Copies the current HMCL executable to local repository.
     */
    public static void downloadFromCurrent() {
        Optional<LocalVersion> current = LocalVersion.current();
        if (current.isPresent()) {
            Path currentPath = current.get().getLocation();
            if (!Files.isRegularFile(currentPath)) {
                LOG.warning("Failed to download " + current.get() + ", it isn't a file");
                return;
            }
            if (isSameAsLocalStorage(currentPath)) {
                LOG.warning("Trying to download from self, ignored");
                return;
            }
            LOG.info("Downloading " + current.get());
            try {
                writeToStorage(current.get().getLocation(), true);
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Failed to download " + current.get(), e);
            }
        }
    }

    /**
     * Writes the executable stored in local repository to the given location.
     */
    public static void applyTo(Path target) throws IOException {
        if (isSameAsLocalStorage(target)) {
            throw new IOException("Cannot apply update to self");
        }

        LOG.info("Applying update to " + target);
        IntegrityChecker.requireVerifiedJar(localStorage);
        ExecutableHeaderHelper.copyWithHeader(localStorage, target);
    }

    private static boolean isSameAsLocalStorage(Path path) {
        return path.toAbsolutePath().equals(localStorage.toAbsolutePath());
    }
}
