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
package lsclipse.dialogs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import metapackage.MetaInfo;

import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import utils.IO;
import utils.WS;

public class SelectProjectDialog extends Dialog {

	private Shell shell;

	private Set<String> changedfiles = new HashSet<String>();
	private String proj1 = "";
	private String proj2 = "";
	
	private Combo cmbProj1;
	private Combo cmbProj2;
	private Table tblMyChangedFiles;

	private IProject svnproj = null;
	private boolean isSVN = false;
	private String svnurl = "";
	private String svnname = "";
	private String svnpasswd = "";
	private int svnrev1 = 0;
	private int svnrev2 = 0;

	private Text txtMinAccuracy;
	private Text txtMinMatches;
	private Text txtAntecedent;
	private Text txtBeamSize;
	private Combo cmbGranularity;
	private Combo cmbHops;
	double acc;
	int minConcFact;
	int k;
	int beamSize;
	int granul;
	int nhops;
	
	public SelectProjectDialog(Shell parentShell) {
		super(parentShell);
		shell = parentShell;
	}
	public String getProj1() {
		return proj1;
	}
	public String getProj2() {
		return proj2;
	}
	public IProject getSVNProject() {
		return svnproj;
	}
	public Set<String> getChangedFiles() {
		return changedfiles;
	}
	public double getAccuracy() {
		return acc;
	}
	public int getMinMatches() {
		return minConcFact;
	}
	public int getAntecedent() {
		return k;
	}
	public int getBeamSize() {
		return beamSize;
	}
	public int getGranularity() {
		return nhops;
	}
	public boolean isSVN() {
		return isSVN;
	}
	public void okPressed() {
//		IPath pt = lsclipse.Activator.getDefault().getStateLocation();
//		IPath pt = ResourcesPlugin.getPlugin().getStateLocation();
//		System.out.println(pt.toOSString());

		changedfiles.clear();
		for (TableItem ti : tblMyChangedFiles.getItems()) {
			if (ti.getChecked()) {
				changedfiles.add(ti.getText(0));
				System.out.println("added file: "+ti.getText(0));
			}
		}
		if (changedfiles.size()==0) {
			MessageDialog.openError(shell.getShell(), "File selection error", "No files selected!");
			return;
		}
		if (!proj1.equals(cmbProj1.getText())) {
			MessageDialog.openError(shell.getShell(), "Base project error", "Base project selection has changed, please get files again");
			return;
		}
		if (!proj1.equals(cmbProj1.getText())) {
			MessageDialog.openError(shell.getShell(), "Base project error", "Base project selection has changed, please get files again");
			return;
		}
		try {
			acc = Double.parseDouble(txtMinAccuracy.getText());
			if (acc<=0 || acc>1) {
				MessageDialog.openError(shell.getShell(), "Optional parameters error", "Accuracy should be between 0 and 1");
				return;
			}
		} catch (NumberFormatException e) {
			MessageDialog.openError(shell.getShell(), "Optional parameters error", "Unrecognizable accuracy value");
			return;
		}
		try {
			minConcFact = Integer.parseInt(txtMinMatches.getText());
			if (minConcFact<=0) {
				MessageDialog.openError(shell.getShell(), "Optional parameters error", "Minimum matches should be greater than 0");
				return;
			}
		} catch (NumberFormatException e) {
			MessageDialog.openError(shell.getShell(), "Optional parameters error", "Unrecognizable minimum match value");
			return;
		}
		try {
			k = Integer.parseInt(txtAntecedent.getText());
			if (k<=0 || k>3) {
				MessageDialog.openError(shell.getShell(), "Optional parameters error", "Antecedent length should be between 1 and 3");
				return;
			}
		} catch (NumberFormatException e) {
			MessageDialog.openError(shell.getShell(), "Optional parameters error", "Unrecognizable antecedent length");
			return;
		}
		try {
			beamSize = Integer.parseInt(txtBeamSize.getText());
		} catch (NumberFormatException e) {
			MessageDialog.openError(shell.getShell(), "Optional parameters error", "Unrecognizable beam size");
			return;
		}
		granul = cmbGranularity.getSelectionIndex();
		nhops = cmbHops.getSelectionIndex()+1;
		
		//retrieve files from SVN
		if (isSVN) {
			//make a hidden clone of the base project
			String workname = chooseTempProjName(proj1);
			IWorkspaceRoot ws = ResourcesPlugin.getWorkspace().getRoot();
			IProject oldworkproj = ws.getProject(workname);
			if (oldworkproj.exists()) {
				try {
					oldworkproj.delete(true, true, new NullProgressMonitor());
					ws.refreshLocal(IWorkspaceRoot.DEPTH_ONE, new NullProgressMonitor());
				} catch (CoreException e) { }
			}
			IProject project1 = ws.getProject(proj1);
			IPath projpath = new Path(workname);
			try {
				project1.copy(projpath, true, new NullProgressMonitor()); 
			} catch (CoreException e) {
				return;
			}
			svnproj = ResourcesPlugin.getWorkspace().getRoot().getProject(workname);
			try {
				svnproj.setHidden(false);
			} catch (CoreException e1) { }

			//replace missing files
			for (String filename : changedfiles) {
				try {
					String f = loadFilesFromSVN(filename);
					//write string to file
					writeChangedFile(filename, f, svnproj);
				} catch (Exception e) {
				}
			}

			try {
				svnproj.refreshLocal(IProject.DEPTH_INFINITE, new NullProgressMonitor());
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			proj2 = workname;
		} else {
			if (!proj2.equals(cmbProj2.getText())) {
				MessageDialog.openError(shell.getShell(), "Changed project error", "Base project selection has changed, please get files again");
				return;
			}
		}

		super.okPressed();
	}
	private static void writeChangedFile(String path, String contents, IProject destProj) {
		IFile destFile = destProj.getFile(path);
		File dest = new File(destFile.getLocation().toOSString());
		try {
			//If file exists in new proj, copy it. If does not exist then overwrite
			if (contents.isEmpty()) {	//file was deleted
				dest.delete();
			} else {
				IO.writeToFile(contents, dest.getAbsolutePath());
			}
			destFile.refreshLocal(1, new NullProgressMonitor());
		} catch (CoreException e) {
		}
	}
	private static String chooseTempProjName(String baseprojname) {
		return "LSdiff-temp-project";
	}
	protected Control createDialogArea(Composite parent) {
		this.getShell().setText("Select Files");

		//Big picture: left side showing projects, right side shows files

		//~~~~~~~~Left panel~~~~~~~~
		//- Two project selection dropdowns
		//  - Tab control for how to get files
		//    - Manual
		//    - SVN log
		//    - Diff

		//~~~~~~~~Right panel~~~~~~~~
		//- Big group labeled "Changed Files" 
		//  - List of files to be parsed (each with a check box)

		//overall layout
    	GridLayout layout = new GridLayout();
		layout.numColumns = 2;
	    parent.setLayout(layout);

	    //declare some layouts
	    GridData ldtDefault = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		ldtDefault.grabExcessHorizontalSpace = true;
		ldtDefault.grabExcessVerticalSpace = true;
		ldtDefault.horizontalAlignment = GridData.FILL;
		ldtDefault.verticalAlignment = GridData.CENTER;
		ldtDefault.exclude = false;

	    GridData ldtTop = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
	    ldtTop.grabExcessHorizontalSpace = true;
	    ldtTop.grabExcessVerticalSpace = false;
	    ldtTop.horizontalAlignment = GridData.FILL;
	    ldtTop.verticalAlignment = GridData.CENTER;
	    ldtTop.exclude = false;

	    GridData ldtGrid = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
	    ldtGrid.grabExcessHorizontalSpace = false;
	    ldtGrid.grabExcessVerticalSpace = false;
	    ldtGrid.horizontalAlignment = GridData.BEGINNING;
	    ldtGrid.verticalAlignment = GridData.CENTER;
	    ldtGrid.exclude = false;

	    GridData ldtTwoCol = new GridData();
		ldtTwoCol.grabExcessHorizontalSpace = true;
		ldtTwoCol.grabExcessVerticalSpace = true;
		ldtTwoCol.horizontalAlignment = GridData.FILL;
		ldtTwoCol.verticalAlignment = GridData.BEGINNING;
		ldtTwoCol.exclude = false;
		ldtTwoCol.horizontalSpan = 2;

		//~~~~~~~~Left panel~~~~~~~~
		//- Two project selection dropdowns
		//  - Tab control for how to get files
		//    - Manual
		//    - SVN log
		//    - Diff
		Composite leftPanel = new Composite(parent, 0);
		leftPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL ));
		GridLayout leftLayout = new GridLayout();
		leftLayout.numColumns = 1;
		leftPanel.setLayout(leftLayout);

