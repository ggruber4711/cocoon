/*
 * Copyright 1999-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.generation;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;

import org.apache.cocoon.Constants;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.components.search.LuceneCocoonHelper;
import org.apache.cocoon.components.search.LuceneCocoonPager;
import org.apache.cocoon.components.search.LuceneCocoonSearcher;
import org.apache.cocoon.components.search.LuceneXMLIndexer;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.Hits;
import org.apache.lucene.store.Directory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Generates an XML representation of a search result.
 *
 * <p>
 *  This generator generates xml content representening an XML search.
 *  The generated xml content contains the search result,
 *  the search query information, and navigation information about the
 *  search results.
 *  The query is sent to the generator, either via the 'queryString' request parameter
 *  or the 'query' SiteMap parameter. The sitemap overides the request.
 * </p>
 *
 * <p>
 *  Search xml sample generated by this generator:
 * </p>
 * <pre><tt>
 * &lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;
 *
 * &lt;search:results date=&quot;1008437081064&quot; query-string=&quot;cocoon&quot;
 *     start-index=&quot;0&quot; page-length=&quot;10&quot;
 *     xmlns:search=&quot;http://apache.org/cocoon/search/1.0&quot;
 *     xmlns:xlink=&quot;http://www.w3.org/1999/xlink&quot;&gt;
 *   &lt;search:hits total-count=&quot;125&quot; count-of-pages=&quot;13&quot;&gt;
 *     &lt;search:hit rank=&quot;0&quot; score=&quot;1.0&quot;
 *         uri=&quot;http://localhost:8080/cocoon/documents/hosting.html&quot;&gt;
 *       &lt;search:field name="title"&gt;Document Title&lt;search:field/&gt;
 *     &lt;search:hit/&gt;
 *     ...
 *   &lt;/search:hits&gt;
 *
 *   &lt;search:navigation total-count=&quot;125&quot; count-of-pages=&quot;13&quot;
 *       has-next=&quot;true&quot; has-previous=&quot;false&quot; next-index=&quot;10&quot; previous-index=&quot;0&quot;&gt;
 *     &lt;search:navigation-page start-index=&quot;0&quot;/&gt;
 *     &lt;search:navigation-page start-index=&quot;10&quot;/&gt;
 *     ...
 *     &lt;search:navigation-page start-index=&quot;120&quot;/&gt;
 *   &lt;/search:navigation&gt;
 * &lt;/search:results&gt;
 * </tt></pre>
 *
 * @author <a href="mailto:berni_huber@a1.net">Bernhard Huber</a>
 * @author <a href="mailto:vgritsenko@apache.org">Vadim Gritsenko</a>
 * @author <a href="mailto:jeremy@apache.org">Jeremy Quinn</a>
 * @author <a href="mailto:conal@nzetc.org">Conal Tuohy</a>
 * @version CVS $Id$
 */
