package distributed.plugin.ui.view;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.Page;

/**
 * An abstract base class for DisJ viewable pages.
 * <p>
 * Clients who are defining an editor may elect to provide a corresponding
 * DisJ view page. This disJ view page will be presented to the
 * user via the standard disJ View (the user decides whether their
 * workbench window contains this view) whenever that editor is active.
 * This class should be subclassed.
 * </p>
 * <p>
 * Internally, each disJ view page consists of a standard tree viewer; 
 * selections made in the tree viewer are reported as selection change events 
 * by the page (which is a selection provider). The tree viewer is not created 
 * until <code>createPage</code> is called; consequently, subclasses must extend
 * <code>createControl</code> to configure the tree viewer with a proper content 
 * provider, label provider, and input element.
 * </p>
 * <p>
 * Note that those wanting to use a control other than internally created
 * <code>TreeViewer</code> will need to implement 
 * <code>IDisJviewable</code> directly rather than subclassing this class.
 * </p> 
 */
public abstract class DisJViewPage extends Page implements IDisJViewable,
		ISelectionChangedListener {
    private ListenerList selectionChangedListeners = new ListenerList();

    private TreeViewer treeViewer;

    /**
     * Create a new viewer page.
     */
    protected DisJViewPage() {
        super();
    }

    /* (non-Javadoc)
     * Method declared on ISelectionProvider.
     */
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        selectionChangedListeners.add(listener);
    }

    /**
     * The <code>DisJViewPage</code> implementation of this 
     * <code>IDisJViewable</code> method creates a tree viewer. Subclasses
     * must extend this method configure the tree viewer with a proper content 
     * provider, label provider, and input element.
     * @param parent
     */
    public void createControl(Composite parent) {
        treeViewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL
                | SWT.V_SCROLL);
        treeViewer.addSelectionChangedListener(this);
    }

    /**
     * Fires a selection changed event.
     *
     * @param selection the new selection
     */
    protected void fireSelectionChanged(ISelection selection) {
        // create an event
        final SelectionChangedEvent event = new SelectionChangedEvent(this,
                selection);

        // fire the event
        Object[] listeners = selectionChangedListeners.getListeners();
        for (int i = 0; i < listeners.length; ++i) {
            final ISelectionChangedListener l = (ISelectionChangedListener) listeners[i];
            SafeRunner.run(new SafeRunnable() {
                public void run() {
                    l.selectionChanged(event);
                }
            });
        }
    }

    /* (non-Javadoc)
     * Method declared on IPage (and Page).
     */
    public Control getControl() {
        if (treeViewer == null) {
			return null;
		}
        return treeViewer.getControl();
    }

    /* (non-Javadoc)
     * Method declared on ISelectionProvider.
     */
    public ISelection getSelection() {
        if (treeViewer == null) {
			return StructuredSelection.EMPTY;
		}
        return treeViewer.getSelection();
    }

    /**
     * Returns this page's tree viewer.
     *
     * @return this page's tree viewer, or <code>null</code> if 
     *   <code>createControl</code> has not been called yet
     */
    protected TreeViewer getTreeViewer() {
        return treeViewer;
    }

    /*
     *  (non-Javadoc)
     * @see org.eclipse.ui.part.IPageBookViewPage#init(org.eclipse.ui.part.IPageSite)
     */
    public void init(IPageSite pageSite) {
        super.init(pageSite);
        pageSite.setSelectionProvider(this);
    }

    /* (non-Javadoc)
     * Method declared on ISelectionProvider.
     */
    public void removeSelectionChangedListener(
            ISelectionChangedListener listener) {
        selectionChangedListeners.remove(listener);
    }

    /* (non-Javadoc)
     * Method declared on ISelectionChangeListener.
     * Gives notification that the tree selection has changed.
     */
    public void selectionChanged(SelectionChangedEvent event) {
        fireSelectionChanged(event.getSelection());
    }

    /**
     * Sets focus to a part in the page.
     */
    public void setFocus() {
        treeViewer.getControl().setFocus();
    }

    /* (non-Javadoc)
     * Method declared on ISelectionProvider.
     */
    public void setSelection(ISelection selection) {
        if (treeViewer != null) {
			treeViewer.setSelection(selection);
		}
    }
}