		//Project1 dropdown
		Label lblProj1 = new Label(leftPanel, 0);
		lblProj1.setText("Base project:");
		cmbProj1 = new Combo(leftPanel, 0);
		cmbProj1.setVisibleItemCount(20);
		cmbProj1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING | GridData.GRAB_HORIZONTAL));

		//Populate the combo boxes
		for (IProject proj : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			cmbProj1.add(proj.getName());
		}

		//Tab control for how to get files
		//Tab control
		TabFolder tabGetChanged = new TabFolder(leftPanel, 0);
		GridData ldtTabFolder = new GridData(GridData.VERTICAL_ALIGN_END | GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL );
		ldtTabFolder.minimumWidth = 140;
		tabGetChanged.setLayoutData(ldtTabFolder);
		TabItem tbiDiff = new TabItem(tabGetChanged, 0);
		tbiDiff.setText("Workspace");
		TabItem tbiSVNLog = new TabItem(tabGetChanged, 0);
		tbiSVNLog.setText("SVN");
		TabItem tbiOptions = new TabItem(tabGetChanged, 0);
		tbiOptions.setText("Options");
		tabGetChanged.setSelection(0);

		//Manual
		Label lblManual = new Label(tabGetChanged, 0);
		lblManual.setText("Coming soon!");
