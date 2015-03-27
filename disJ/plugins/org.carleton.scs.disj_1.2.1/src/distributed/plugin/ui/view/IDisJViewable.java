package distributed.plugin.ui.view;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

/**
 * Marker-style interface for a content outline page. This interface defines
 * the minimum requirement for pages within the content disJ view, namely
 * they must be pages (implement <code>IPage</code>) and provide selections
 * (implement <code>ISelectionProvider</code>).
 * <p>
 * Access to a content disJ page begins when an editor is activated. When
 * activation occurs, the content disJ view will ask the editor for its
 * content disJ page. This is done by invoking 
 * <code>getAdapter(IContentOutlinePage.class)</code> on the editor.  
 * If the editor returns a page, the view then creates the controls for that
 * page (using <code>createControl</code>) and makes the page visible.
 * </p>
 * <p>
 * Clients may implement this interface from scratch, or subclass the
 * abstract base class <code>ContentOutlinePage</code>.
 * </p>
 * <p> 
 * Note that this interface extends <code>ISelectionProvider</code>.
 * This is no longer required in the case of implementors who also 
 * implement <code>IPageBookViewPage</code> (or extend <code>Page</code>)
 * as they are now passed an <code>IPageSite</code> during their initialization 
 * and this site can be configured with a selection provider. 
 * However to avoid a breaking change 
 *  1) this interface will continue to extend ISelectionProvider 
 *  2) if an IContentOutlinePage does not set a selection provider for its 
 * site, the ContentOutline will continue to use the page itself for 
 * this purpose. 
 * </p> 
 *
 * @see ContentOutlinePage
 */
public interface IDisJViewable extends IPage, ISelectionProvider {

}
