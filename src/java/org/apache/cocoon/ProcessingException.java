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
package org.apache.cocoon;

import java.util.List;

import org.apache.cocoon.util.location.LocatedException;
import org.apache.cocoon.util.location.LocatedRuntimeException;
import org.apache.cocoon.util.location.Location;
import org.apache.cocoon.util.location.MultiLocatable;

/**
 * This Exception is thrown every time there is a problem in processing
 * a request.
 *
 * @author <a href="mailto:pier@apache.org">Pierpaolo Fumagalli</a>
 *         (Apache Software Foundation)
 * @version CVS $Id$
 */
public class ProcessingException extends LocatedException implements MultiLocatable {
    
    /**
     * Construct a new <code>ProcessingException</code> instance.
     */
    public ProcessingException(String message) {
        super(message);
    }
    
    /**
     * Creates a new <code>ProcessingException</code> instance.
     *
     * @param ex an <code>Exception</code> value
     */
    public ProcessingException(Exception ex) {
        super(ex.getMessage(), ex);
    }
    
    /**
     * Construct a new <code>ProcessingException</code> that references
     * a parent Exception.
     */
    public ProcessingException(String message, Throwable t) {
        super(message, t);
    }
    
    /**
     * Construct a new <code>ProcessingException</code> that has an associated location.
     */
    public ProcessingException(String message, Location location) {
        super(message, location);
    }
    
    /**
     * Construct a new <code>ProcessingException</code> that has a parent exception
     * and an associated location.
     * <p>
     * This constructor is protected to enforce the use of {@link #throwLocated(String, Throwable, Location)}
     * which limits exception nesting as far as possible.
     */
    protected ProcessingException(String message, Throwable t, Location location) {
        super(message, t, location);
    }
    
    /**
     * Throw a located exception given an existing exception and the location where
     * this exception was catched.
     * <p>
     * If the exception is already a <code>ProcessingException</code> or a {@link LocatedRuntimeException},
     * the location is added to the original exception's location chain and the original exception
     * is rethrown (<code>description</code> is ignored) to limit exception nesting. Otherwise, a new
     * <code>ProcessingException</code> is thrown, wrapping the original exception.
     * <p>
     * Note: this method returns an exception as a convenience if you want to keep the <code>throw</code>
     * semantics in the caller code, i.e. write<br>
     * <code>&nbsp;&nbsp;throw ProcessingException.throwLocated(...);</code><br>
     * instead of<br>
     * <code>&nbsp;&nbsp;ProcessingException.throwLocated(...);</code><br>
     * <code>&nbsp;&nbsp;return;</code>
     * 
     * @param message a message (can be <code>null</code>)
     * @param thr the original exception (can be <code>null</code>)
     * @param location the location (can be <code>null</code>)
     * @return a (fake) located exception
     * @throws ProcessingException or <code>LocatedRuntimeException</code>
     */
    public static ProcessingException throwLocated(String message, Throwable thr, Location location) throws ProcessingException {
        if (thr instanceof ProcessingException) {
            ProcessingException pe = (ProcessingException)thr;
            pe.addLocation(location);
            throw pe;

        } else if (thr instanceof LocatedRuntimeException) {
            LocatedRuntimeException re = (LocatedRuntimeException)thr;
            re.addLocation(location);
            // Rethrow
            throw re;
        }
        
        throw new ProcessingException(message, thr, location);
    }
    
    /**
     * Throw a located exception given an existing exception and the locations where
     * this exception was catched.
     * <p>
     * If the exception is already a <code>ProcessingException</code> or a {@link LocatedRuntimeException},
     * the locations are added to the original exception's location chain and the original exception
     * is rethrown (<code>description</code> is ignored) to limit exception nesting. Otherwise, a new
     * <code>ProcessingException</code> is thrown, wrapping the original exception.
     * <p>
     * Note: this method returns an exception as a convenience if you want to keep the <code>throw</code>
     * semantics in the caller code, i.e. write<br>
     * <code>&nbsp;&nbsp;throw ProcessingException.throwLocated(...);</code><br>
     * instead of<br>
     * <code>&nbsp;&nbsp;ProcessingException.throwLocated(...);</code><br>
     * <code>&nbsp;&nbsp;return;</code>
     * 
     * @param message a message (can be <code>null</code>)
     * @param thr the original exception (can be <code>null</code>)
     * @param locations the locations (can be <code>null</code>)
     * @return a (fake) located exception
     * @throws ProcessingException or <code>LocatedRuntimeException</code>
     */
    public static ProcessingException throwLocated(String message, Throwable thr, List locations) throws ProcessingException {
        MultiLocatable multiloc;
        if (thr instanceof ProcessingException) {
            multiloc = (ProcessingException)thr;
        } else if (thr instanceof LocatedRuntimeException) {
            multiloc = (LocatedRuntimeException)thr;
        } else {
            multiloc = new ProcessingException(message, thr);
        }
        
        if (locations != null) {
            for (int i = 0; i < locations.size(); i++) {
                multiloc.addLocation((Location)locations.get(i));
            }
        }
        
        if (multiloc instanceof LocatedRuntimeException) {
            throw (LocatedRuntimeException)multiloc;
        } else {
            throw (ProcessingException)multiloc;
        }
    }


//    public String toString() {
//        StringBuffer s = new StringBuffer();
//        s.append(super.toString());
//        final Throwable t = getCause();
//        if(t!=null) {
//            s.append(": ");
//            // be more verbose try to get location info
//            s.append( extraInfo(t) );
//            s.append(t.toString());
//        }
//        return s.toString();
//    }
//    
//    /**
//     * Examine Throwable and try to figure out location information.
//     * <p>
//     *   At the moment only SAXParseException, and TransformerException
//     *   are considered.
//     * </p>
//     *
//     * @return String containing location information of the format
//     *  <code>{file-name}:{line}:{column}:</code>, if no location info is 
//     *  available return empty string
//     */
//    private String extraInfo( Throwable t ) {
//        StringBuffer sb = new StringBuffer();
//        if (t instanceof SAXParseException) {
//            SAXParseException spe = (SAXParseException)t;
//            sb.append( String.valueOf(spe.getSystemId()));
//            sb.append( ":" );
//            sb.append( String.valueOf(spe.getLineNumber()));
//            sb.append( ":" );
//            sb.append( String.valueOf(spe.getColumnNumber()));
//            sb.append( ":" );
//        } else if (t instanceof TransformerException) {
//            TransformerException transformerException = (TransformerException) t;
//            SourceLocator sourceLocator = transformerException.getLocator();
//            
//            if( null != sourceLocator ) {
//                sb.append( String.valueOf(sourceLocator.getSystemId()));
//                sb.append( ":" );
//                sb.append( String.valueOf(sourceLocator.getLineNumber()));
//                sb.append( ":" );
//                sb.append( String.valueOf(sourceLocator.getColumnNumber()));
//                sb.append( ":" );
//            }
//        }
//        return sb.toString();
//    }
//    
//    public void printStackTrace() {
//        super.printStackTrace();
//        if(getCause()!=null)
//            getCause().printStackTrace();
//    }
//    
//    public void printStackTrace( PrintStream s ) {
//        super.printStackTrace(s);
//        if(getCause()!=null)
//            getCause().printStackTrace(s);
//    }
//    
//    public void printStackTrace( PrintWriter s ) {
//        super.printStackTrace(s);
//        if(getCause()!=null)
//            getCause().printStackTrace(s);
//    }
}