//		tbiManual.setControl(lblManual);

		//SVN Log
		Composite SVNPanel = new Composite(tabGetChanged, 0);
    	GridLayout SVNLayout = new GridLayout();
    	SVNLayout.numColumns = 2;
		SVNPanel.setLayout(SVNLayout);
		tbiSVNLog.setControl(SVNPanel);
		//URL
		final Label lblSVNURL = new Label(SVNPanel, 0);
		lblSVNURL.setText("Repository URL:");
		final Text txtSVNURL = new Text(SVNPanel, SWT.BORDER | SWT.SINGLE);
		txtSVNURL.setText("svn://");
		txtSVNURL.setLayoutData(ldtTwoCol);
		//Base Revision
		final Label lblSVNName = new Label(SVNPanel, 0);
		lblSVNName.setText("User name:");
		lblSVNName.setLayoutData(ldtTop);
		final Text txtSVNName = new Text(SVNPanel, SWT.BORDER | SWT.SINGLE);
		txtSVNName.setText("");
		txtSVNName.setLayoutData(ldtDefault);
		//Changed Revision
		final Label lblSVNPasswd = new Label(SVNPanel, 0);
		lblSVNPasswd.setText("Password:");
		lblSVNPasswd.setLayoutData(ldtTop);
		final Text txtSVNPasswd = new Text(SVNPanel, SWT.BORDER | SWT.SINGLE | SWT.PASSWORD);
		txtSVNPasswd.setText("");
		txtSVNPasswd.setLayoutData(ldtDefault);
		//Base Revision
		final Label lblSVNBaseRev = new Label(SVNPanel, 0);
		lblSVNBaseRev.setText("Base Revision:");
		lblSVNBaseRev.setLayoutData(ldtTop);
		final Text txtSVNBaseRev = new Text(SVNPanel, SWT.BORDER | SWT.SINGLE);
		txtSVNBaseRev.setText("");
		txtSVNBaseRev.setLayoutData(ldtDefault);
		//Changed Revision
		final Label lblSVNChangeRev = new Label(SVNPanel, 0);
		lblSVNChangeRev.setText("New Revision:");
		lblSVNChangeRev.setLayoutData(ldtTop);
		final Text txtSVNChangeRev = new Text(SVNPanel, SWT.BORDER | SWT.SINGLE);
		txtSVNChangeRev.setText("");
		txtSVNChangeRev.setLayoutData(ldtDefault);
		//GetFiles button
		final Button btnSVNLog = new Button(SVNPanel, SWT.PUSH);
		btnSVNLog.setText("Get Files");
		btnSVNLog.setLayoutData(new GridData(GridData.FILL_VERTICAL | GridData.VERTICAL_ALIGN_END));
		btnSVNLog.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				proj1 = cmbProj1.getText();
				proj2 = "";

				//parse some inputs;
				svnurl = txtSVNURL.getText();
				try {
					svnrev1 = Integer.parseInt(txtSVNBaseRev.getText());
				} catch (NumberFormatException e) {
					MessageDialog.openError(shell.getShell(), "Bad Base Revision", "Enter a number for base revision");
					return;
				}
				try {
					svnrev2 = Integer.parseInt(txtSVNChangeRev.getText());
				} catch (NumberFormatException e) {
					MessageDialog.openError(shell.getShell(), "Bad Changed Revision", "Enter a number for changed revision");
					return;
				}
				svnname = txtSVNName.getText();
				svnpasswd = txtSVNPasswd.getText();
				Set<String> allfiles = null;
				try {
					allfiles = findChangedFilesFromSVN(svnurl, svnrev1, svnrev2);
				} catch (SVNException e) {
					MessageDialog.openError(shell.getShell(), "SVN Repository Error", "Cannot connect to repository");
					return;
				}
				//populate list
				tblMyChangedFiles.removeAll();
				for (String file : allfiles) {
					TableItem ti = new TableItem(tblMyChangedFiles, 0);
					ti.setText(new String[] { file });
					ti.setChecked(true);
				}
				isSVN = true;
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		//Options
		Composite optionsPanel = new Composite(tabGetChanged, 0);
    	GridLayout optionsLayout = new GridLayout();
    	optionsLayout.numColumns = 2;
    	optionsPanel.setLayout(optionsLayout);
		tbiOptions.setControl(optionsPanel);
		//URL
		//Base Revision
		final Label lblMinAccuracy = new Label(optionsPanel, 0);
		lblMinAccuracy.setText("Min Accuracy:");
		lblMinAccuracy.setLayoutData(ldtTop);
		txtMinAccuracy = new Text(optionsPanel, SWT.BORDER | SWT.SINGLE);
		txtMinAccuracy.setText(Double.toString(MetaInfo.accuracy));
		txtMinAccuracy.setLayoutData(ldtDefault);
		//Base Revision
		final Label lblMinMatches = new Label(optionsPanel, 0);
		lblMinMatches.setText("Min Matches:");
		lblMinMatches.setLayoutData(ldtTop);
		txtMinMatches = new Text(optionsPanel, SWT.BORDER | SWT.SINGLE);
		txtMinMatches.setText(Integer.toString(MetaInfo.minConcFact));
		txtMinMatches.setLayoutData(ldtDefault);
		//Base Revision
		final Label lblAntecedent = new Label(optionsPanel, 0);
		lblAntecedent.setText("Antecedents:");
		lblAntecedent.setLayoutData(ldtTop);
		txtAntecedent = new Text(optionsPanel, SWT.BORDER | SWT.SINGLE);
		txtAntecedent.setText(Integer.toString(MetaInfo.k));
		txtAntecedent.setLayoutData(ldtDefault);
		//Base Revision
		final Label lblBeamSize = new Label(optionsPanel, 0);
		lblBeamSize.setText("Beam Size:");
		lblBeamSize.setLayoutData(ldtTop);
		txtBeamSize = new Text(optionsPanel, SWT.BORDER | SWT.SINGLE);
		txtBeamSize.setText(Integer.toString(MetaInfo.beamSize));
		txtBeamSize.setLayoutData(ldtDefault);
		//Base Revision
		final Label lblGranularity = new Label(optionsPanel, 0);
		lblGranularity.setText("Granularity:");
		lblGranularity.setLayoutData(ldtTop);
		cmbGranularity = new Combo(optionsPanel, SWT.READ_ONLY);
		cmbGranularity.add("PACKAGE");
		cmbGranularity.add("TYPE");
		cmbGranularity.add("TYPE-DEPENDENCY");
		cmbGranularity.add("METHOD");
		cmbGranularity.add("FIELD");
		cmbGranularity.add("BODY");
		cmbGranularity.select(3);
		cmbGranularity.setLayoutData(ldtDefault);
		//Base Revision
		final Label lblHops = new Label(optionsPanel, SWT.READ_ONLY);
		lblHops.setText("Num of Hops:");
		lblHops.setLayoutData(ldtTop);
		cmbHops = new Combo(optionsPanel, 0);
		cmbHops.setSize(1,1);
		cmbHops.add("1");
		cmbHops.add("2");
		cmbHops.add("3");
		cmbHops.add("Infinite");
		cmbHops.select(0);
		cmbHops.setLayoutData(ldtDefault);
		final Button btnSVNLog1 = new Button(optionsPanel, SWT.PUSH);
		btnSVNLog1.setText("Get Files");
		btnSVNLog1.setVisible(false);

		//Diff
		final Composite DiffPanel = new Composite(tabGetChanged, 0);
		final GridLayout DiffLayout = new GridLayout();
    	DiffLayout.numColumns = 1;
    	DiffPanel.setLayout(DiffLayout);
		tbiDiff.setControl(DiffPanel);
		//Diff options
		Label lblProj3 = new Label(DiffPanel, 0);
		lblProj3.setText("Changed project:");
		final Combo cmbOtherProj = new Combo(DiffPanel, SWT.CHECK);
		cmbProj2 = cmbOtherProj;
		cmbProj2.setVisibleItemCount(20);
		for (IProject proj : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			cmbOtherProj.add(proj.getName());
		}
		cmbOtherProj.setLayoutData(ldtTop);
		//GetDiff button
		final Button btnDiff = new Button(DiffPanel, SWT.PUSH);
		btnDiff.setText("Get Files");
		btnDiff.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				proj1 = cmbProj1.getText();
				proj2 = cmbProj2.getText();

				//parse project inputs
				Set<String> allfiles = null;
				try {
					allfiles = WS.findChangedFilesByDiff(cmbProj1.getText(), cmbOtherProj.getText());
				} catch (Exception e) {
					MessageDialog.openError(shell.getShell(), "Project Diff Error", e.getMessage());
					return;
				}
				//populate list
				tblMyChangedFiles.removeAll();
				for (String file : allfiles) {
					TableItem ti = new TableItem(tblMyChangedFiles, 0);
					ti.setText(new String[] { file });
					ti.setChecked(true);
				}
				isSVN = false;
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		//~~~~~~~~Right panel~~~~~~~~
		//- Big group labeled "Changed Files" 
		//  - List of files to be parsed (each with a check box)
		Group grpRightPanel = new Group(parent, SWT.SHADOW_ETCHED_IN);
		grpRightPanel.setText("Changed Files");
		grpRightPanel.setLayout(layout);
		Composite rightPanel = new Composite(grpRightPanel, 0);
		rightPanel.setLayoutData(ldtDefault);
		GridLayout rightLayout = new GridLayout();
		rightLayout.numColumns = 1;
		rightPanel.setLayout(rightLayout);

		//List of files (with check box)
		final Table tblChangeFiles = new Table(rightPanel, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION | SWT.CHECK);
		tblMyChangedFiles = tblChangeFiles;
		tblChangeFiles.setLayoutData(new GridData(280,242));
		final TableColumn col1 = new TableColumn(tblChangeFiles, SWT.NULL);
		col1.setText("Changed Files");
		col1.setWidth(280);

		return parent;
	}
    private SVNRepository makeRepositoryConnection(String url, String username, String password) throws SVNException {
		ISVNAuthenticationManager authmgr = new BasicAuthenticationManager(username, password);
	    SVNURL svnurl = SVNURL.parseURIDecoded(url);
	    if (svnurl.getProtocol().equalsIgnoreCase("http") || svnurl.getProtocol().equalsIgnoreCase("https")) {
		    DAVRepositoryFactory.setup();
	    } else if (svnurl.getProtocol().equalsIgnoreCase("svn") || svnurl.getProtocol().equalsIgnoreCase("svn+ssh")) {
		    SVNRepositoryFactoryImpl.setup();
	    } else if (svnurl.getProtocol().equalsIgnoreCase("file")) {
	    	FSRepositoryFactory.setup();
	    }
        SVNRepository repository = SVNRepositoryFactory.create( svnurl, null );
        repository.setAuthenticationManager(authmgr);
    	return repository;
    }
    private Set<String> findChangedFilesFromSVN(String url, int revision1, int revision2) throws SVNException {
    	int LENGTH_COMMENT = 200; 

    	Set<String> res = new HashSet<String>();
    	HashSet<String> blacklist = new HashSet<String>();

    	SVNRepository repository = makeRepositoryConnection(url, svnname, svnpasswd);
        Collection logEntries = null;
        long startRevision = revision1;
        long latestRevision = revision2;
        logEntries = repository.log( new String[] { "" } , null , startRevision , latestRevision , true , true );
        for ( Iterator entries = logEntries.iterator( ); entries.hasNext( ); ) { //should be only one revision here

            SVNLogEntry logEntry = ( SVNLogEntry ) entries.next( );

        	//write to db
        	String revision = Long.toString(logEntry.getRevision());
        	SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        	String datetime = dateformat.format(logEntry.getDate()); 
        	String author = logEntry.getAuthor();
        	String comments = logEntry.getMessage();
        	if (comments.length()>LENGTH_COMMENT) comments=comments.substring(0, LENGTH_COMMENT);
        	String[] record = {revision, datetime, author, comments};

            Set changedPathsSet = logEntry.getChangedPaths( ).keySet( );
            for ( Iterator changedPaths = changedPathsSet.iterator( ); changedPaths.hasNext( ); ) {
                SVNLogEntryPath entryPath = ( SVNLogEntryPath ) logEntry.getChangedPaths( ).get( changedPaths.next( ) );

                try {
                	if (repository.checkPath(entryPath.getPath(), logEntry.getRevision()).compareTo(SVNNodeKind.FILE)!=0) continue;
                } catch (SVNException e) {
                	continue;
                }
                if (!entryPath.getPath().endsWith(".java")) continue;

                char changetype = entryPath.getType();
                switch (changetype) {

                case SVNLogEntryPath.TYPE_ADDED:
                	break;
                case SVNLogEntryPath.TYPE_DELETED:
                	break;
                case SVNLogEntryPath.TYPE_MODIFIED:
                	res.add(entryPath.getPath().substring(1));
                	break;
                case SVNLogEntryPath.TYPE_REPLACED:
                	break;
                }
            }
        }
        return res;
    }
    private String loadFilesFromSVN(String filePath) throws SVNException {
    	int LENGTH_COMMENT = 200; 

        DAVRepositoryFactory.setup( );

        SVNRepository repository = null;
        repository = SVNRepositoryFactory.create( SVNURL.parseURIEncoded( svnurl ) );
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager( svnname , svnpasswd );
        repository.setAuthenticationManager( authManager );

        SVNNodeKind nodeKind = repository.checkPath( "/"+filePath , -1 );
        
        if ( nodeKind == SVNNodeKind.NONE ) {
            System.err.println( "There is no entry at '" + svnurl + "'." );
            System.exit( 1 );
        } else if ( nodeKind == SVNNodeKind.DIR ) {
            System.err.println( "The entry at '" + svnurl + "' is a directory while a file was expected." );
            System.exit( 1 );
        }
        
        SVNProperties fileProperties = new SVNProperties();  
        ByteArrayOutputStream baos = new ByteArrayOutputStream( );
        repository.getFile( filePath , -1 , fileProperties , baos );
        
        return baos.toString();
    }
}
