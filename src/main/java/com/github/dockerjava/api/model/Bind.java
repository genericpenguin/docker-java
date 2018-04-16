package com.github.dockerjava.api.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a host path being bind mounted as a {@link Volume} in a Docker container.
 * The Bind can be in read only or read write access mode.
 */
public class Bind implements Serializable {
    private static final long serialVersionUID = 1L;

    private String path;

    private Volume volume;

    private AccessMode accessMode;

    /**
     * @since {@link com.github.dockerjava.core.RemoteApiVersion#VERSION_1_23}
     */
    private Boolean noCopy;

    /**
     * @since {@link com.github.dockerjava.core.RemoteApiVersion#VERSION_1_17}
     */
    private SELContext secMode;

    /**
     * @since {@link com.github.dockerjava.core.RemoteApiVersion#VERSION_1_22}
     */
    private PropagationMode propagationMode;

    public Bind(String path, Volume volume) {
        this(path, volume, AccessMode.DEFAULT, SELContext.DEFAULT);
    }

    public Bind(String path, Volume volume, Boolean noCopy) {
        this(path, volume, AccessMode.DEFAULT, SELContext.DEFAULT, noCopy);
    }

    public Bind(String path, Volume volume, AccessMode accessMode) {
        this(path, volume, accessMode, SELContext.DEFAULT);
    }

    public Bind(String path, Volume volume, AccessMode accessMode, SELContext secMode) {
        this(path, volume, accessMode, secMode, null);
    }

    public Bind(String path, Volume volume, AccessMode accessMode, SELContext secMode, Boolean noCopy) {
        this(path, volume, accessMode, secMode, noCopy, PropagationMode.DEFAULT_MODE);
    }

    public Bind(String path, Volume volume, AccessMode accessMode, SELContext secMode, Boolean noCopy, PropagationMode propagationMode) {
        this.path = path;
        this.volume = volume;
        this.accessMode = accessMode;
        this.secMode = secMode;
        this.noCopy = noCopy;
        this.propagationMode = propagationMode;
    }

    public String getPath() {
        return path;
    }

    public Volume getVolume() {
        return volume;
    }

    public AccessMode getAccessMode() {
        return accessMode;
    }

    public SELContext getSecMode() {
        return secMode;
    }

    public Boolean getNoCopy() {
        return noCopy;
    }

    public PropagationMode getPropagationMode() {
        return propagationMode;
    }

