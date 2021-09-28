/* 
*    Logical Structural Diff (LSDiff)  
*    Copyright (C) <2015>  <Dr. Miryung Kim miryung@cs.ucla.edu>
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package lsclipse.views;

import java.util.ArrayList;
import java.util.HashMap;

import lsclipse.LSDResult;
import lsclipse.LSDiffRunner;
import lsclipse.dialogs.ProgressBarDialog;
import lsclipse.dialogs.SelectProjectDialog;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ResourceNode;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;

import translator.Rule;


public class RulesView extends ViewPart {
	Action selectAction, explainAction, englishAction, filterAction, sortAction, navigateAction, compareAction;
    TabFolder tabFolder;
    GridData layoutData1;
    GridData layoutHidden;
    GridData layoutEnglish;
    Composite parent;
    Table rulesTable;
	TabItem tabItemExamples;
	TabItem tabItemExceptions;
	List examplesList;
	List exceptionsList;
	Label txtEnglish;

	private IProject svnproj = null;
	
	java.util.List<LSDResult> rules = new ArrayList<LSDResult>();
	IProject baseproj = null;
	IProject newproj = null;

	public void createPartControl(Composite parent) {
		this.parent = parent;

		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
	    parent.setLayout(layout);

	    //declare showing layout
		layoutData1 = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		layoutData1.grabExcessHorizontalSpace = true;
		layoutData1.grabExcessVerticalSpace = true;
		layoutData1.horizontalAlignment = GridData.FILL;
		layoutData1.verticalAlignment = GridData.FILL;
		layoutData1.exclude = false;

	    //declare 'hidden' layout
		layoutHidden = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		layoutHidden.grabExcessHorizontalSpace = true;
		layoutHidden.grabExcessVerticalSpace = true;
		layoutHidden.horizontalAlignment = GridData.FILL;
		layoutHidden.verticalAlignment = GridData.FILL;
		layoutHidden.exclude = true;

	    //English text layout
		layoutEnglish = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		layoutEnglish.grabExcessHorizontalSpace = true;
		layoutEnglish.horizontalAlignment = GridData.FILL;
		layoutEnglish.horizontalSpan = 2;
		layoutEnglish.exclude = false;

		//make rules table
	    rulesTable = new Table(parent, SWT.SINGLE | SWT.FULL_SELECTION);
		TableColumn col1 = new TableColumn(rulesTable, SWT.NULL);
		rulesTable.setHeaderVisible(true);
		col1.setText("Accuracy");
		col1.pack();
		TableColumn col2 = new TableColumn(rulesTable, SWT.NULL);
		col2.setText("Rule");
		col2.setWidth(430);
		rulesTable.setLayoutData(layoutData1);
		rulesTable.addListener(SWT.Selection, new Listener() {
		      public void handleEvent(Event e) {
		          refreshExamples();
		        }
			});

		//make tabfolder and example lists
		tabFolder = new TabFolder (parent, 0);
		tabItemExamples = new TabItem (tabFolder, SWT.NONE);
		tabItemExamples.setText ("Matches");
		examplesList = new List(tabFolder, SWT.SINGLE | SWT.H_SCROLL);
		examplesList.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent arg0) {
				navigateAction.run();
			}
			public void mouseDown(MouseEvent arg0) { }
			public void mouseUp(MouseEvent arg0) { }
		});
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				if (examplesList.getSelectionCount()<1) return;
				manager.add(navigateAction);
				manager.add(compareAction);
			}
		});
		Menu menu = menuMgr.createContextMenu(examplesList);
		examplesList.setMenu(menu);

		tabItemExamples.setControl(examplesList);
		tabItemExceptions = new TabItem (tabFolder, SWT.NONE);
		tabItemExceptions.setText ("Exceptions");
		exceptionsList = new List(tabFolder, SWT.SINGLE | SWT.H_SCROLL);
		tabItemExceptions.setControl(exceptionsList);
		tabFolder.setLayoutData(layoutHidden);

		//make tabfolder and example lists
		txtEnglish = new Label(parent, SWT.SINGLE);
		txtEnglish.setLayoutData(layoutHidden);
//		txtEnglish.set.setEditable(false);

		parent.layout();

		createActions();
        createMenu();
        createToolbar();
	}
    public void createActions() {
    	//Select Action
		selectAction = new Action("Select version...") {
			public void run() {
				final ProgressBarDialog pbdiag = new ProgressBarDialog(parent.getShell());
/*
				String r1 = LSDiffRunner.batchLSDiffIncFE(pbdiag);
				String r2 = LSDiffRunner.batchLSDiffGranularity(pbdiag);
				String r3 = LSDiffRunner.batchLSDiffNHops(pbdiag);

				System.out.println("*** Final perofmrance ***");
				System.out.println("--Incremental--");
				System.out.println(r1);
				System.out.println("--Granularity--");
				System.out.println(r2);
				System.out.println("--nHops--");
				System.out.println(r3);
/*/
				//collect information from seldiag
				final SelectProjectDialog seldiag = new SelectProjectDialog(parent.getShell());
		    	final int returncode = seldiag.open();
		    	if (returncode>0) return;

		    	//remember base project (and new project)
				baseproj = ResourcesPlugin.getWorkspace().getRoot().getProject(seldiag.getProj1());
				newproj = ResourcesPlugin.getWorkspace().getRoot().getProject(seldiag.getProj2());
				svnproj = seldiag.getSVNProject();

				new Thread() {
					public void run() {
				    	//open new log box
						pbdiag.open();
						pbdiag.setStep(0);

						//do lsdiff
						rules = LSDiffRunner.doLSDiff(seldiag.getProj1(), seldiag.getProj2(), seldiag.getChangedFiles(), seldiag.getAccuracy(), seldiag.getMinMatches(), seldiag.getAntecedent(), seldiag.getBeamSize(), pbdiag);

						//display results on view
				    	parent.getDisplay().asyncExec(new Runnable() {
							public void run() {
								refreshRules();
								refreshExamples();
							}
						});
					}
				}.start();
