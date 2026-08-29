/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.blocks.fop;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.components.source.SourceUtil;
import org.apache.cocoon.serialization.AbstractSerializer;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceException;
import org.apache.excalibur.source.SourceResolver;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FopFactoryBuilder;
import org.apache.fop.configuration.DefaultConfigurationBuilder;
import org.apache.xmlgraphics.io.Resource;
import org.apache.xmlgraphics.io.ResourceResolver;

/**
 * FOP based serializer, for FOP 2.x.
 *
 * <p>Ported from the FOP 0.93-1.0 API, which was removed in FOP 2.0. Three things changed and
 * none of them has a drop-in replacement:
 *
 * <ul>
 * <li><code>FopFactory</code> is immutable and no longer has a no-argument
 *     <code>newInstance()</code>. It is built once, in {@link #configure}, through
 *     {@link FopFactoryBuilder} -- which is why the factory is no longer a field initialiser.</li>
 * <li><code>FopFactory.setUserConfig(Configuration)</code> is gone, and FOP no longer takes
 *     Avalon's <code>Configuration</code> at all: FOP 2.7 forked it into
 *     <code>org.apache.fop.configuration</code>. The user config is therefore parsed with FOP's
 *     own {@link DefaultConfigurationBuilder} straight from the source's stream, rather than
 *     through Avalon's <code>SAXConfigurationHandler</code>.</li>
 * <li><code>FopFactory.setURIResolver(URIResolver)</code> is gone, replaced by
 *     {@link ResourceResolver}, supplied at construction. {@link CocoonResourceResolver} adapts
 *     Cocoon's {@link SourceResolver} to it, so FOP still resolves <code>cocoon:</code>,
 *     <code>context:</code> and the other Cocoon protocols when it fetches images, fonts and
 *     included documents.</li>
 * </ul>
 *
 * <p>This class still implements {@link URIResolver}. FOP no longer calls it, but it is public
 * API of this serializer and costs nothing to keep.
 *
 * @version $Id$
 */
