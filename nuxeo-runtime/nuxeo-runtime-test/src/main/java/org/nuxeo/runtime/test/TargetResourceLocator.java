/*******************************************************************************
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *******************************************************************************/
package org.nuxeo.runtime.test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class TargetResourceLocator {

    protected final ClassLoader loader;

    protected final Path basepath;

    public TargetResourceLocator(Class<?> clazz) {
        basepath = basepath(clazz);
        loader = clazz.getClassLoader();
    }

    public Path getBasepath() {
        return basepath;
    }

    protected int depthOfClass(String name) {
        int depth = 0;
        int index = name.indexOf('.');
        while (index > 0) {
            depth += 1;
            index = name.indexOf('.', index + 1);
        }
        return depth;
    }

    protected Path basepath(Class<?> clazz) {
        String name = clazz.getName();
        int depth = depthOfClass(name) + 1;
        Path path;
        try {
            path = toPath(clazz.getResource("/".concat(name).replace('.', '/').concat(".class")).toURI());
            Path root = path.getRoot();
            if (path.getNameCount() > depth) {
                path = path.subpath(0, path.getNameCount() - depth);
                path = root.resolve(path);
            }
            return path;
        } catch (URISyntaxException | IOException cause) {
            throw new AssertionError("Cannot convert " + name + " to base dir", cause);
        }
    }

    public URL getTargetTestResource(String name) throws IOException {
        try {
            final Enumeration<URL> resources = loader.getResources(name);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (!resources.hasMoreElements()) {
                    return resource;
                }
                URI uri = resource.toURI();
                Path path = toPath(uri);
                if (path.getFileSystem().equals(basepath.getFileSystem()) && path.startsWith(basepath)) {
                    return path.toUri().toURL();
                }
            }
        } catch (URISyntaxException cause) {
            throw new AssertionError("Cannot find location of " + name, cause);
        }
        return null;
    }

    protected Path toPath(URI uri) throws IOException {
        final Map<String, String> env = new HashMap<>();
        final String[] array = uri.toString().split("!");
        if (array.length == 1) {
            return Paths.get(uri);
        }
        // work-around for jar paths on JDK7
        try (FileSystem fs = FileSystems.newFileSystem(URI.create(array[0]), env)) {
            return fs.getPath(array[1]);
        }
    }
}