//*/
			}
		};
		selectAction.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages().getImageDescriptor(
						ISharedImages.IMG_OBJ_FOLDER));

		//Explain Action
		explainAction = new Action("Explain") {
			public void run() {
				if (tabFolder.getLayoutData().equals(layoutHidden)) {
					showRulesList();
				} else {
					hideRulesList();
				}
			}
		};
		explainAction.setImageDescriptor(lsclipse.LSclipse.getImageDescriptor("icons/explain.gif"));

		//English Action
		englishAction = new Action("Translate to English") {
			public void run() {
				if (txtEnglish.getLayoutData().equals(layoutHidden)) {
					showEnglish();
				} else {
					hideEnglish();
				}
			}
		};
		englishAction.setImageDescriptor(lsclipse.LSclipse.getImageDescriptor("icons/english.gif"));

		//*	TODO: Currently do not have sort and filter functions
		//Sort Action
		sortAction = new Action("Sort") {
			public void run() {
			}
		};
		sortAction.setImageDescriptor(lsclipse.LSclipse.getImageDescriptor("icons/sort.gif"));

		//Filter Action
		filterAction = new Action("Filter") {
			public void run() {
			}
		};
		filterAction.setImageDescriptor(lsclipse.LSclipse.getImageDescriptor("icons/filter.gif"));
		//*/

		//Navigate Action
		navigateAction = new Action() {
			public void run() {
				//IMPT!!! double click listener called AFTER the new line is selected
				//check if there is a selection
				int rulesel = rulesTable.getSelectionIndex();
				if (rulesel<0) return;
				int factsel = examplesList.getSelectionIndex();
				if (factsel<0) return;

				LSDResult lsdres = rules.get(rulesel);
				LSDResult.Bookmark bookmark = lsdres.examplesBookmarks.get(factsel);
				IProject proj = newproj;
				if (bookmark.oldFB) proj=baseproj;
				openInEditor(proj.getFile(bookmark.filename), bookmark.startpos, bookmark.length);
			}
		};
		navigateAction.setText("Open in editor");

		//Compare Action
		compareAction = new Action() {
			class CompareInput extends CompareEditorInput {
				IFile input1;
				IFile input2;

				public CompareInput(IFile input1, IFile input2) {
					super(new CompareConfiguration());
					this.input1 = input1;
					this.input2 = input2;
				}

				protected Object prepareInput(
						org.eclipse.core.runtime.IProgressMonitor pm) {
					return new DiffNode(new ResourceNode(input1),
							new ResourceNode(input2));
				}
			}
			public void run() {
				//IMPT!!! double click listener called AFTER the new line is selected
				//check if there is a selection
				int rulesel = rulesTable.getSelectionIndex();
				if (rulesel<0) return;
				int factsel = examplesList.getSelectionIndex();
				if (factsel<0) return;

				LSDResult lsdres = rules.get(rulesel);
				LSDResult.Bookmark bookmark = lsdres.examplesBookmarks.get(factsel);

				IFile input1 = baseproj.getFile(bookmark.filename);
				IFile input2 = newproj.getFile(bookmark.filename);
				
		  		CompareUI.openCompareEditor(new CompareInput(input1, input2));
			}
		};
		compareAction.setText("Compare with base");
   }
	private void createToolbar() {
        IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();
        mgr.add(selectAction);
        mgr.add(explainAction);
        mgr.add(englishAction);
//		mgr.add(sortAction);
//		mgr.add(filterAction);
	}
	private void createMenu() {
//        IMenuManager mgr = getViewSite().getActionBars().getMenuManager();
//        mgr.add(selectAllAction);
	}

	public void setFocus() {
	}

	private void showRulesList() {
		tabFolder.setLayoutData(layoutData1);
		tabFolder.layout();
		parent.layout();
	}
    private void hideRulesList() {
		tabFolder.setLayoutData(layoutHidden);
		tabFolder.layout();
		parent.layout();
    }
	private void showEnglish() {
		txtEnglish.setLayoutData(layoutEnglish);
		parent.layout();
	}
    private void hideEnglish() {
    	txtEnglish.setLayoutData(layoutHidden);
		parent.layout();
    }

    private void refreshRules() {
    	rulesTable.removeAll();
    	for (int i=0; i<rules.size(); ++i) {
    		LSDResult rule = rules.get(i);
    		TableItem ti = new TableItem(rulesTable, SWT.NULL);
    		String acc = rule.num_matches+"/"+(rule.num_matches+rule.num_counter);
    		String desc = rule.desc;
    		ti.setText(new String[] { acc, desc });
    	}
    	rulesTable.getColumn(1).pack();
    	rulesTable.layout();
    }

    private void refreshExamples() {
    	int index = rulesTable.getSelectionIndex();
    	refreshExamples(index);
        refreshEnglish(index);
    }

    private void refreshExamples(int index) {

    	if (index<0 || index>=rules.size()) return; //array out of bounds

    	LSDResult rule = rules.get(index);

    	//refresh examples
    	tabItemExamples.setText("Matches ("+(rule.examples.size())+")");
    	examplesList.removeAll();
    	for (String s : rule.getExampleStr())
    		examplesList.add(s);

    	//refresh exceptions
    	tabItemExceptions.setText("Exceptions ("+(rule.exceptions.size())+")");
    	exceptionsList.removeAll();
    	for (String s : rule.getExceptionsString()) 
    		exceptionsList.add(s);
    }

    private void refreshEnglish(int index) {

    	if (index<0 || index>=rules.size()) return; //array out of bounds

    	LSDResult rule = rules.get(index);
		Rule englishRule = new Rule(rule.rule, rule.num_matches, (rule.num_matches+rule.num_counter));
		txtEnglish.setText("\""+englishRule.translateRule()+"\"");
    }

	@SuppressWarnings("unchecked")
	private static void openInEditor(IFile file, int startpos, int length) {

		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		HashMap map = new HashMap();
		map.put(IMarker.CHAR_START, new Integer(startpos));
		map.put(IMarker.CHAR_END, new Integer(startpos+length));
		try {
			IMarker marker = file.createMarker(IMarker.TEXT);
			marker.setAttributes(map);

			IDE.openEditor(page, marker);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

	@Override
	public void dispose() {
		//delete svn temp proj if necessary
		if (svnproj!=null) {
			try {
				svnproj.delete(true, new NullProgressMonitor());
				IWorkspaceRoot ws = ResourcesPlugin.getWorkspace().getRoot();
				ws.refreshLocal(IWorkspaceRoot.DEPTH_INFINITE, new NullProgressMonitor());
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
}
