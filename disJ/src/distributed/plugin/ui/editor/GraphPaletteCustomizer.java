/*******************************************************************************
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     DisJ Development Group
 *******************************************************************************/

package distributed.plugin.ui.editor;

/**
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.ui.palette.PaletteCustomizer;
import org.eclipse.gef.ui.palette.customize.DefaultEntryPage;
import org.eclipse.gef.ui.palette.customize.DrawerEntryPage;
import org.eclipse.gef.ui.palette.customize.EntryPage;

//import org.eclipse.gef.examples.logicdesigner.LogicMessages;

/**
 * PaletteCustomizer for the logic example.
 * 
 */
public class GraphPaletteCustomizer 
	extends PaletteCustomizer 
{
	
protected static final String ERROR_MESSAGE 
									= "You messed up";
	
/**
 * @see org.eclipse.gef.ui.palette.PaletteCustomizer#getPropertiesPage(PaletteEntry)
 */
public EntryPage getPropertiesPage(PaletteEntry entry) {
	if (entry.getType().equals(PaletteDrawer.PALETTE_TYPE_DRAWER)) {
		return new LogicDrawerEntryPage();
	}
	return new LogicEntryPage();
}

/**
 * @see org.eclipse.gef.ui.palette.PaletteCustomizer#revertToSaved()
 */
public void revertToSaved() {
}


/**
 * @see org.eclipse.gef.ui.palette.PaletteCustomizer#dialogClosed(PaletteEntry)
 */
public void save() {
}

private class LogicEntryPage extends DefaultEntryPage {
	protected void handleNameChanged(String text) {
		if (text.indexOf('*') >= 0) {
			getPageContainer().showProblem(ERROR_MESSAGE);
		} else {
			super.handleNameChanged(text);
			getPageContainer().clearProblem();
		}
	}
}

private class LogicDrawerEntryPage extends DrawerEntryPage {
	protected void handleNameChanged(String text) {
		if (text.indexOf('*') >= 0) {
			getPageContainer().showProblem(ERROR_MESSAGE);
		} else {
			super.handleNameChanged(text);
			getPageContainer().clearProblem();
		}
	}
}

}

