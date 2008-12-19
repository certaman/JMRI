// ImportEngineRosterAction.java

package jmri.jmrit.operations.rollingstock.engines;
import jmri.jmrit.operations.locations.LocationManager;
import jmri.jmrit.operations.locations.Location;
import jmri.jmrit.operations.locations.Track;
import jmri.jmrix.nce.consist.NceConsistBackup;

import javax.swing.*;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.ResourceBundle;

import jmri.util.StringUtil;


/**
 * Starts the ImportRosterEngines thread
 * @author Dan Boudreau Copyright (C) 2008
 * @version $Revision: 1.2 $
 */
public class ImportRosterEngineAction extends AbstractAction {
	
    public ImportRosterEngineAction(String actionName, Component frame) {
        super(actionName);
    }
	
	public void actionPerformed(ActionEvent ae) {
		Thread mb = new ImportRosterEngines();
		mb.setName("ImportRosterEngines");
		mb.start();
	}

	static org.apache.log4j.Category log = org.apache.log4j.Category
			.getInstance(ImportRosterEngineAction.class.getName());
}
