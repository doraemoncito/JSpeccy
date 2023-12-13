package gui.remote;

import com.sun.istack.NotNull;

import javax.swing.*;
import javax.swing.filechooser.FileView;
import java.io.File;
import java.util.Objects;

/**
 * Remote REST file system view class for use in JFileChoosers
 * @author jose.hernandez
 */
public class RemoteRestFileView extends FileView {

    @Override
    @NotNull
    public String getName(final File file) {

        if (file instanceof RemoteRestFile remoteRestFile) {
            return remoteRestFile.getDescription();
        }
        return file.getName();
    }

    @Override
    public Boolean isTraversable(final File file) {

        return file.isDirectory();
    }

    @Override
    public Icon getIcon(final File file) {

        boolean isDirectory = file.isDirectory();
        if (file instanceof RemoteRestFile remoteRestFile) {
            isDirectory = remoteRestFile.isDirectory();
        }

        String imagePath = isDirectory ? "/icons/fileopen.png" : "/icons/Akai24x24.png"; // NOI18N
        return new ImageIcon(Objects.requireNonNull(getClass().getResource(imagePath)));
    }

    @Override
    public String getDescription(final File file) {

        if (file instanceof RemoteRestFile remoteRestFile) {
            return remoteRestFile.getDescription();
        }
        return super.getDescription(file);
    }

    @Override
    public String getTypeDescription(final File file) {

        if (file instanceof RemoteRestFile remoteRestFile) {
            return remoteRestFile.getTypeDescription();
        }
        return super.getTypeDescription(file);
    }

}