public class FOPNGSerializer extends AbstractSerializer
                             implements Configurable, CacheableProcessingComponent,
                                        Serviceable, URIResolver, Disposable {

    protected SourceResolver resolver;

    /**
     * Factory used to create fop objects. Immutable in FOP 2 and built once in
     * {@link #configure}, so it cannot be initialised here as it was under FOP 1.
     */
    protected FopFactory fopfactory;

    /**
     * The FOP instance.
     */
    protected Fop fop;

    /**
     * The current <code>mime-type</code>.
     */
    protected String mimetype;

    /**
     * Should we set the content length ?
     */
    protected boolean setContentLength = true;

    /**
     * Manager to get URLFactory from.
     */
    protected ServiceManager manager;
    private Map rendererOptions;


    /**
     * Set the component manager for this serializer.
     */
    public void service(ServiceManager manager) throws ServiceException {
        this.manager = manager;
        this.resolver = (SourceResolver) this.manager.lookup(SourceResolver.ROLE);
    }

    /**
     * Set the configurations for this serializer.
     */
    public void configure(Configuration conf) throws ConfigurationException {
        //should the content length be set
        this.setContentLength = conf.getChild("set-content-length").getValueAsBoolean(true);

        // FOP 2 resolves relative references against a base URI before handing them to the
        // resource resolver, so one has to exist. Anything Cocoon-specific arrives absolute
        // through CocoonResourceResolver, so this only affects plain relative hrefs.
        String baseUri = conf.getChild("base-uri").getValue(null);
        URI base;
        try {
            base = baseUri != null ? new URI(baseUri) : new File(".").getAbsoluteFile().toURI();
        } catch (URISyntaxException e) {
            throw new ConfigurationException("Not a valid base-uri: " + baseUri, e);
        }

        FopFactoryBuilder builder = new FopFactoryBuilder(base, new CocoonResourceResolver());

        String configUrl = conf.getChild("user-config").getValue(null);
        if (configUrl != null) {
            Source configSource = null;
            SourceResolver resolver = null;
            try {
                resolver = (SourceResolver)this.manager.lookup(SourceResolver.ROLE);
                configSource = resolver.resolveURI(configUrl);
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("Loading configuration from " + configSource.getURI());
                }
                // FOP 2.7 forked Avalon's Configuration into org.apache.fop.configuration, so
                // the old SAXConfigurationHandler route no longer produces a type FOP accepts.
                // FOP's own builder parses the same fop.xconf format from the stream.
                InputStream configStream = configSource.getInputStream();
                try {
                    builder.setConfiguration(new DefaultConfigurationBuilder().build(configStream));
                } finally {
                    configStream.close();
                }
            } catch (Exception e) {
                getLogger().warn("Cannot load configuration from " + configUrl);
                throw new ConfigurationException("Cannot load configuration from " + configUrl, e);
            } finally {
                if (resolver != null) {
                    resolver.release(configSource);
                    manager.release(resolver);
                }
            }
        }

        this.fopfactory = builder.build();

        // Get the mime type.
        this.mimetype = conf.getAttribute("mime-type");

        Configuration confRenderer = conf.getChild("renderer-config");
        if (confRenderer != null) {
            Configuration[] parameters = confRenderer.getChildren("parameter");
            if (parameters.length > 0) {
                rendererOptions = new HashMap();
                for (int i = 0; i < parameters.length; i++) {
                    String name = parameters[i].getAttribute("name");
                    String value = parameters[i].getAttribute("value");

                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("renderer " + String.valueOf(name) + " = " + String.valueOf(value));
                    }
                }
            }
        }
    }

    /**
     * Recycle serializer by removing references
     */
    public void recycle() {
        super.recycle();
        this.fop = null;
    }

    public void dispose() {
        if (this.resolver != null) {
            this.manager.release(this.resolver);
            this.resolver = null;
        }
        this.manager = null;
    }

    // -----------------------------------------------------------------

    /**
     * Return the MIME type.
     */
    public String getMimeType() {
        return mimetype;
    }

    /**
     * Create the FOP driver
     * Set the <code>OutputStream</code> where the XML should be serialized.
     * @throws IOException
     */
    public void setOutputStream(OutputStream out) throws IOException {

        // Give the source resolver to Batik which is used by FOP
        //SourceProtocolHandler.setup(this.resolver);

        FOUserAgent userAgent = fopfactory.newFOUserAgent();
        if (this.rendererOptions != null) {
            userAgent.getRendererOptions().putAll(this.rendererOptions);
        }
        try {
            this.fop = fopfactory.newFop(getMimeType(), userAgent, out);
            setContentHandler(this.fop.getDefaultHandler());
        } catch (FOPException e) {
            getLogger().error("FOP setup failed", e);
            throw new IOException("Unable to setup fop: " + e.getLocalizedMessage());
        }
    }

    /**
     * Generate the unique key.
     * This key must be unique inside the space of this component.
     * This method must be invoked before the generateValidity() method.
     *
     * @return The generated key or <code>0</code> if the component
     *              is currently not cacheable.
     */
    public Serializable getKey() {
        return "1";
    }

    /**
     * Generate the validity object.
     * Before this method can be invoked the generateKey() method
     * must be invoked.
     *
     * @return The generated validity object or <code>null</code> if the
     *         component is currently not cacheable.
     */
    public SourceValidity getValidity() {
        return NOPValidity.SHARED_INSTANCE;
    }

    /**
     * Test if the component wants to set the content length
     */
    public boolean shouldSetContentLength() {
        return this.setContentLength;
    }

    //From URIResolver, copied from TraxProcessor
    public javax.xml.transform.Source resolve(String href, String base) throws TransformerException {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("resolve(href = " + href + ", base = " + base + "); resolver = " + resolver);
        }

        StreamSource streamSource = null;
        Source source = null;
        try {
            if (base == null || href.indexOf(":") > 1) {
                // Null base - href must be an absolute URL
                source = resolver.resolveURI(href);
            } else if (href.length() == 0) {
                // Empty href resolves to base
                source = resolver.resolveURI(base);
            } else {
                // is the base a file or a real m_url
                if (!base.startsWith("file:")) {
                    int lastPathElementPos = base.lastIndexOf('/');
                    if (lastPathElementPos == -1) {
                        // this should never occur as the base should
                        // always be protocol:/....
                        return null; // we can't resolve this
                    } else {
                        source = resolver.resolveURI(base.substring(0, lastPathElementPos) + "/" + href);
                    }
                } else {
                    File parent = new File(base.substring(5));
                    File parent2 = new File(parent.getParentFile(), href);
                    source = resolver.resolveURI(parent2.toURL().toExternalForm());
                }
            }

            if (getLogger().isDebugEnabled()) {
                getLogger().debug("source = " + source + ", system id = " + source.getURI());
            }

            streamSource = new StreamSource(new ReleaseSourceInputStream(source.getInputStream(), source, resolver), source.getURI());
        } catch (SourceException e) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Failed to resolve " + href + "(base = " + base + "), return null", e);
            }

            // CZ: To obtain the same behaviour as when the resource is
            // transformed by the XSLT Transformer we should return null here.
            return null;
        } catch (java.net.MalformedURLException mue) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Failed to resolve " + href + "(base = " + base + "), return null", mue);
            }

            return null;
        } catch (IOException ioe) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Failed to resolve " + href + "(base = " + base + "), return null", ioe);
            }

            return null;
        } finally {
            // If streamSource is not null, the source should only be released when the input stream
            // is not needed anymore.
            if (streamSource == null)
                resolver.release(source);
        }
        return streamSource;
    }

    /**
     * Lets FOP fetch resources through Cocoon.
     *
     * <p>FOP 2 replaced <code>FopFactory.setURIResolver</code> with this interface. Without it,
     * FOP resolves images, fonts and included documents with its own plain URL handling and
     * every Cocoon protocol -- <code>cocoon:</code>, <code>context:</code>, <code>servlet:</code>
     * and the rest -- stops working inside an FO document.
     *
     * <p>The returned stream releases its {@link Source} on close, so FOP closing the resource
     * is what frees it. That is the same contract the {@link URIResolver} path already relied on
     * through {@link ReleaseSourceInputStream}.
     */
    private class CocoonResourceResolver implements ResourceResolver {

        public Resource getResource(URI uri) throws IOException {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("FOP requested resource " + uri);
            }
            Source source = null;
            try {
                source = resolver.resolveURI(uri.toASCIIString());
                return new Resource(
                        new ReleaseSourceInputStream(source.getInputStream(), source, resolver));
            } catch (IOException e) {
                if (source != null) {
                    resolver.release(source);
                }
                throw e;
            } catch (RuntimeException e) {
                if (source != null) {
                    resolver.release(source);
                }
                throw e;
            }
        }

        /**
         * FOP only calls this for output-producing configurations such as multi-file renderers,
         * which this serializer does not use -- it writes to the single stream given to
         * {@link FOPNGSerializer#setOutputStream}. Failing loudly beats returning somewhere
         * unexpected on disk.
         */
        public OutputStream getOutputStream(URI uri) throws IOException {
            throw new UnsupportedOperationException(
                    "FOPNGSerializer serializes to the pipeline's output stream; FOP asked to "
                    + "write to " + uri + ", which is not supported");
        }
    }

    /**
     * An InputStream which releases the Cocoon/Avalon source from which the InputStream
     * has been retrieved when the stream is closed.
     */
    public static class ReleaseSourceInputStream extends InputStream {
        private InputStream delegate;
        private Source source;
        private SourceResolver sourceResolver;

        private ReleaseSourceInputStream(InputStream delegate, Source source, SourceResolver sourceResolver) {
            this.delegate = delegate;
            this.source = source;
            this.sourceResolver = sourceResolver;
        }

        public void close() throws IOException {
            delegate.close();
            sourceResolver.release(source);
        }

        public int read() throws IOException {
            return delegate.read();
        }

        public int read(byte b[]) throws IOException {
            return delegate.read(b);
        }

        public int read(byte b[], int off, int len) throws IOException {
            return delegate.read(b, off, len);
        }

        public long skip(long n) throws IOException {
            return delegate.skip(n);
        }

        public int available() throws IOException {
            return delegate.available();
        }

        public synchronized void mark(int readlimit) {
            delegate.mark(readlimit);
        }

        public synchronized void reset() throws IOException {
            delegate.reset();
        }

        public boolean markSupported() {
            return delegate.markSupported();
        }
    }
}
