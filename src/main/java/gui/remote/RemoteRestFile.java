package gui.remote;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;

/**
 * @author jose.hernandez
 */
@EqualsAndHashCode(callSuper = true)
@Getter
@Slf4j
public class RemoteRestFile extends File {

    private final URL url;

    // Long human-readable textual description of the file
    private final String description;

    // Long human-readable textual description of the file type
    private final String typeDescription;

    // True if this Object is meant to represent a directory, false otherwise.
    private final boolean isDirectory;

    private final RemoteRestFile parentFile;

    public RemoteRestFile(
            final String pathname,
            final String description,
            final URL url,
            final boolean isDirectory,
            final String typeDescription,
            final RemoteRestFile parentFile) {
        /* Remove the zip extension from the path so that the file chooser filters can work as expected.
         * We will still know from the URL that the target file is actually a zip fie.
         */
        super(pathname.replace(".zip", ""));
        this.description = description;
        this.url = url;
        this.isDirectory = isDirectory;
        this.typeDescription = typeDescription;
        this.parentFile = parentFile;
    }

    @Override
    public boolean isDirectory() {

        return isDirectory;
    }

    @Override
    public boolean exists() {

        return true;
    }

    @Override
    public boolean canRead() {

        return true;
    }

    @Override
    public boolean canWrite() {

        return false;
    }

    @Override
    public String getParent() {

        return (parentFile != null) ? parentFile.getPath() : super.getParent();
    }

    @Override
    public File getParentFile() {

        return parentFile;
    }

    @Override
    public int compareTo(final File file) {

        if (file instanceof RemoteRestFile remoteRestFile) {
            return this.description.compareToIgnoreCase(remoteRestFile.getDescription());
        }
        return super.compareTo(file);
    }

}
