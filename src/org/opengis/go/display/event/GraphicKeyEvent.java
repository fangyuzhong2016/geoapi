/**************************************************************************************************
 **
 ** $Id$
 **
 ** $URL$
 **
 ** Copyright (C) 2003 Open GIS Consortium, Inc. All Rights Reserved. http://www.opengis.org/Legal/
 **
 *************************************************************************************************/
package org.opengis.go.display.event;

// J2SE direct dependencies
import java.awt.event.KeyEvent;

/**
 * Provides a common abstraction for the various event objects pertaining to key
 * events on a {@link org.opengis.go.display.primitive.Graphic}.
 * 
 * @version $Revision: 659 $, $Date: 2006-02-23 14:07:07 +1100 (jeu., 23 févr. 2006) $
 * @author <A HREF="http://www.opengis.org">OpenGIS&reg; consortium</A>
 */
public interface GraphicKeyEvent extends GraphicEvent {

    /**
     * Returns the <code>KeyEvent</code> that this event references.
     *
     * @return the <code>KeyEvent</code> referenced by this event.
     *
     * @revisit This {@link KeyEvent} implementation is specific to AWT and Swing
     *          (i.e. is not used by SWT as far as I know).
     */
    public KeyEvent getKeyEvent();
}
