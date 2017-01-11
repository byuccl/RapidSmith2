package edu.byu.ece.rapidSmith.device.vsrt.gui;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

import com.trolltech.qt.core.Qt.FocusPolicy;
import com.trolltech.qt.gui.QComboBox;
import com.trolltech.qt.gui.QWheelEvent;
import com.trolltech.qt.gui.QWidget;


/**
 * This class displays all of the available families/architectures 
 * whose primitive sites are available for editing in a combo box widget
 * @author Thomas Townsend
 * Created on: Jul 2, 2014
 */
public class FamilyComboBox extends QComboBox{
	/***/
	private QWidget parent; 
	/**Family/architecture located at index 0 of the combo box*/
	private String firstFamily;
	/**Filter used to only extract directories that contain .def files*/
	private FilenameFilter defFilter; 
	
	public FamilyComboBox (QWidget parent, FilenameFilter filter) {
		
		super(parent);
		this.parent = parent; 
		
		this.defFilter = filter;
	}
	
	/**
	 * Finds all of the directories within the directory passed into the function
	 * that contain .def files within them, and adds them to the combobox. 
	 * @param directory
	 */
	public void init_cb(String directory) {
		
		this.setFocusPolicy(FocusPolicy.NoFocus);
			
		File prim_defs = new File(directory); 
	
		File[] families = prim_defs.listFiles();
		
		Arrays.sort(families);
		
		//Only add directories which contain primitive .def files within them 
		for (File file : families) {
			if (file.isDirectory() && (file.listFiles(this.defFilter).length > 0))
				this.addItem( file.getName() );
		}
		firstFamily = this.itemText(0);
		this.currentIndexChanged.connect(parent, "load_new_family()" );	
	}
	
	/**
	 * overriding this method so that the combo box does not<br> 
	 * scroll when mouse over wheel event occurs	 
	 * */
	@Override
	protected void wheelEvent(QWheelEvent event) {
		
	}
	
	/**
	 * Returns the family name at index 0 of the combo box so that 
	 * when the SiteSelectTab is initialized, the primitive sites of this family 
	 * will be initially displayed
	 * @return String - name of the family/architecture and index 0
	 */
	public String getFirstFamily(){
		return this.firstFamily;
	}
}