    /**
     * Parses a bind mount specification to a {@link Bind}.
     *
     * @param serialized
     *            the specification, e.g. <code>/host:/container:ro</code>
     * @return a {@link Bind} matching the specification
     * @throws IllegalArgumentException
     *             if the specification cannot be parsed
     */
    public static Bind parse(String serialized) {
        try {
            List<String> split = splitN(serialized, 3);
            String[] parts = new String[split.size()];
            parts = split.toArray(parts);

            switch (parts.length) {
            case 2: {
                return new Bind(parts[0], new Volume(parts[1]));
            }
            case 3: {
                String[] flags = parts[2].split(",");
                AccessMode accessMode = AccessMode.DEFAULT;
                SELContext seMode = SELContext.DEFAULT;
                Boolean nocopy = null;
                PropagationMode propagationMode = PropagationMode.DEFAULT_MODE;
                for (String p : flags) {
                    if (p.length() == 2) {
                        accessMode = AccessMode.valueOf(p.toLowerCase());
                    } else if ("nocopy".equals(p)) {
                        nocopy = true;
                    } else if (PropagationMode.SHARED.toString().equals(p)) {
                        propagationMode = PropagationMode.SHARED;
                    } else if (PropagationMode.SLAVE.toString().equals(p)) {
                        propagationMode = PropagationMode.SLAVE;
                    } else if (PropagationMode.PRIVATE.toString().equals(p)) {
                        propagationMode = PropagationMode.PRIVATE;
                    } else {
                        seMode = SELContext.fromString(p);
                    }
                }

                return new Bind(parts[0], new Volume(parts[1]), accessMode, seMode, nocopy, propagationMode);
            }
            default: {
                throw new IllegalArgumentException();
            }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing Bind '" + serialized + "'", e);
        }
    }
    // from https://github.com/robvanmieghem/docker/blob/master/volume/volume.go
    private static List<String> splitN(String raw, int n) {
        ArrayList<String> array = new ArrayList<String>();
        if ((raw.length() == 0) || (raw.charAt(0) == ':')) {
            return null;
        }

        // numberOfParts counts the number of parts separated by a separator colon
        int numberOfParts = 0;
        // left represents the left-most cursor in raw, updated at every `:` character considered as a separator.
        int left = 0;

        // right represents the right-most cursor in raw incremented with the loop. Note this
        // starts at index 1 as index 0 is already handle above as a special case.
        for (int right = 1; right < raw.length(); right++) {
            // stop parsing if reached maximum number of parts
            if ((n >= 0) && (numberOfParts >= n)) {
                break;
            }
            if (raw.charAt(right) != ':') {
                continue;
            }

            char potentialDriveLetter = raw.charAt(right - 1);
            if (((potentialDriveLetter >= 'A') && (potentialDriveLetter <= 'Z')) || ((potentialDriveLetter >= 'a') &&
                    (potentialDriveLetter <= 'z'))) {
                if (right > 1) {
                    char beforePotentialDriveLetter = raw.charAt(right - 2);
                    if ((beforePotentialDriveLetter != ':') &&
                            (beforePotentialDriveLetter != '/') &&
                            (beforePotentialDriveLetter != '\\')) {
                        // e.g. `C:` is not preceded by any delimiter, therefore it was not a drive letter
                        // but a path ending with `C:`.
                        array.add(raw.substring(left, right));
                        left = right + 1;
                        numberOfParts++;
                    }
                    // else, `C:` is considered as a drive letter and not as a delimiter, so
                    // we continue parsing.
                }
                // if right == 1, then `C:` is the beginning of the raw string, therefore `:` is again not
                // considered a delimiter and we continue parsing.
            } else {
                // if `:` is not preceded by a potential drive letter, then consider it as a delimiter.
                array.add(raw.substring(left, right));
                left = right + 1;
                numberOfParts++;
            }
        }
        // need to take care of the last part
        if (left < raw.length()) {
            if ((n >= 0) && (numberOfParts >= n)) {
                // if the maximum number of parts is reached, just append the rest to the last part
                // left-1 is at the last `:` that needs to be included since not considered a separator.
                String lastElement = array.get(n - 1);
                array.set(n - 1, lastElement + raw.substring(left));
            } else {
                array.add(raw.substring(left));
            }
        }
        return array;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Bind) {
            Bind other = (Bind) obj;
            return new EqualsBuilder()
                    .append(path, other.getPath())
                    .append(volume, other.getVolume())
                    .append(accessMode, other.getAccessMode())
                    .append(secMode, other.getSecMode())
                    .append(noCopy, other.getNoCopy())
                    .append(propagationMode, other.getPropagationMode())
                    .isEquals();
        } else {
            return super.equals(obj);
        }
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(path)
                .append(volume)
                .append(accessMode)
                .append(secMode)
                .append(noCopy)
                .append(propagationMode)
                .toHashCode();
    }

    /**
     * Returns a string representation of this {@link Bind} suitable for inclusion in a JSON message.
     * The format is <code>&lt;host path&gt;:&lt;container path&gt;:&lt;access mode&gt;</code>,
     * like the argument in {@link #parse(String)}.
     *
     * @return a string representation of this {@link Bind}
     */
    @Override
    public String toString() {
        return String.format("%s:%s:%s%s%s%s",
                path,
                volume.getPath(),
                accessMode.toString(),
                secMode != SELContext.none ? "," + secMode.toString() : "",
                noCopy != null ? ",nocopy" : "",
                propagationMode != PropagationMode.DEFAULT_MODE ? "," + propagationMode.toString() : "");
    }
}