public class SearchGenerator extends ServiceableGenerator
    implements Contextualizable, Disposable
{

    /**
     * The XML namespace for the output document.
     */
    protected final static String NAMESPACE = "http://apache.org/cocoon/search/1.0";

    /**
     * The XML namespace prefix for the output document.
     */
    protected final static String PREFIX = "search";

    /**
     * The XML namespace for xlink
     */
    protected final static String XLINK_NAMESPACE = "http://www.w3.org/1999/xlink";

    /**
     * Description of the Field
     */
    protected final static String CDATA = "CDATA";

    /**
     * Name of root element of generated xml content, ie <code>results</code>.
     */
    protected final static String RESULTS_ELEMENT = "results";

    /**
     * Qualified name of root element of generated xml content, ie <code>search:results</code>.
     */
    protected final static String Q_RESULTS_ELEMENT = PREFIX + ":" + RESULTS_ELEMENT;


    /**
     * Attribute <code>date</code> of <code>results</code> element.
     * It contains the date a long value, indicating when a search
     * generated this xml content.
     */
    protected final static String DATE_ATTRIBUTE = "date";

    /**
     * Attribute <code>query-string</code> of <code>results</code> element.
     * Echos the <code>queryString</code> query parameter.
     */
    protected final static String QUERY_STRING_ATTRIBUTE = "query-string";

    /**
     * Attribute <code>start-index</code> of <code>results</code> element.
     * Echoes the <code>startIndex</code> query parameter.
     */
    protected final static String START_INDEX_ATTRIBUTE = "start-index";

    /**
     * Attribute <code>page-length</code> of <code>results</code> element.
     * Echoes the <code>pageLenth</code> query parameter.
     */
    protected final static String PAGE_LENGTH_ATTRIBUTE = "page-length";

    /**
     * Attribute <code>name</code> of <code>hit</code> element.
     */
    protected final static String NAME_ATTRIBUTE = "name";

    /**
     * Child element of generated xml content, ie <code>hits</code>.
     * This element describes all hits.
     */
    protected final static String HITS_ELEMENT = "hits";

    /**
     * QName of child element of generated xml content, ie <code>search:hits</code>.
     * This element describes all hits.
     */
    protected final static String Q_HITS_ELEMENT = PREFIX + ":" + HITS_ELEMENT;

    /**
     * Attribute <code>total-count</code> of <code>hits</code> element.
     * The value describes total number of hits found by the search engine.
     */
    protected final static String TOTAL_COUNT_ATTRIBUTE = "total-count";

    /**
     * Attribute <code>count-of-pages</code> of <code>hits</code> element.
     * The value describes number of pages needed for all hits.
     */
    protected final static String COUNT_OF_PAGES_ATTRIBUTE = "count-of-pages";

    /**
     * Child element of generated xml content, ie <code>hit</code>.
     * This element describes a single hit.
     */
    protected final static String HIT_ELEMENT = "hit";

    /**
     * QName of child element of generated xml content, ie <code>search:hit</code>.
     * This element describes a single hit.
     */
    protected final static String Q_HIT_ELEMENT = PREFIX + ":" + HIT_ELEMENT;

    /**
     * Attribute <code>rank</code> of <code>hit</code> element.
     * The value describes the count index of this hits, ranging between 0, and
     * total-count minus 1.
     */
    protected final static String RANK_ATTRIBUTE = "rank";

    /**
     * Attribute <code>score</code> of <code>hit</code> element.
     * The value describes the score of this hits, ranging between 0, and 1.0.
     */
    protected final static String SCORE_ATTRIBUTE = "score";

    /**
     * Attribute <code>uri</code> of <code>hit</code> element.
     * The value describes the uri of a document matching the search query.
     */
    protected final static String URI_ATTRIBUTE = "uri";

    /**
     * Child element <code>field</code> of the <code>hit</code> element.
     * This element contains value of the stored field of a hit.
     *
     * @since 2.0.4
     */
    protected final static String FIELD_ELEMENT = "field";

    /**
     * QName of child element <code>search:field</code> of the <code>hit</code> element.
     */
    protected final static String Q_FIELD_ELEMENT = PREFIX + ":" + FIELD_ELEMENT;

    /**
     * Child element of generated xml content, ie <code>navigation</code>.
     * This element describes some hints for easier navigation.
     */
    protected final static String NAVIGATION_ELEMENT = "navigation";

    /**
     * QName of child element of generated xml content, ie <code>search:navigation</code>.
     */
    protected final static String Q_NAVIGATION_ELEMENT = PREFIX + ":" + NAVIGATION_ELEMENT;

    /**
     * Child element of generated xml content, ie <code>navigation-page</code>.
     * This element describes the start-index of page containing hits.
     */
    protected final static String NAVIGATION_PAGE_ELEMENT = "navigation-page";

    /**
     * QName of child element of generated xml content, ie <code>search:navigation-page</code>.
     * This element describes the start-index of page containing hits.
     */
    protected final static String Q_NAVIGATION_PAGE_ELEMENT = PREFIX + ":" + NAVIGATION_PAGE_ELEMENT;

    /**
     * Attribute <code>has-next</code> of <code>navigation-page</code> element.
     * The value is true if a next navigation control should be presented.
     */
    protected final static String HAS_NEXT_ATTRIBUTE = "has-next";

    /**
     * Attribute <code>has-next</code> of <code>navigation-page</code> element.
     * The value is true if a previous navigation control should be presented.
     */
    protected final static String HAS_PREVIOUS_ATTRIBUTE = "has-previous";

    /**
     * Attribute <code>next-index</code> of <code>navigation-page</code> element.
     * The value describes the start-index of the next-to-be-presented page.
     */
    protected final static String NEXT_INDEX_ATTRIBUTE = "next-index";

    /**
     * Attribute <code>previous-index</code> of <code>navigation-page</code> element.
     * The value describes the start-index of the previous-to-be-presented page.
     */
    protected final static String PREVIOUS_INDEX_ATTRIBUTE = "previous-index";

    /**
     * Setup parameter name of index directory, ie <code>index</code>.
     */
    protected final static String INDEX_PARAM = "index";

    /**
     * Default value of setup parameter <code>index</code>, ie <code>index</code>.
     */
    protected final static String INDEX_PARAM_DEFAULT = "index";

    /**
     * Setup the actual query from generator parameter,
     * ie <code>query</code>.
     */
    protected final static String QUERY_PARAM = "query";

    /**
     * Setup parameter name specifying the name of query-string query parameter,
     * ie <code>query-string</code>.
     */
    protected final static String QUERY_STRING_PARAM = "query-string";

    /**
     * Default value of setup parameter <code>query-string</code>, ie <code>queryString</code>.
     */
    protected final static String QUERY_STRING_PARAM_DEFAULT = "queryString";

    /**
     * Setup parameter name specifying the name of start-index query parameter,
     * ie <code>start-index</code>.
     */
    protected final static String START_INDEX_PARAM = "start-index";

    /**
     * Default value of setup parameter <code>start-index</code>, ie <code>startIndex</code>.
     */
    protected final static String START_INDEX_PARAM_DEFAULT = "startIndex";

    /**
     * Setup parameter name specifying the name of start-next-index query parameter,
     * ie <code>start-next-index</code>.
     */
    protected final static String START_INDEX_NEXT_PARAM = "start-next-index";

    /**
     * Default value of setup parameter <code>start-next-index</code>, ie <code>startNextIndex</code>.
     */
    protected final static String START_INDEX_NEXT_PARAM_DEFAULT = "startNextIndex";

    /**
     * Setup parameter name specifying the name of start-previous-index query parameter,
     * ie <code>start-previous-index</code>.
     */
    protected final static String START_INDEX_PREVIOUS_PARAM = "start-previous-index";

    /**
     * Default value of setup parameter <code>start-previous-index</code>, ie <code>startPreviousIndex</code>.
     */
    protected final static String START_INDEX_PREVIOUS_PARAM_DEFAULT = "startPreviousIndex";

    /**
     *Description of the Field
     *
     * @since
     */
    protected final static int START_INDEX_DEFAULT = 0;

    /**
     * Setup parameter name specifying the name of page-length query parameter,
     * ie <code>page-length</code>.
     */
    protected final static String PAGE_LENGTH_PARAM = "page-length";

    /**
     *Description of the Field
     *
     * @since
     */
    protected final static String PAGE_LENGTH_PARAM_DEFAULT = "pageLength";

    /**
     *Description of the Field
     *
     * @since
     */
    protected final static int PAGE_LENGTH_DEFAULT = 10;



    /**
     * Default home directory of index directories.
     * <p>
     *   Releative index directories specified in the setup of this generator are resolved
     *   relative to this directory.
     * </p>
     * <p>
     *   By default this directory is set to the <code>WORKING_DIR</code> of Cocoon.
     * </p>
     */
    private File workDir = null;

    /**
     * The avalon component to use for searching.
     */
    private LuceneCocoonSearcher lcs;

    /**
     * Absolute filesystem directory of lucene index directory
     */
    private File index = null;

    /**
     * Query-string to search for
     */
    private String queryString = "";

    /**
     * Attributes used when generating xml content.
     */
    private final AttributesImpl atts = new AttributesImpl();

    /**
     * startIndex of query parameter
     */
    private Integer startIndex = null;

    /**
     * pageLength of query parameter
     */
    private Integer pageLength = null;


    // TODO: parameterize()

    /**
     * Set the current <code>ServiceManager</code> instance used by this
     * <code>Serviceable</code>.
     */
    public void service(ServiceManager manager) throws ServiceException {
        super.service(manager);
    }

    /**
     * setup all members of this generator.
     *
     * @since
     */
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters par)
             throws ProcessingException, SAXException, IOException {
        super.setup(resolver, objectModel, src, par);

        try {
            lcs = (LuceneCocoonSearcher) this.manager.lookup(LuceneCocoonSearcher.ROLE);
        } catch (ServiceException e) {
            throw new ProcessingException("Unable to lookup " + LuceneCocoonSearcher.ROLE, e);
        }

        String param_name;
        Request request = ObjectModelHelper.getRequest(objectModel);

        String index_file_name = par.getParameter(INDEX_PARAM, INDEX_PARAM_DEFAULT);
        if (request.getParameter(INDEX_PARAM) != null) {
            index_file_name = request.getParameter(INDEX_PARAM);
        }

        // now set the index
        index = new File(index_file_name);
        if (!index.isAbsolute()) {
            index = new File(workDir, index.toString());
        }

        // try getting the queryString from the generator sitemap params

        queryString = par.getParameter(QUERY_PARAM, "");

        // try getting the queryString from the request params
        if (queryString.length() == 0) {
            param_name = par.getParameter(QUERY_STRING_PARAM, QUERY_STRING_PARAM_DEFAULT);
            if (request.getParameter(param_name) != null) {
                queryString = request.getParameter(param_name);
            }
        }
        // always try lookup the start index from the request params
        // get startIndex
        startIndex = null;
        param_name = par.getParameter(START_INDEX_NEXT_PARAM, START_INDEX_NEXT_PARAM_DEFAULT);
        if (request.getParameter(param_name) != null) {
            startIndex = createInteger(request.getParameter(param_name));
        }

        if (startIndex == null) {
            param_name = par.getParameter(START_INDEX_PREVIOUS_PARAM, START_INDEX_PREVIOUS_PARAM_DEFAULT);
            if (request.getParameter(param_name) != null) {
                startIndex = createInteger(request.getParameter(param_name));
            }
        }
        if (startIndex == null) {
            param_name = par.getParameter(START_INDEX_PARAM, START_INDEX_PARAM_DEFAULT);
            if (request.getParameter(param_name) != null) {
                startIndex = createInteger(request.getParameter(param_name));
            }
        }

        // get pageLength
        param_name = par.getParameter(PAGE_LENGTH_PARAM, PAGE_LENGTH_PARAM_DEFAULT);
        if (request.getParameter(param_name) != null) {
            pageLength = createInteger(request.getParameter(param_name));
        }
    }


    /**
     * Contextualize this class.
     *
     * <p>
     *   Especially retrieve the work directory.
     *   If the index directory is specified relativly, the working directory is
     *   used as home directory of the index directory.
     * </p>
     *
     * @param  context               Context to use
     * @exception  ContextException  If contextualizing fails.
     * @since
     */
    public void contextualize(Context context) throws ContextException {
        // retrieve the working directory, assuming that the index may reside there
        workDir = (File) context.get(Constants.CONTEXT_WORK_DIR);
    }

    /**
     * Generate xml content describing search results.
     * Entry point of the ComposerGenerator.
     * The xml content is generated from the hits object.
     *
     *
     * @exception  IOException       when there is a problem reading the from file system.
     * @since
     * @throws  SAXException         when there is a problem creating the output SAX events.
     * @throws  ProcessingException  when there is a problem obtaining the hits
     */
    public void generate() throws IOException, SAXException, ProcessingException {
        // set default parameter value, in case of no values are set yet.
        if (startIndex == null) {
            startIndex = new Integer(START_INDEX_DEFAULT);
        }
        if (pageLength == null) {
            pageLength = new Integer(PAGE_LENGTH_DEFAULT);
        }

        // Start the document and set the namespace.
        this.contentHandler.startDocument();
        this.contentHandler.startPrefixMapping(PREFIX, NAMESPACE);
        this.contentHandler.startPrefixMapping("xlink", XLINK_NAMESPACE);

        generateResults();

        // End the document.
        this.contentHandler.endPrefixMapping("xlink");
        this.contentHandler.endPrefixMapping(PREFIX);
        this.contentHandler.endDocument();
    }


    /**
     * Create an Integer.
     * <p>
     *   Create an Integer from String s, if conversion fails return null.
     * </p>
     *
     * @param  s  Converting s to an Integer
     * @return    Integer converted value originating from s, or null
     * @since
     */
    private Integer createInteger(String s) {
        Integer i = null;
        try {
            i = new Integer(s);
        } catch (NumberFormatException nfe) {
            // ignore it, write only warning
            if (getLogger().isWarnEnabled()) {
                getLogger().warn("Cannot convert " + s + " to Integer", nfe);
            }
        }
        return i;
    }


    /**
     * Build and generate the search results.
     * <p>
     *  First build the hits, next generate xml content from the hits,
     *  taking page index, and length into account.
     * </p>
     *
     * @since
     * @throws  SAXException         when there is a problem creating the output SAX events.
     * @throws  ProcessingException  when there is a problem obtaining the hits
     */
    private void generateResults() throws SAXException, ProcessingException {

        // Make the hits
        LuceneCocoonPager pager = buildHits();

        // The current date and time.
        long time = System.currentTimeMillis();

        atts.clear();
        atts.addAttribute("", DATE_ATTRIBUTE, DATE_ATTRIBUTE, CDATA, String.valueOf(time));
        if (queryString != null && queryString.length() > 0)
            atts.addAttribute("", QUERY_STRING_ATTRIBUTE, QUERY_STRING_ATTRIBUTE, CDATA, String.valueOf(queryString));
        atts.addAttribute("", START_INDEX_ATTRIBUTE, START_INDEX_ATTRIBUTE, CDATA, String.valueOf(startIndex));
        atts.addAttribute("", PAGE_LENGTH_ATTRIBUTE, PAGE_LENGTH_ATTRIBUTE, CDATA, String.valueOf(pageLength));

        contentHandler.startElement(NAMESPACE, RESULTS_ELEMENT, Q_RESULTS_ELEMENT, atts);

        // build xml from the hits
        generateHits(pager);
        generateNavigation(pager);

        // End root element.
        contentHandler.endElement(NAMESPACE, RESULTS_ELEMENT, Q_RESULTS_ELEMENT);
    }


    /**
     * Generate the xml content of all hits
     *
     * @param  pager                 the LuceneContentPager with the search results
     * @since
     * @throws  SAXException         when there is a problem creating the output SAX events.
     */
    private void generateHits(LuceneCocoonPager pager) throws SAXException {
        if (pager != null) {
            atts.clear();
            atts.addAttribute("", TOTAL_COUNT_ATTRIBUTE, TOTAL_COUNT_ATTRIBUTE,
                CDATA, String.valueOf(pager.getCountOfHits()));
            atts.addAttribute("", COUNT_OF_PAGES_ATTRIBUTE, COUNT_OF_PAGES_ATTRIBUTE,
                CDATA, String.valueOf(pager.getCountOfPages()));
            contentHandler.startElement(NAMESPACE, HITS_ELEMENT, Q_HITS_ELEMENT, atts);
            generateHit(pager);
            contentHandler.endElement(NAMESPACE, HITS_ELEMENT, Q_HITS_ELEMENT);
        }
    }


    /**
     * Generate the xml content for each hit.
     *
     * @param  pager                 the LuceneCocoonPager with the search results.
     * @since
     * @throws  SAXException         when there is a problem creating the output SAX events.
     */
    private void generateHit(LuceneCocoonPager pager) throws SAXException {
        // get the off set to start from
        int counter = pager.getStartIndex();

        // get an list of hits which should be placed onto a single page
        List l = (List) pager.next();
        Iterator i = l.iterator();
        for (; i.hasNext(); counter++) {
            LuceneCocoonPager.HitWrapper hw = (LuceneCocoonPager.HitWrapper) i.next();
            Document doc = hw.getDocument();
            float score = hw.getScore();
            String uri = doc.get(LuceneXMLIndexer.URL_FIELD);

            atts.clear();
            atts.addAttribute("", RANK_ATTRIBUTE, RANK_ATTRIBUTE, CDATA,
                    String.valueOf(counter));
            atts.addAttribute("", SCORE_ATTRIBUTE, SCORE_ATTRIBUTE, CDATA,
                    String.valueOf(score));
            atts.addAttribute("", URI_ATTRIBUTE, URI_ATTRIBUTE, CDATA,
                    String.valueOf(uri));
            contentHandler.startElement(NAMESPACE, HIT_ELEMENT, Q_HIT_ELEMENT, atts);
            // fix me, add here a summary of this hit
            for (Enumeration e = doc.fields(); e.hasMoreElements(); ) {
                Field field = (Field)e.nextElement();
                if (field.isStored()) {
                    if (LuceneXMLIndexer.URL_FIELD.equals(field.name()))
                        continue;
                    atts.clear();
                    atts.addAttribute("", NAME_ATTRIBUTE, NAME_ATTRIBUTE, CDATA, field.name());
                    contentHandler.startElement(NAMESPACE, FIELD_ELEMENT, Q_FIELD_ELEMENT, atts);
                    String value = field.stringValue();
                    contentHandler.characters(value.toCharArray(), 0, value.length());
                    contentHandler.endElement(NAMESPACE, FIELD_ELEMENT, Q_FIELD_ELEMENT);
                }
            }

            contentHandler.endElement(NAMESPACE, HIT_ELEMENT, Q_HIT_ELEMENT);
        }
    }


    /**
     * Generate the navigation element.
     *
     * @param  pager                    Description of Parameter
     * @exception  SAXException         Description of Exception
     * @since
     */
    private void generateNavigation(LuceneCocoonPager pager) throws SAXException {

        if (pager != null) {
            // generate navigation element
            atts.clear();
            atts.addAttribute("", TOTAL_COUNT_ATTRIBUTE, TOTAL_COUNT_ATTRIBUTE,
                CDATA, String.valueOf(pager.getCountOfHits()));
            atts.addAttribute("", COUNT_OF_PAGES_ATTRIBUTE, COUNT_OF_PAGES_ATTRIBUTE,
                CDATA, String.valueOf(pager.getCountOfPages()));
            atts.addAttribute("", HAS_NEXT_ATTRIBUTE, HAS_NEXT_ATTRIBUTE,
                CDATA, String.valueOf(pager.hasNext()));
            atts.addAttribute("", HAS_PREVIOUS_ATTRIBUTE, HAS_PREVIOUS_ATTRIBUTE,
                CDATA, String.valueOf(pager.hasPrevious()));
            atts.addAttribute("", NEXT_INDEX_ATTRIBUTE, NEXT_INDEX_ATTRIBUTE,
                CDATA, String.valueOf(pager.nextIndex()));
            atts.addAttribute("", PREVIOUS_INDEX_ATTRIBUTE, PREVIOUS_INDEX_ATTRIBUTE,
                CDATA, String.valueOf(pager.previousIndex()));
            contentHandler.startElement(NAMESPACE, NAVIGATION_ELEMENT, Q_NAVIGATION_ELEMENT, atts);
            int count_of_pages = pager.getCountOfPages();
            for (int i = 0, page_start_index = 0;
                    i < count_of_pages;
                    i++, page_start_index += pageLength.intValue()) {
                atts.clear();
                atts.addAttribute("", START_INDEX_ATTRIBUTE, START_INDEX_ATTRIBUTE,
                    CDATA, String.valueOf(page_start_index));
                contentHandler.startElement(NAMESPACE, NAVIGATION_PAGE_ELEMENT, Q_NAVIGATION_PAGE_ELEMENT, atts);
                contentHandler.endElement(NAMESPACE, NAVIGATION_PAGE_ELEMENT, Q_NAVIGATION_PAGE_ELEMENT);
            }
            // navigation is EMPTY element
            contentHandler.endElement(NAMESPACE, NAVIGATION_ELEMENT, Q_NAVIGATION_ELEMENT);
        }
    }


    /**
     * Build hits from a query input, and setup paging object.
     *
     * @since
     * @throws  ProcessingException  if an error occurs
     */
    private LuceneCocoonPager buildHits() throws ProcessingException {

        if (queryString != null && queryString.length() != 0) {
            Hits hits = null;

            try {
                Analyzer analyzer = LuceneCocoonHelper.getAnalyzer("org.apache.lucene.analysis.standard.StandardAnalyzer");
                lcs.setAnalyzer(analyzer);
                // get the directory where the index resides
                Directory directory = LuceneCocoonHelper.getDirectory(index, false);
                lcs.setDirectory(directory);
                hits = lcs.search(queryString, LuceneXMLIndexer.BODY_FIELD);
            } catch (IOException ioe) {
                throw new ProcessingException("IOException in search", ioe);
            }

            // wrap the hits by an pager help object for accessing only a range of hits
            LuceneCocoonPager pager = new LuceneCocoonPager(hits);

            int start_index = START_INDEX_DEFAULT;
            if (this.startIndex != null) {
                start_index = this.startIndex.intValue();
                if (start_index <= 0) {
                    start_index = 0;
                }
                pager.setStartIndex(start_index);
            }

            int page_length = PAGE_LENGTH_DEFAULT;
            if (this.pageLength != null) {
                page_length = this.pageLength.intValue();
                if (page_length <= 0) {
                    page_length = hits.length();
                }
                pager.setCountOfHitsPerPage(page_length);
            }

            return pager;
        }

        return null;
    }

    /**
     * Recycle the generator
     */
    public void recycle() {
        super.recycle();
        if (lcs != null) {
            this.manager.release(lcs);
        }
        this.queryString = null;
        this.startIndex = null;
        this.pageLength = null;
        this.index = null;
    }

    public void dispose() {
        super.dispose();
    }
}

