package gui.remote;

import lombok.extern.slf4j.Slf4j;
import zxinfo.ZxinfoService;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Remote REST file system view class for use in JFileChoosers.
 *
 * @author jose.hernandez
 */
@Slf4j
public class RemoteRestFileSystemView extends FileSystemView {

    public static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final ZxinfoService zxinfoService;

    private static final RemoteRestFile ROOT = new RemoteRestFile("/", "World of Spectrum", null, true, null, null);

    private final Map<String, RemoteRestFile> parentCache;

    public RemoteRestFileSystemView(final ZxinfoService zxinfoService) {

        this.zxinfoService = zxinfoService;

        parentCache = ALPHABET.chars()
                .mapToObj(c -> String.valueOf((char) c))
                .map(letter -> new RemoteRestFile("/" + letter + "/", letter, null, true, null, ROOT))
                .collect(Collectors.toMap(RemoteRestFile::getDescription, Function.identity()));
    }

    @Override
    public File createNewFolder(final File containingDir) {

        // We cannot create new folders on the remote server as it is intended to be read-only.
        return null;
    }

    @Override
    public File getHomeDirectory() {

        return ROOT;
    }

    @Override
    public File getDefaultDirectory() {

        return getHomeDirectory();
    }

    @Override
    public boolean isFileSystemRoot(File dir) {

        return dir != null && dir.equals(ROOT);
    }

    @Override
    public File[] getFiles(final File dir, final boolean useFileHiding) {

        if (dir.getPath().equals(ROOT.getPath())) {
            // Split the list of games alphabetically into groups by the first letter of the name of the game file
            return parentCache.values().toArray(new RemoteRestFile[0]);
        }
        else {
            char letter = dir.getName().charAt(0);
            RemoteRestFile parent = parentCache.get(String.valueOf(letter));
            return zxinfoService.getGamesByLetter(letter).stream()
                    .flatMap(game -> game.getGameFiles().stream()
                            .map(gamefile -> new RemoteRestFile(gamefile.getPath(),
                                    game.getTitle() + ", " + gamefile, gamefile.getUrl(),
                                    false,
                                    gamefile.getFormat(),
                                    parent)))
                    .toArray(File[]::new);
        }
    }

    @Override
    public File[] getRoots() {

        return new File[]{ROOT};
    }

    @Override
    public String getSystemDisplayName(final File file) {

        return (file instanceof RemoteRestFile remoteRestFile)
                ? remoteRestFile.getDescription()
                : super.getSystemDisplayName(file);
    }

    @Override
    public Icon getSystemIcon(final File file) {

        if (file instanceof RemoteRestFile remoteRestFile) {
            String imagePath = (remoteRestFile.isDirectory()) ? "/icons/fileopen.png" : "/icons/Akai24x24.png"; // NOI18N
            return new ImageIcon(Objects.requireNonNull(getClass().getResource(imagePath)));
        }
        return super.getSystemIcon(file);
    }

    @Override
    public File getParentDirectory(File file) {

        if (!file.isDirectory()) {
            return null;
        }

        String path = file.getPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return new File(path);
    }

    @Override
    public File createFileObject(String pathname) {

        if (ROOT.getPath().equals(pathname)) {
            return ROOT;
        } else {
            return new RemoteRestFile(pathname, pathname, null, true, null, ROOT);
        }
    }

}
